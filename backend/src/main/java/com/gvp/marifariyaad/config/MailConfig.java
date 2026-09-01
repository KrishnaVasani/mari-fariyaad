package com.gvp.marifariyaad.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import jakarta.annotation.PostConstruct;
import java.util.Properties;

/**
 * SMTP configuration is read from environment variables (MAIL_HOST,
 * MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD) when present, falling back to
 * the project owner's Gmail App Password below so OTP email works out of
 * the box. To use a different mailbox (e.g. in another deployment), just
 * set the MAIL_* environment variables - they always take priority.
 */
@Configuration
@Slf4j
public class MailConfig {

    @Value("${MAIL_HOST:smtp.gmail.com}")
    private String host;

    @Value("${MAIL_PORT:587}")
    private int port;

    @Value("${MAIL_USERNAME:vivek.sonrat@gmail.com}")
    private String username;

    @Value("${MAIL_PASSWORD:cqrxowekktyxlsyz}")
    private String password;

    @PostConstruct
    public void warnIfNotConfigured() {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            log.warn("=================================================================");
            log.warn(" MAIL_USERNAME / MAIL_PASSWORD are NOT set.");
            log.warn(" Registration and Forgot-Password OTP emails will FAIL to send.");
            log.warn(" Copy .env.example to .env, fill in a real Gmail App Password");
            log.warn(" (https://myaccount.google.com/apppasswords), and restart.");
            log.warn("=================================================================");
        }
    }

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        return mailSender;
    }
}
