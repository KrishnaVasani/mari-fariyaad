package com.gvp.marifariyaad.dto;

import com.gvp.marifariyaad.entity.ComplaintTimeline;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComplaintTimelineResponse {
    private String status;
    private String note;
    private LocalDateTime createdAt;

    public static ComplaintTimelineResponse fromEntity(ComplaintTimeline t) {
        return new ComplaintTimelineResponse(
                t.getStatus().name().replace("_", " "),
                t.getNote(),
                t.getCreatedAt()
        );
    }
}
