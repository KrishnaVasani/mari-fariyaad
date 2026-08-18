package com.gvp.marifariyaad.dto;

import com.gvp.marifariyaad.entity.Complaint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComplaintResponse {
    private String ticketId;
    private String title;
    private String category;
    private String categoryName;
    private String locationType;
    private String building;
    private String floor;
    private String room;
    private String department;
    private String hostel;
    private String description;
    private String priority;
    private String status;
    private String assignedTo;
    private String photoUrl;
    private String videoUrl;
    private LocalDateTime submittedAt;
    private String fullName;
    private String email;
    private String mobile;
    private String role;
    private List<ComplaintTimelineResponse> timeline;

    public static ComplaintResponse fromEntity(Complaint c) {
        ComplaintResponse r = new ComplaintResponse();
        r.setTicketId(c.getTicketId());
        r.setTitle(c.getTitle());
        r.setCategory(c.getCategory());
        r.setCategoryName(c.getCategoryName());
        r.setLocationType(c.getLocationType());
        r.setBuilding(c.getBuilding());
        r.setFloor(c.getFloor());
        r.setRoom(c.getRoom());
        r.setDepartment(c.getDepartment());
        r.setHostel(c.getHostel());
        r.setDescription(c.getDescription());
        r.setPriority(c.getPriority());
        r.setStatus(c.getStatus().name().replace("_", " "));
        r.setAssignedTo(c.getAssignedTo());
        r.setPhotoUrl(c.getPhotoPath() != null ? "/uploads/complaints/photos/" + c.getPhotoPath() : null);
        r.setVideoUrl(c.getVideoPath() != null ? "/uploads/complaints/videos/" + c.getVideoPath() : null);
        r.setSubmittedAt(c.getSubmittedAt());
        if (c.getUser() != null) {
            r.setFullName(c.getUser().getFullName());
            r.setEmail(c.getUser().getEmail());
            r.setMobile(c.getUser().getMobile());
            r.setRole(c.getUser().getRole().name());
        }
        if (c.getTimeline() != null) {
            r.setTimeline(c.getTimeline().stream()
                    .map(ComplaintTimelineResponse::fromEntity)
                    .collect(Collectors.toList()));
        }
        return r;
    }
}
