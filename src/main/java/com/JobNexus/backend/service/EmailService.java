package com.JobNexus.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendOtpEmail(String to, String otp) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage(); // là tạo một đối tượng MimeMessage mới để đại diện cho email cần gửi.
        MimeMessageHelper helper = new MimeMessageHelper(message, true);// TRUE LÀ CHO PHÉP GỬI NỘI DUNG HTML

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("JobNexus - Xác thực email - Mã OTP");
        helper.setText(
                "<div style='font-family: Arial, sans-serif;'>" +
                        "<h2 style='color: #4CAF50;'>Xác thực email của bạn</h2>" +
                        "<p>Mã OTP của bạn là:</p>" +
                        "<h1 style='color: #2196F3; letter-spacing: 5px;'>" + otp + "</h1>" +
                        "<p style='color: #666;'>Mã này có hiệu lực trong <strong>10 phút</strong>.</p>" +
                        "<p style='color: #f44336;'>⚠️ Vui lòng không chia sẻ mã này với bất kỳ ai.</p>" +
                        "</div>",
                true
        );

        mailSender.send(message);
    }
}
