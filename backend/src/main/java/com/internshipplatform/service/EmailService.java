package com.internshipplatform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.mail.from:noreply@internshipplatform.com}")
    private String mailFrom;

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String subject = "Password Reset Request - Internship Platform";
        String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;
        String body = "Hello,\n\n"
                + "You have requested to reset your password.\n\n"
                + "Click the link below to reset your password:\n"
                + resetUrl + "\n\n"
                + "This link will expire in 15 minutes.\n\n"
                + "If you did not request this, please ignore this email.\n\n"
                + "Best regards,\nInternship Platform Team";

        sendEmail(toEmail, subject, body);
    }

    public void sendVerificationEmail(String toEmail, String verificationToken) {
        String subject = "Verify Your Email - Internship Platform";
        String verifyUrl = frontendUrl + "/verify-email?token=" + verificationToken;
        String body = "Hello,\n\n"
                + "Thank you for registering with Internship Platform.\n\n"
                + "Please verify your email address by clicking the link below:\n"
                + verifyUrl + "\n\n"
                + "This link will expire in 24 hours.\n\n"
                + "If you did not create an account, please ignore this email.\n\n"
                + "Best regards,\nInternship Platform Team";

        sendEmail(toEmail, subject, body);
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            // Don't throw - email failure shouldn't break the request
        }
    }
}
