package com.smooth.smooth_backend_user.dto.request;

import com.smooth.smooth_backend_user.entity.User;
import lombok.Data;

@Data
public class UpdateEmergencyInfoRequestDto {

    private User.BloodType bloodType; // 선택사항

    private String emergencyContact1;

    private String emergencyContact2;

    private String emergencyContact3;
}