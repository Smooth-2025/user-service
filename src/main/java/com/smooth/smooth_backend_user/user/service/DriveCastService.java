package com.smooth.smooth_backend_user.user.service;

import com.smooth.smooth_backend_user.user.client.dto.EmergencyInfoResponse;
import com.smooth.smooth_backend_user.user.client.dto.TraitsBulkResponse;
import com.smooth.smooth_backend_user.user.client.dto.UserTraitResponse;
import com.smooth.smooth_backend_user.user.entity.User;
import com.smooth.smooth_backend_user.user.exception.UserErrorCode;
import com.smooth.smooth_backend_user.global.exception.BusinessException;
import com.smooth.smooth_backend_user.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriveCastService {

    private final UserRepository userRepository;

    // 벌크 조회: 캐릭터가 있는 유저들만 반환
    public TraitsBulkResponse getBulkTraits(Boolean hasCharacter) {
        log.info("벌크 성향 조회 요청: hasCharacter={}", hasCharacter);

        List<User> users;
        if (hasCharacter) {
            // 캐릭터가 있는 유저만 조회
            users = userRepository.findUsersWithCharacter();
        } else {
            // 모든 유저 조회 (필요시)
            users = userRepository.findAll();
        }

        List<TraitsBulkResponse.TraitData> traitDataList = users.stream()
                .filter(user -> user.getCharacterType() != null) // 혹시 모를 null 체크
                .map(user -> TraitsBulkResponse.TraitData.builder()
                        .userId(user.getId().toString())
                        .character(user.getCharacterType())
                        .build())
                .collect(Collectors.toList());

        log.info("벌크 성향 조회 완료: 총 {}명", traitDataList.size());

        return TraitsBulkResponse.builder()
                .data(traitDataList)
                .generatedAtUtc(Instant.now().toString())
                .build();
    }

    // 단건 조회: 특정 유저의 캐릭터 조회
    public UserTraitResponse getUserTrait(String userId) {
        log.info("단건 성향 조회 요청: userId={}", userId);

        Long userIdLong;
        try {
            userIdLong = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            log.warn("잘못된 userId 형식: {}", userId);
            throw new BusinessException(UserErrorCode.INVALID_USER_ID);
        }

        Optional<String> characterType = userRepository.findCharacterTypeByUserId(userIdLong);

        if (characterType.isEmpty() || characterType.get() == null) {
            log.warn("성향 정보 없음: userId={}", userId);
            throw new BusinessException(UserErrorCode.TRAIT_NOT_FOUND);
        }

        log.info("단건 성향 조회 완료: userId={}, character={}", userId, characterType.get());

        return UserTraitResponse.builder()
                .userId(userId)
                .character(characterType.get())
                .build();
    }

    // 119 신고용 응급정보 조회
    public EmergencyInfoResponse getEmergencyInfo(String userId) {
        log.info("응급정보 조회 요청: userId={}", userId);

        Long userIdLong;
        try {
            userIdLong = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            log.warn("잘못된 userId 형식: {}", userId);
            throw new BusinessException(UserErrorCode.INVALID_USER_ID);
        }

        User user = userRepository.findById(userIdLong)
                .orElseThrow(() -> {
                    log.warn("사용자 없음: userId={}", userId);
                    return new BusinessException(UserErrorCode.USER_NOT_FOUND);
                });

        // 응급정보가 있는지 확인 (최소한 혈액형이나 연락처 중 하나는 있어야 함)
        if (isEmergencyInfoEmpty(user)) {
            log.warn("응급정보 없음: userId={}", userId);
            throw new BusinessException(UserErrorCode.EMERGENCY_INFO_NOT_AVAILABLE);
        }

        EmergencyInfoResponse.EmergencyData emergencyData = EmergencyInfoResponse.EmergencyData.builder()
                .userId(userId)
                .gender(user.getGender() != null ? user.getGender().toString() : null)
                .bloodType(user.getBloodType() != null ? user.getBloodType().toString() : null)
                .emergencyContact1(user.getEmergencyContact1())
                .emergencyContact2(user.getEmergencyContact2())
                .emergencyContact3(user.getEmergencyContact3())
                .build();

        log.info("응급정보 조회 완료: userId={}", userId);

        return EmergencyInfoResponse.builder()
                .code("OK")
                .message(null)
                .data(emergencyData)
                .build();
    }

    // 응급정보가 비어있는지 확인
    private boolean isEmergencyInfoEmpty(User user) {
        return (user.getBloodType() == null) &&
                (user.getEmergencyContact1() == null || user.getEmergencyContact1().trim().isEmpty()) &&
                (user.getEmergencyContact2() == null || user.getEmergencyContact2().trim().isEmpty()) &&
                (user.getEmergencyContact3() == null || user.getEmergencyContact3().trim().isEmpty());
    }
}