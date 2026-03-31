package com.JobsNow.backend.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class VNPayUtils {

    public static String buildHashData(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        boolean first = true;
        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if (fieldValue == null || fieldValue.isEmpty()) {
                continue;
            }
            if (!first) {
                hashData.append("&");
            }
            hashData.append(fieldName).append("=")
                    .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
            first = false;
        }
        return hashData.toString();
    }

    public static String calculateSecureHash(Map<String, String> params, String secretKey) {
        return hmacSHA512(secretKey, buildHashData(params));
    }

    public static boolean isValidSignature(Map<String, String> params, String secretKey, String secureHash) {
        if (secureHash == null || secureHash.isBlank()) {
            return false;
        }
        String calculated = calculateSecureHash(params, secretKey);
        return secureHash.equalsIgnoreCase(calculated);
    }

    public static String buildQuery(Map<String, String> vnpParams, String secretKey, String payUrl) {
        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnpParams.get(fieldName);

            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName).append("=");
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append("=");
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                if (itr.hasNext()) {
                    hashData.append("&");
                    query.append("&");
                }
            }
        }

        String vnpSecureHash = hmacSHA512(secretKey, hashData.toString());
        query.append("&vnp_SecureHash=").append(vnpSecureHash);

        return payUrl + "?" + query;
    }

    public static String hmacSHA512(String key, String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException();
            }
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes();
            SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    public static Map<String, String> extractParams(String url) {
        Map<String, String> params = new HashMap<>();
        try {
            String queryString = url.contains("?") ? url.split("\\?")[1] : url;
            String[] pairs = queryString.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                String k = java.net.URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                String v = keyValue.length > 1 ? java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8) : "";
                params.put(k, v);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error extracting params from URL", e);
        }
        return params;
    }
}
