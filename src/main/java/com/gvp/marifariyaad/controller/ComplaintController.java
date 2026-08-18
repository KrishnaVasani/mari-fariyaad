package com.gvp.marifariyaad.controller;

import com.gvp.marifariyaad.dto.*;
import com.gvp.marifariyaad.entity.Complaint;
import com.gvp.marifariyaad.entity.ComplaintStatus;
import com.gvp.marifariyaad.entity.User;
import com.gvp.marifariyaad.security.UserPrincipal;
import com.gvp.marifariyaad.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ComplaintResponse> submit(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String title,
            @RequestParam String category,
            @RequestParam(required = false) String categoryName,
            @RequestParam String locationType,
            @RequestParam String building,
            @RequestParam(required = false) String floor,
            @RequestParam(required = false) String room,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String hostel,
            @RequestParam String description,
            @RequestParam String priority,
            @RequestParam(required = false) MultipartFile photo,
            @RequestParam(required = false) MultipartFile video) {

        Complaint complaint = complaintService.submitComplaint(
                principal.getUser(), title, category, categoryName, locationType, building, floor, room,
                department, hostel, description, priority, photo, video);

        return ResponseEntity.ok(ComplaintResponse.fromEntity(complaint));
    }

    @GetMapping
    public ResponseEntity<List<ComplaintResponse>> myComplaints(@AuthenticationPrincipal UserPrincipal principal) {
        List<ComplaintResponse> list = complaintService.getComplaintsForUser(principal.getUser()).stream()
                .map(ComplaintResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> myStats(@AuthenticationPrincipal UserPrincipal principal) {
        List<Complaint> list = complaintService.getComplaintsForUser(principal.getUser());
        long total = list.size();
        long pending = list.stream().filter(c -> c.getStatus() == ComplaintStatus.Pending).count();
        long inProgress = list.stream().filter(c -> c.getStatus() == ComplaintStatus.In_Progress || c.getStatus() == ComplaintStatus.Assigned).count();
        long resolved = list.stream().filter(c -> c.getStatus() == ComplaintStatus.Resolved).count();
        return ResponseEntity.ok(new DashboardStatsResponse(total, pending, inProgress, resolved));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ComplaintResponse>> search(@AuthenticationPrincipal UserPrincipal principal,
                                                            @RequestParam String query) {
        List<ComplaintResponse> list = complaintService.search(query, principal.getUser()).stream()
                .map(ComplaintResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<ComplaintResponse> getByTicketId(@AuthenticationPrincipal UserPrincipal principal,
                                                             @PathVariable String ticketId) {
        Complaint complaint = complaintService.getByTicketId(ticketId, principal.getUser());
        return ResponseEntity.ok(ComplaintResponse.fromEntity(complaint));
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<ComplaintResponse>> allComplaints() {
        List<ComplaintResponse> list = complaintService.getAllComplaints().stream()
                .map(ComplaintResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PutMapping("/{ticketId}/status")
    public ResponseEntity<ComplaintResponse> updateStatus(@PathVariable String ticketId,
                                                            @Valid @RequestBody ComplaintStatusUpdateRequest request) {
        Complaint updated = complaintService.updateStatus(ticketId, request);
        return ResponseEntity.ok(ComplaintResponse.fromEntity(updated));
    }
}
