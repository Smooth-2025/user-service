package com.smooth.smooth_backend_user.user.repository;

import com.smooth.smooth_backend_user.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByLoginId(String loginId);
    boolean existsByEmail(String email);
    boolean existsByLoginId(String loginId);

    // 벌크 조회: 캐릭터가 있는 유저들만
    @Query("SELECT u FROM User u WHERE u.characterType IS NOT NULL")
    List<User> findUsersWithCharacter();

    // 단건 조회: 특정 유저의 캐릭터 확인
    @Query("SELECT u.characterType FROM User u WHERE u.id = :userId")
    Optional<String> findCharacterTypeByUserId(@Param("userId") Long userId);
}