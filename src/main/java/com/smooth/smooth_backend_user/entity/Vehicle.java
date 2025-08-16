package com.smooth.smooth_backend_user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "plate_number", "imei" }))
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plate_number", nullable = false, length = 20)
    private String plateNumber;

    @Column(name = "imei", nullable = false, length = 15)
    private String imei;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "vehicle", cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE },
            orphanRemoval = true)
    private List<UserVehicle> userVehicles = new ArrayList<>();


    public void addUserVehicle(UserVehicle userVehicle) {
        if (!userVehicles.contains(userVehicle)) {
            userVehicles.add(userVehicle);
            userVehicle.setVehicle(this);
        }
    }

    public void removeUserVehicle(UserVehicle userVehicle) {
        if (userVehicles.remove(userVehicle)) {
            userVehicle.setVehicle(null);
        }
    }

    public static Vehicle createVehicle(String plateNumber, String imei) {
        Vehicle vehicle = new Vehicle();
        vehicle.plateNumber = plateNumber;
        vehicle.imei = imei;
        vehicle.userVehicles = new ArrayList<>();
        return vehicle;
    }

}
