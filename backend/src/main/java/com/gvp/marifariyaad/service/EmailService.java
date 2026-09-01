package com.gvp.marifariyaad.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender javaMailSender;

    /**
     * @return true if the email was handed off to the SMTP server successfully,
     *         false if it failed (bad/missing credentials, network issue, etc).
     *         Callers (AuthService) use this to decide whether to tell the user
     *         the OTP was actually sent - we must never claim success when it wasn't.
     */
    public boolean sendOtpEmail(String toEmail, String subject, String otp, String purpose) {
        String body = "Dear User,\n\n"
                + "Your One-Time Password (OTP) for " + purpose + " on Mari-Fariyaad (Gujarat Vidyapith "
                + "Complaint Management Portal) is:\n\n"
                + otp + "\n\n"
                + "This OTP is valid for 10 minutes. Please do not share this OTP with anyone.\n\n"
                + "If you did not request this, please ignore this email.\n\n"
                + "Regards,\nMari-Fariyaad Team\nGujarat Vidyapith";
        boolean sent = sendPlainTextEmail(toEmail, subject, body);
        if (!sent) {
            // SMTP is not configured/working - still surface the OTP in the server
            // console so registration/password-reset can be tested locally without
            // real mail credentials. This line must never appear in production logs
            // once MAIL_USERNAME/MAIL_PASSWORD are set correctly (see MailConfig).
            log.warn("EMAIL DELIVERY FAILED for {} — OTP was NOT emailed. For local testing only, the OTP is: {}", toEmail, otp);
        }
        return sent;
    }

    public boolean sendPlainTextEmail(String toEmail, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            javaMailSender.send(message);
            return true;
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }
}
