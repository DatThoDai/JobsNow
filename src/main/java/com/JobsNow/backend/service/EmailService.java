package com.JobsNow.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendOtpEmail(String to, String otp) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(fromEmail, "JobsNow");
        helper.setTo(to);
        helper.setSubject("JobsNow - Xác thực email - Mã OTP");
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

    public void sendLoginOtpEmail(String to, String otp) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(fromEmail, "JobsNow");
        helper.setTo(to);
        helper.setSubject("JobsNow - Mã đăng nhập");
        helper.setText(
                "<div style='font-family: Arial, sans-serif;'>" +
                        "<h2 style='color: #4CAF50;'>Mã đăng nhập của bạn</h2>" +
                        "<p>Mã OTP để đăng nhập:</p>" +
                        "<h1 style='color: #2196F3; letter-spacing: 5px;'>" + otp + "</h1>" +
                        "<p style='color: #666;'>Mã này có hiệu lực trong <strong>5 phút</strong>.</p>" +
                        "<p style='color: #f44336;'>Vui lòng không chia sẻ mã này với bất kỳ ai.</p>" +
                        "</div>",
                true
        );

        mailSender.send(message);
    }

    /**
     * Gửi mail khi recruiter duyệt application (HIRED).
     * Người nhận: jobseeker.
     */
    public void sendApplicationApprovedEmail(String to, String candidateName, String jobTitle, String companyName) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(fromEmail, "JobsNow");
        helper.setTo(to);
        helper.setSubject("JobsNow - Đơn ứng tuyển đã được duyệt");
        helper.setText(
                "<div style='font-family: Arial, sans-serif;'>" +
                        "<h2 style='color: #4CAF50;'>Chúc mừng " + (candidateName != null ? candidateName : "Ứng viên") + "!</h2>" +
                        "<p>Đơn ứng tuyển của bạn vào vị trí <strong>" + jobTitle + "</strong> tại <strong>" + companyName + "</strong> đã được duyệt.</p>" +
                        "<p>Nhà tuyển dụng sẽ liên hệ với bạn trong thời gian sớm nhất.</p>" +
                        "<p style='color: #666;'>Trân trọng,<br/>JobsNow</p>" +
                        "</div>",
                true
        );
        mailSender.send(message);
    }

    /**
     * Gửi mail khi recruiter từ chối application (REJECTED).
     */
    /**
     * Nội dung HTML do recruiter soạn (TipTap). Có thể dùng placeholder: {{name}}, {{jobTitle}}, {{companyName}}
     * — được thay bằng dữ liệu thật khi gửi (tương tự Sendy merge tags).
     */
    public void sendApplicationInterviewEmail(
            String to,
            String candidateName,
            String jobTitle,
            String companyName,
            String interviewBodyHtml
    ) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(fromEmail, "JobsNow");
        helper.setTo(to);
        helper.setSubject("JobsNow - Lịch phỏng vấn / Thông tin phỏng vấn");
        String safeName = HtmlUtils.htmlEscape(candidateName != null ? candidateName : "Ứng viên");
        String safeJob = HtmlUtils.htmlEscape(jobTitle != null ? jobTitle : "");
        String safeCompany = HtmlUtils.htmlEscape(companyName != null ? companyName : "");
        String body = interviewBodyHtml != null ? interviewBodyHtml : "";
        body = body
                .replace("{{name}}", safeName)
                .replace("{{jobTitle}}", safeJob)
                .replace("{{companyName}}", safeCompany);
        helper.setText(
                "<div style='font-family: Arial, sans-serif; max-width: 640px;'>" +
                        "<h2 style='color: #1565c0;'>Thông báo phỏng vấn</h2>" +
                        "<div style='margin-top:16px;color:#333;line-height:1.6;'>" + body + "</div>" +
                        "<p style='color:#666;margin-top:24px;font-size:13px;'>Vị trí: <strong>" + safeJob + "</strong><br/>" +
                        "Công ty: <strong>" + safeCompany + "</strong></p>" +
                        "<p style='color:#666;'>Trân trọng,<br/>JobsNow</p>" +
                        "</div>",
                true
        );
        mailSender.send(message);
    }

    public void sendApplicationRejectedEmail(String to, String candidateName, String jobTitle, String companyName) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(fromEmail, "JobsNow");
        helper.setTo(to);
        helper.setSubject("JobsNow - Thông báo về đơn ứng tuyển");
        helper.setText(
                "<div style='font-family: Arial, sans-serif;'>" +
                        "<h2 style='color: #666;'>Kính gửi " + (candidateName != null ? candidateName : "Ứng viên") + ",</h2>" +
                        "<p>Rất tiếc, đơn ứng tuyển của bạn vào vị trí <strong>" + jobTitle + "</strong> tại <strong>" + companyName + "</strong> chưa được chọn trong lần này.</p>" +
                        "<p>Chúc bạn sớm tìm được công việc phù hợp.</p>" +
                        "<p style='color: #666;'>Trân trọng,<br/>JobsNow</p>" +
                        "</div>",
                true
        );
        mailSender.send(message);
    }

    /**
     * Gửi mail khi admin duyệt bài post cho recruiter.
     */
    public void sendJobPostApprovedEmail(String to, String jobTitle, String companyName) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(fromEmail, "JobsNow");
        helper.setTo(to);
        helper.setSubject("JobsNow - Tin tuyển dụng đã được duyệt");
        helper.setText(
                "<div style='font-family: Arial, sans-serif;'>" +
                        "<h2 style='color: #4CAF50;'>Tin tuyển dụng đã được duyệt</h2>" +
                        "<p>Tin tuyển dụng <strong>" + jobTitle + "</strong> của công ty <strong>" + companyName + "</strong> đã được duyệt và hiển thị trên JobsNow.</p>" +
                        "<p style='color: #666;'>Trân trọng,<br/>JobsNow</p>" +
                        "</div>",
                true
        );
        mailSender.send(message);
    }

    /**
     * Gửi mail khi admin từ chối bài post cho recruiter.
     */
    public void sendJobPostRejectedEmail(String to, String jobTitle, String companyName, String reason) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(fromEmail, "JobsNow");
        helper.setTo(to);
        helper.setSubject("JobsNow - Tin tuyển dụng chưa được duyệt");
        helper.setText(
                "<div style='font-family: Arial, sans-serif;'>" +
                        "<h2 style='color: #f44336;'>Tin tuyển dụng chưa được duyệt</h2>" +
                        "<p>Tin tuyển dụng <strong>" + jobTitle + "</strong> của công ty <strong>" + companyName + "</strong> chưa được duyệt.</p>" +
                        (reason != null && !reason.isBlank() ? "<p><strong>Lý do:</strong> " + reason + "</p>" : "") +
                        "<p style='color: #666;'>Trân trọng,<br/>JobsNow</p>" +
                        "</div>",
                true
        );
        mailSender.send(message);
    }
}
