package com.gvp.marifariyaad.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ComplaintStatusUpdateRequest {

    @NotBlank(message = "Status is required")
    private String status;

    private String assignedTo;

    private String note;
}
