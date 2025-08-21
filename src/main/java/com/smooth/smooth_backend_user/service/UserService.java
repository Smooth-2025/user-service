package com.smooth.smooth_backend_user.service;

import com.smooth.smooth_backend_user.dto.request.ChangePasswordRequestDto;
import com.smooth.smooth_backend_user.dto.request.LoginRequestDto;
import com.smooth.smooth_backend_user.dto.request.RegisterRequestDto;
import com.smooth.smooth_backend_user.dto.request.UpdateEmergencyInfoRequestDto;
import com.smooth.smooth_backend_user.entity.User;
import com.smooth.smooth_backend_user.exception.AuthErrorCode;
import com.smooth.smooth_backend_user.exception.UserErrorCode;
import com.smooth.smooth_backend_user.global.exception.BusinessException;
import com.smooth.smooth_backend_user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private User createUserFromDto(RegisterRequestDto dto) {
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setName(dto.getName());
        user.setPhone(dto.getPhone());
        user.setGender(dto.getGender());

        // 응급정보
        user.setBloodType(dto.getBloodType());
        user.setEmergencyContact1(dto.getEmergencyContact1());
        user.setEmergencyContact2(dto.getEmergencyContact2());
        user.setEmergencyContact3(dto.getEmergencyContact3());

        // 약관동의
        user.setTermsOfServiceAgreed(dto.getTermsOfServiceAgreed());
        user.setPrivacyPolicyAgreed(dto.getPrivacyPolicyAgreed());
        user.setTermsAgreedAt(LocalDateTime.now());

        return user;
    }

    public boolean isEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public User register(RegisterRequestDto dto) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        }

        //필수 약관동의
        if (!Boolean.TRUE.equals(dto.getTermsOfServiceAgreed())) {
            throw new BusinessException(AuthErrorCode.TERMS_NOT_AGREED, "이용약관에 동의해주세요.");
        }

        if (!Boolean.TRUE.equals(dto.getPrivacyPolicyAgreed())) {
            throw new BusinessException(AuthErrorCode.TERMS_NOT_AGREED, "개인정보 처리방침에 동의해주세요.");
        }


        User user = createUserFromDto(dto);

        try {
            return userRepository.save(user);
        } catch (Exception e) {
            log.error("회원가입 중 오류 발생: {}", dto.getEmail(), e);
            throw new BusinessException(AuthErrorCode.EMAIL_ALREADY_EXISTS, "회원가입 처리 중 오류가 발생했습니다.");
        }

    }

    public User login(LoginRequestDto dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));

        // 비밀번호 확인
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        return user;
    }

    // 회원 정보 조회 (읽기 전용)
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    }

    // 사용자 삭제
    public void deleteAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        try {
            userRepository.delete(user);
            log.info("회원탈퇴 완료: 사용자 ID {}", userId);
        } catch (Exception e) {
            log.error("회원탈퇴 처리 중 오류 발생: 사용자 ID {}", userId, e);
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND, "회원탈퇴 처리 중 오류가 발생했습니다.");
        }
    }

    // 비밀번호 변경
    public void changePassword(Long userId, ChangePasswordRequestDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException(AuthErrorCode.CURRENT_PASSWORD_MISMATCH);
        }

        // 새 비밀번호 확인
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(AuthErrorCode.PASSWORD_CONFIRMATION_MISMATCH);
        }

        // 비밀번호 변경
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        log.info("비밀번호 변경 완료: 사용자 ID {}", userId);
    }

    // 응급정보 수정
    public User updateEmergencyInfo(Long userId, UpdateEmergencyInfoRequestDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        // 응급정보 업데이트
        user.setBloodType(dto.getBloodType());
        user.setEmergencyContact1(dto.getEmergencyContact1());
        user.setEmergencyContact2(dto.getEmergencyContact2());
        user.setEmergencyContact3(dto.getEmergencyContact3());

        User savedUser = userRepository.save(user);
        log.info("응급정보 수정 완료: 사용자 ID {}", userId);

        return savedUser;
    }

}