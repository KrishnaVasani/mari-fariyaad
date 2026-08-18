package com.gvp.marifariyaad.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender javaMailSender;

    public void sendOtpEmail(String toEmail, String recipientName, String subject, String otp, String purpose) {
        String body = "<p>Hello " + recipientName + ",</p>"
                + "<p>Thank you for registering with <strong>Mari-Fariyaad</strong>.</p>"
                + "<p>Your email verification OTP is:</p>"
                + "<h2>" + otp + "</h2>"
                + "<p>This OTP is valid for <strong>5 minutes</strong>.</p>"
                + "<p>Please do not share this OTP with anyone.</p>"
                + "<p>Regards,<br/>Mari-Fariyaad Team<br/>GVP</p>";
        sendHtmlEmail(toEmail, subject, body);
    }

    private void sendHtmlEmail(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            javaMailSender.send(message);
        } catch (MailException e) {
            log.error("SMTP failure sending email to {}: {}", toEmail, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to build email for {}: {}", toEmail, e.getMessage());
            throw new MailSendException("Failed to send email", e);
        }
    }
}
