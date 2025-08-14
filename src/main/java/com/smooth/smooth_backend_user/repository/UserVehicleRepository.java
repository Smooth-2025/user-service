package com.smooth.smooth_backend_user.repository;

import com.smooth.smooth_backend_user.entity.UserVehicle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserVehicleRepository extends JpaRepository<UserVehicle, Long> {
}
