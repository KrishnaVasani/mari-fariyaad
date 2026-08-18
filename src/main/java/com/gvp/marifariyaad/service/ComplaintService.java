package com.gvp.marifariyaad.service;

import com.gvp.marifariyaad.dto.ComplaintStatusUpdateRequest;
import com.gvp.marifariyaad.entity.Complaint;
import com.gvp.marifariyaad.entity.ComplaintStatus;
import com.gvp.marifariyaad.entity.ComplaintTimeline;
import com.gvp.marifariyaad.entity.User;
import com.gvp.marifariyaad.exception.BadRequestException;
import com.gvp.marifariyaad.exception.ResourceNotFoundException;
import com.gvp.marifariyaad.exception.UnauthorizedException;
import com.gvp.marifariyaad.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final FileStorageService fileStorageService;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Transactional
    public Complaint submitComplaint(User user,
                                      String title, String category, String categoryName,
                                      String locationType, String building, String floor, String room,
                                      String department, String hostel, String description, String priority,
                                      MultipartFile photo, MultipartFile video) {

        requireNotBlank(title, "Complaint title");
        requireNotBlank(category, "Complaint category");
        requireNotBlank(locationType, "Location sector");
        requireNotBlank(building, "Building name / block");
        requireNotBlank(description, "Complaint description");
        requireNotBlank(priority, "Priority level");

        String photoFileName = fileStorageService.storePhoto(photo);
        String videoFileName = fileStorageService.storeVideo(video);

        Complaint complaint = Complaint.builder()
                .ticketId(generateUniqueTicketId())
                .user(user)
                .title(title.trim())
                .category(category)
                .categoryName(categoryName)
                .locationType(locationType)
                .building(building.trim())
                .floor(floor)
                .room(room)
                .department(department)
                .hostel(hostel)
                .description(description.trim())
                .priority(priority)
                .status(ComplaintStatus.Pending)
                .assignedTo(null)
                .photoPath(photoFileName)
                .photoOriginalName(photo != null ? photo.getOriginalFilename() : null)
                .videoPath(videoFileName)
                .videoOriginalName(video != null ? video.getOriginalFilename() : null)
                .build();

        Complaint saved = complaintRepository.save(complaint);

        ComplaintTimeline entry = ComplaintTimeline.builder()
                .complaint(saved)
                .status(ComplaintStatus.Pending)
                .note("Complaint submitted successfully by " + user.getFullName() + ".")
                .build();
        saved.getTimeline().add(entry);

        return complaintRepository.save(saved);
    }

    @Transactional(readOnly = true)
    public List<Complaint> getComplaintsForUser(User user) {
        return complaintRepository.findAllByUserOrderBySubmittedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAllByOrderBySubmittedAtDesc();
    }

    @Transactional(readOnly = true)
    public Complaint getByTicketId(String ticketId, User requestingUser) {
        Complaint complaint = complaintRepository.findByTicketIdIgnoreCase(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("No complaint found with ticket ID: " + ticketId));
        enforceOwnershipOrAdmin(complaint, requestingUser);
        return complaint;
    }

    @Transactional(readOnly = true)
    public List<Complaint> search(String query, User requestingUser) {
        boolean isAdmin = requestingUser.getRole().name().equals("ADMIN");
        List<Complaint> base = isAdmin ? complaintRepository.findAllByOrderBySubmittedAtDesc()
                : complaintRepository.findAllByUserOrderBySubmittedAtDesc(requestingUser);

        String normalized = query == null ? "" : query.trim().toLowerCase();
        if (normalized.isEmpty()) return base;

        return base.stream()
                .filter(c -> c.getTicketId().toLowerCase().equals(normalized)
                        || (c.getUser() != null && c.getUser().getEmail().toLowerCase().equals(normalized)))
                .toList();
    }

    @Transactional
    public Complaint updateStatus(String ticketId, ComplaintStatusUpdateRequest request) {
        Complaint complaint = complaintRepository.findByTicketIdIgnoreCase(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("No complaint found with ticket ID: " + ticketId));

        ComplaintStatus newStatus;
        try {
            newStatus = ComplaintStatus.valueOf(request.getStatus().trim().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status value: " + request.getStatus());
        }

        complaint.setStatus(newStatus);
        if (request.getAssignedTo() != null && !request.getAssignedTo().isBlank()) {
            complaint.setAssignedTo(request.getAssignedTo().trim());
        }

        Complaint saved = complaintRepository.save(complaint);

        String note = request.getNote() != null && !request.getNote().isBlank()
                ? request.getNote()
                : "Status updated to " + newStatus.name().replace("_", " ") + "."
                + (complaint.getAssignedTo() != null ? " Assigned to: " + complaint.getAssignedTo() + "." : "");

        ComplaintTimeline entry = ComplaintTimeline.builder()
                .complaint(saved)
                .status(newStatus)
                .note(note)
                .build();
        saved.getTimeline().add(entry);

        return complaintRepository.save(saved);
    }

    public void enforceOwnershipOrAdmin(Complaint complaint, User requestingUser) {
        boolean isAdmin = requestingUser.getRole().name().equals("ADMIN");
        boolean isOwner = complaint.getUser() != null && complaint.getUser().getId().equals(requestingUser.getId());
        if (!isAdmin && !isOwner) {
            throw new UnauthorizedException("You are not authorized to view this complaint.");
        }
    }

    private void requireNotBlank(String value, String fieldLabel) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(fieldLabel + " is required.");
        }
    }

    private String generateUniqueTicketId() {
        String ticketId;
        int attempts = 0;
        do {
            int randomNum = 100000 + SECURE_RANDOM.nextInt(900000);
            ticketId = "GVP-" + Year.now().getValue() + "-" + randomNum;
            attempts++;
        } while (complaintRepository.existsByTicketId(ticketId) && attempts < 20);
        return ticketId;
    }
}
