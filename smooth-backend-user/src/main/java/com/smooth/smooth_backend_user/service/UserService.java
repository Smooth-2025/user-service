package com.smooth.smooth_backend_user.service;

import com.smooth.smooth_backend_user.dto.request.LoginRequestDto;
import com.smooth.smooth_backend_user.dto.request.RegisterRequestDto;
import com.smooth.smooth_backend_user.entity.User;
import com.smooth.smooth_backend_user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public boolean isEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public User register(RegisterRequestDto dto) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("이미 존재하는 이메일입니다.");
        }

        //필수 약관동의
        if (!Boolean.TRUE.equals(dto.getTermsOfServiceAgreed())) {
            throw new RuntimeException("이용약관에 동의해주세요.");
        }

        if (!Boolean.TRUE.equals(dto.getPrivacyPolicyAgreed())) {
            throw new RuntimeException("개인정보 처리방침에 동의해주세요.");
        }

        // User 엔티티
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword())); // 비밀번호 암호화
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
        return userRepository.save(user);
    }

    public User login(LoginRequestDto dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("회원정보가 일치하지 않습니다."));

        // 비밀번호 확인
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("회원정보가 일치하지 않습니다.");
        }

        return user;
    }

    //사용자 찾기
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }

    //사용자 삭제
    public void deleteAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        userRepository.delete(user);
    }

}