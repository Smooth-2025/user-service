package com.smooth.smooth_backend_user.user.repository;

import com.smooth.smooth_backend_user.user.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByPlateNumberAndImei(String plateNumber, String imei);
}
