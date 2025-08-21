package com.smooth.smooth_backend_user.dto.response;

import com.smooth.smooth_backend_user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponseDto {
    private Long id;
    private String email;
    private String name;
    private String phone;
    private User.Gender gender;
    private User.BloodType bloodType;
    private String emergencyContact1;
    private String emergencyContact2;
    private String emergencyContact3;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserProfileResponseDto fromUser(User user) {
        return UserProfileResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .gender(user.getGender())
                .bloodType(user.getBloodType())
                .emergencyContact1(user.getEmergencyContact1())
                .emergencyContact2(user.getEmergencyContact2())
                .emergencyContact3(user.getEmergencyContact3())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}