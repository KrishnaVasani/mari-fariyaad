package com.gvp.marifariyaad.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiMessageResponse {
    private boolean success;
    private String message;

    public static ApiMessageResponse of(boolean success, String message) {
        return new ApiMessageResponse(success, message);
    }
}
