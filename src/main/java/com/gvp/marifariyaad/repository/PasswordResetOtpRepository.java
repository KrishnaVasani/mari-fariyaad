package com.gvp.marifariyaad.repository;

import com.gvp.marifariyaad.entity.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {
    Optional<PasswordResetOtp> findTopByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc(String email);
    List<PasswordResetOtp> findAllByEmailIgnoreCaseAndUsedFalse(String email);
}
