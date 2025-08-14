package com.smooth.smooth_backend_user.repository;

import com.smooth.smooth_backend_user.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
}
