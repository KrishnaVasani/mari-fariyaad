package com.gvp.marifariyaad.dto;

import com.gvp.marifariyaad.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String mobile;
    private String gender;
    private String role;
    private String department;
    private String hostel;
    private String address;

    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getMobile(),
                user.getGender(),
                user.getRole().name(),
                user.getDepartment(),
                user.getHostel(),
                user.getAddress()
        );
    }
}
