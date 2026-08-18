package com.gvp.marifariyaad.repository;

import com.gvp.marifariyaad.entity.Complaint;
import com.gvp.marifariyaad.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    Optional<Complaint> findByTicketIdIgnoreCase(String ticketId);
    List<Complaint> findAllByUserOrderBySubmittedAtDesc(User user);
    List<Complaint> findAllByOrderBySubmittedAtDesc();
    boolean existsByTicketId(String ticketId);
}
