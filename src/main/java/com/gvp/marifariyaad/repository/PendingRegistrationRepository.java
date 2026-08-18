package com.gvp.marifariyaad.repository;

import com.gvp.marifariyaad.entity.PendingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, Long> {
    Optional<PendingRegistration> findTopByEmailIgnoreCaseOrderByCreatedAtDesc(String email);
    List<PendingRegistration> findAllByEmailIgnoreCase(String email);
}
