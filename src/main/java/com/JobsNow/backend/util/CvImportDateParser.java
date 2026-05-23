package com.JobsNow.backend.util;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CvImportDateParser {

    private static final Pattern MM_YYYY = Pattern.compile("(\\d{1,2})\\s*[/\\-]\\s*(\\d{4})");
    private static final Pattern YYYY_ONLY = Pattern.compile("\\b(19|20)\\d{2}\\b");

    private CvImportDateParser() {}

    /** Required fields fallback when CV has no parseable date. */
    public static LocalDate defaultStartDate() {
        return LocalDate.of(2000, 1, 1);
    }

    public static LocalDate parseStartDate(String startDate, String duration) {
        LocalDate fromStart = parseFlexible(startDate);
        if (fromStart != null) {
            return fromStart;
        }
        if (duration != null && !duration.isBlank()) {
            LocalDate fromDuration = parseRangeStart(duration);
            if (fromDuration != null) {
                return fromDuration;
            }
        }
        return defaultStartDate();
    }

    public static LocalDate parseEndDate(String endDate, String duration) {
        if (isPresent(endDate)) {
            return null;
        }
        LocalDate fromEnd = parseFlexible(endDate);
        if (fromEnd != null) {
            return fromEnd;
        }
        if (duration != null && !duration.isBlank()) {
            return parseRangeEnd(duration);
        }
        return null;
    }

    public static LocalDate parseIssueDate(String issueDate) {
        LocalDate parsed = parseFlexible(issueDate);
        return parsed != null ? parsed : defaultStartDate();
    }

    private static boolean isPresent(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String lower = value.trim().toLowerCase(Locale.ROOT);
        return lower.contains("present")
                || lower.contains("hiện tại")
                || lower.contains("nay")
                || lower.equals("now");
    }

    private static LocalDate parseFlexible(String raw) {
        if (raw == null || raw.isBlank() || isPresent(raw)) {
            return null;
        }
        String text = raw.trim();
        Matcher mmYyyy = MM_YYYY.matcher(text);
        if (mmYyyy.find()) {
            int month = Integer.parseInt(mmYyyy.group(1));
            int year = Integer.parseInt(mmYyyy.group(2));
            return safeYearMonth(year, month);
        }
        Matcher year = YYYY_ONLY.matcher(text);
        if (year.find()) {
            return LocalDate.of(Integer.parseInt(year.group()), 1, 1);
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException ignored) {}
        for (DateTimeFormatter fmt : new DateTimeFormatter[] {
                DateTimeFormatter.ofPattern("yyyy-MM"),
                DateTimeFormatter.ofPattern("yyyy/MM")
        }) {
            try {
                return YearMonth.parse(text, fmt).atDay(1);
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    private static LocalDate parseRangeStart(String duration) {
        String[] parts = duration.split("\\s*[-–—tođến]+\\s*", 2);
        if (parts.length > 0) {
            return parseFlexible(parts[0].trim());
        }
        return null;
    }

    private static LocalDate parseRangeEnd(String duration) {
        String[] parts = duration.split("\\s*[-–—tođến]+\\s*", 2);
        if (parts.length > 1) {
            return parseFlexible(parts[1].trim());
        }
        return null;
    }

    private static LocalDate safeYearMonth(int year, int month) {
        if (month < 1 || month > 12) {
            return LocalDate.of(year, 1, 1);
        }
        return YearMonth.of(year, month).atDay(1);
    }
}
