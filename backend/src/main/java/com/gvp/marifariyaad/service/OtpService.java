package com.gvp.marifariyaad.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class OtpService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    public static final int OTP_VALIDITY_MINUTES = 10;

    private final PasswordEncoder passwordEncoder;

    public OtpService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /** Generates a secure random 6-digit OTP (000000-999999), zero-padded. */
    public String generateOtp() {
        int number = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", number);
    }

    public String hashOtp(String otp) {
        return passwordEncoder.encode(otp);
    }

    public boolean matches(String rawOtp, String otpHash) {
        return passwordEncoder.matches(rawOtp, otpHash);
    }
}
