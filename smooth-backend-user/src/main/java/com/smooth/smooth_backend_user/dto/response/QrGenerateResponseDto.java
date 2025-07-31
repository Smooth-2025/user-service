package com.smooth.smooth_backend_user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QrGenerateResponseDto extends CommonResponseDto {
    private String sessionToken;
    private String qrData;
    private Integer expiresIn;

    public static QrGenerateResponseDto success(String sessionToken, String qrData, Integer expiresIn) {
        QrGenerateResponseDto response = new QrGenerateResponseDto();
        response.setSuccess(true);
        response.setMessage("QR 코드가 생성되었습니다.");
        response.setSessionToken(sessionToken);
        response.setQrData(qrData);
        response.setExpiresIn(expiresIn);
        return response;
    }
    public static QrGenerateResponseDto error(String message) {
        QrGenerateResponseDto response = new QrGenerateResponseDto();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}