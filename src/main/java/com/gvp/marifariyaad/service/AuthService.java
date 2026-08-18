package com.gvp.marifariyaad.service;

import com.gvp.marifariyaad.dto.*;
import com.gvp.marifariyaad.entity.*;
import com.gvp.marifariyaad.exception.BadRequestException;
import com.gvp.marifariyaad.exception.ResourceNotFoundException;
import com.gvp.marifariyaad.repository.PasswordResetOtpRepository;
import com.gvp.marifariyaad.repository.PendingRegistrationRepository;
import com.gvp.marifariyaad.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;

    private static final List<String> VALID_ROLES = List.of(
            "Student", "Faculty", "Staff", "Hosteller", "Research Scholar", "Visitor", "Contract Worker", "Other"
    );

    @Transactional
    public void startRegistration(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Password and Confirm Password do not match.");
        }
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BadRequestException("An account with this email is already registered. Please login instead.");
        }

        pendingRegistrationRepository.findAllByEmailIgnoreCase(request.getEmail())
                .forEach(pendingRegistrationRepository::delete);

        String otp = otpService.generateOtp();

        PendingRegistration pending = PendingRegistration.builder()
                .fullName(request.getFullName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .mobile(request.getMobile().trim())
                .gender(request.getGender())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .department(request.getDepartment())
                .hostel(request.getHostel())
                .address(request.getAddress())
                .otpHash(otpService.hashOtp(otp))
                .otpExpiresAt(LocalDateTime.now().plusMinutes(OtpService.OTP_VALIDITY_MINUTES))
                .build();

        pendingRegistrationRepository.save(pending);

        try {
            emailService.sendOtpEmail(pending.getEmail(), pending.getFullName(), "Mari-Fariyaad - Email Verification OTP", otp,
                    "email verification and account registration");
        } catch (MailException ex) {
            throw new BadRequestException("Failed to send registration OTP email. Please verify your email address and try again.");
        }
    }

    @Transactional
    public User completeRegistration(VerifyRegistrationRequest request) {
        PendingRegistration pending = pendingRegistrationRepository
                .findTopByEmailIgnoreCaseOrderByCreatedAtDesc(request.getEmail())
                .orElseThrow(() -> new BadRequestException("No pending registration found for this email. Please register again."));

        if (pending.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            pendingRegistrationRepository.delete(pending);
            throw new BadRequestException("OTP has expired. Please request a new OTP to complete registration.");
        }

        if (!otpService.matches(request.getOtp(), pending.getOtpHash())) {
            throw new BadRequestException("Invalid OTP. Please check and try again.");
        }

        if (userRepository.existsByEmailIgnoreCase(pending.getEmail())) {
            pendingRegistrationRepository.delete(pending);
            throw new BadRequestException("An account with this email is already registered. Please login instead.");
        }

        User user = User.builder()
                .fullName(pending.getFullName())
                .email(pending.getEmail())
                .mobile(pending.getMobile())
                .gender(pending.getGender())
                .passwordHash(pending.getPasswordHash())
                .role(Role.USER)
                .department(pending.getDepartment())
                .hostel(pending.getHostel())
                .address(pending.getAddress())
                .enabled(true)
                .emailVerified(true)
                .build();

        User saved = userRepository.save(user);
        pendingRegistrationRepository.delete(pending);
        return saved;
    }

    @Transactional
    public void startForgotPassword(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with this email address."));

        passwordResetOtpRepository.findAllByEmailIgnoreCaseAndUsedFalse(email)
                .forEach(resetOtp -> resetOtp.setUsed(true));

        String otp = otpService.generateOtp();
        PasswordResetOtp resetOtp = PasswordResetOtp.builder()
                .email(user.getEmail())
                .otpHash(otpService.hashOtp(otp))
                .expiresAt(LocalDateTime.now().plusMinutes(OtpService.OTP_VALIDITY_MINUTES))
                .used(false)
                .build();
        passwordResetOtpRepository.save(resetOtp);

        try {
            emailService.sendOtpEmail(user.getEmail(), user.getFullName(), "Mari-Fariyaad - Password Reset OTP", otp, "resetting your account password");
        } catch (MailException ex) {
            throw new BadRequestException("Failed to send password reset OTP email. Please verify your email address and try again.");
        }
    }

    @Transactional(readOnly = true)
    public void verifyResetOtp(String email, String otp) {
        PasswordResetOtp resetOtp = getValidResetOtp(email, otp);
        // Valid: nothing further to persist here, actual reset happens in resetPassword().
    }

    public boolean isEmailNotVerified(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .map(user -> !user.isEmailVerified())
                .orElse(false);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetOtp resetOtp = getValidResetOtp(request.getEmail(), request.getOtp());

        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("No account found with this email address."));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetOtp.setUsed(true);
        passwordResetOtpRepository.save(resetOtp);
    }

    private PasswordResetOtp getValidResetOtp(String email, String otp) {
        PasswordResetOtp resetOtp = passwordResetOtpRepository
                .findTopByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new BadRequestException("No active password reset request found. Please request a new OTP."));

        if (resetOtp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP expired. Please request a new password reset OTP.");
        }
        if (resetOtp.isUsed()) {
            throw new BadRequestException("This OTP has already been used. Please request a new one.");
        }
        if (!otpService.matches(otp, resetOtp.getOtpHash())) {
            throw new BadRequestException("Invalid OTP. Please check and try again.");
        }
        return resetOtp;
    }

    @Transactional
    public User updateProfile(User user, ProfileUpdateRequest request) {
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getMobile() != null && !request.getMobile().isBlank()) {
            if (!request.getMobile().matches("^[0-9]{10}$")) {
                throw new BadRequestException("Mobile number must be exactly 10 digits.");
            }
            user.setMobile(request.getMobile().trim());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(User user, ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void resendRegistrationOtp(String email) {
        PendingRegistration pending = pendingRegistrationRepository
                .findTopByEmailIgnoreCaseOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new BadRequestException("No pending registration found for this email. Please register again."));

        LocalDateTime lastSentAt = pending.getOtpExpiresAt().minusMinutes(OtpService.OTP_VALIDITY_MINUTES);
        if (lastSentAt.isAfter(LocalDateTime.now().minusSeconds(60))) {
            throw new BadRequestException("Please wait at least 1 minute before requesting a new OTP.");
        }

        String otp = otpService.generateOtp();
        pending.setOtpHash(otpService.hashOtp(otp));
        pending.setOtpExpiresAt(LocalDateTime.now().plusMinutes(OtpService.OTP_VALIDITY_MINUTES));
        pendingRegistrationRepository.save(pending);

        try {
            emailService.sendOtpEmail(pending.getEmail(), pending.getFullName(), "Mari-Fariyaad - Email Verification OTP", otp,
                    "resending your registration email verification OTP");
        } catch (MailException ex) {
            throw new BadRequestException("Failed to resend registration OTP email. Please verify your email address and try again.");
        }
    }
}
