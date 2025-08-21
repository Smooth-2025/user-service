package com.smooth.smooth_backend_user.repository;

import com.smooth.smooth_backend_user.entity.UserVehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserVehicleRepository extends JpaRepository<UserVehicle, Long> {

    Optional<UserVehicle> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    boolean existsByVehicleId(Long vehicleId);

}
