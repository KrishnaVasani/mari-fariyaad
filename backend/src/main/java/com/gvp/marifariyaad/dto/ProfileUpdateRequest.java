package com.gvp.marifariyaad.dto;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String fullName;
    private String mobile;
    private String address;
}
