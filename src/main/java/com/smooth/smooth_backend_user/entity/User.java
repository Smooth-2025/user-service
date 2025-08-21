package com.smooth.smooth_backend_user.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    private BloodType bloodType;

    @Column(name = "emergency_contact_1")
    private String emergencyContact1;

    @Column(name = "emergency_contact_2")
    private String emergencyContact2;

    @Column(name = "emergency_contact_3")
    private String emergencyContact3;

    //이용약관
    @Column(name = "terms_of_service_agreed", nullable = false)
    private Boolean termsOfServiceAgreed = false;

    //개인정보 처리방침
    @Column(name = "privacy_policy_agreed", nullable = false)
    private Boolean privacyPolicyAgreed = false;

    @Column(name = "terms_agreed_at")
    private LocalDateTime termsAgreedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;


    @OneToOne(fetch = FetchType.LAZY, mappedBy = "user", optional = true)
    private UserVehicle userVehicle;

    public enum Gender {
        MALE, FEMALE
    }

    public enum BloodType {
        A, B, AB, O
    }
}