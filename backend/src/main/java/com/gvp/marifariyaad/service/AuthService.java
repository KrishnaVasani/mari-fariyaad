package com.gvp.marifariyaad.service;

import com.gvp.marifariyaad.dto.*;
import com.gvp.marifariyaad.entity.*;
import com.gvp.marifariyaad.exception.BadRequestException;
import com.gvp.marifariyaad.exception.ResourceNotFoundException;
import com.gvp.marifariyaad.repository.PasswordResetOtpRepository;
import com.gvp.marifariyaad.repository.PendingRegistrationRepository;
import com.gvp.marifariyaad.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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

        // Clear out any older pending registration attempts for this email
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

        boolean emailSent = emailService.sendOtpEmail(pending.getEmail(), "Mari-Fariyaad - Verify Your Email (OTP)", otp, "email verification / account registration");
        if (!emailSent) {
            throw new BadRequestException(
                    "We couldn't send the verification OTP to your email right now. " +
                    "This usually means the mail server isn't configured correctly. " +
                    "Please try again shortly or contact the admin.");
        }
    }

    @Transactional
    public User completeRegistration(VerifyRegistrationRequest request) {
        PendingRegistration pending = pendingRegistrationRepository
                .findTopByEmailIgnoreCaseOrderByCreatedAtDesc(request.getEmail())
                .orElseThrow(() -> new BadRequestException("No pending registration found for this email. Please register again."));

        if (pending.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            pendingRegistrationRepository.delete(pending);
            throw new BadRequestException("OTP expired. Please register again to receive a new OTP.");
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
                .build();

        User saved = userRepository.save(user);
        pendingRegistrationRepository.delete(pending);
        return saved;
    }

    @Transactional
    public void startForgotPassword(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with this email address."));

        String otp = otpService.generateOtp();
        PasswordResetOtp resetOtp = PasswordResetOtp.builder()
                .email(user.getEmail())
                .otpHash(otpService.hashOtp(otp))
                .expiresAt(LocalDateTime.now().plusMinutes(OtpService.OTP_VALIDITY_MINUTES))
                .used(false)
                .build();
        passwordResetOtpRepository.save(resetOtp);

        boolean emailSent = emailService.sendOtpEmail(user.getEmail(), "Mari-Fariyaad - Password Reset OTP", otp, "resetting your account password");
        if (!emailSent) {
            throw new BadRequestException(
                    "We couldn't send the password reset OTP to your email right now. " +
                    "This usually means the mail server isn't configured correctly. " +
                    "Please try again shortly or contact the admin.");
        }
    }

    @Transactional(readOnly = true)
    public void verifyResetOtp(String email, String otp) {
        PasswordResetOtp resetOtp = getValidResetOtp(email, otp);
        // Valid: nothing further to persist here, actual reset happens in resetPassword().
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
}
