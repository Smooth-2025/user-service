package com.smooth.smooth_backend_user.service;

import com.smooth.smooth_backend_user.dto.request.LinkVehicleRequestDto;
import com.smooth.smooth_backend_user.dto.response.LinkVehicleResponseDto;
import com.smooth.smooth_backend_user.entity.User;
import com.smooth.smooth_backend_user.entity.UserVehicle;
import com.smooth.smooth_backend_user.entity.Vehicle;
import com.smooth.smooth_backend_user.exception.UserErrorCode;
import com.smooth.smooth_backend_user.exception.UserVehicleErrorCode;
import com.smooth.smooth_backend_user.global.exception.BusinessException;
import com.smooth.smooth_backend_user.repository.UserRepository;
import com.smooth.smooth_backend_user.repository.UserVehicleRepository;
import com.smooth.smooth_backend_user.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserVehicleService {
    private final UserRepository userRepository;
    private final UserVehicleRepository userVehicleRepository;
    private final VehicleRepository vehicleRepository;

    public  LinkVehicleResponseDto getRegisteredVehicle(Long userId) {
        UserVehicle userVehicle = userVehicleRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(UserVehicleErrorCode.LINK_NOT_FOUND));

        return LinkVehicleResponseDto.fromUserVehicle(userVehicle);
    }


    @Transactional
    public LinkVehicleResponseDto linkVehicle (Long userId, LinkVehicleRequestDto vehicleInfo ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (userVehicleRepository.existsByUserId(userId)) {
            throw new BusinessException(UserVehicleErrorCode.VEHICLE_LINK_CONFLICT);
        }

        String plateNumber = vehicleInfo.getPlateNumber();
        String imei = vehicleInfo.getImei();

        Vehicle vehicle = getOrCreateVehicle(plateNumber, imei);

        UserVehicle newUserVehicle = UserVehicle.createUserVehicle(user, vehicle);
        userVehicleRepository.save(newUserVehicle);

        return LinkVehicleResponseDto.fromUserVehicle(newUserVehicle);

    }

    private Vehicle getOrCreateVehicle(String plateNumber, String imei) {
        return vehicleRepository.findByPlateNumberAndImei(plateNumber, imei)
                .orElseGet(() -> {
                    try {
                        return vehicleRepository.save(Vehicle.createVehicle(plateNumber, imei));
                    } catch (DataIntegrityViolationException e) {
                        return vehicleRepository.findByPlateNumberAndImei(plateNumber, imei)
                                .orElseThrow(() -> e);
                    }
                });
    }


    @Transactional
    public void unlinkVehicle(Long userId) {
        UserVehicle userVehicle = userVehicleRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(UserVehicleErrorCode.LINK_NOT_FOUND));

        Vehicle vehicle = userVehicle.getVehicle();
        Long vehicleId = vehicle.getId();

        vehicle.removeUserVehicle(userVehicle);

        boolean stillLinked = userVehicleRepository.existsByVehicleId(vehicleId);
        if (!stillLinked) {
            vehicleRepository.delete(vehicle);
        }
    }





}
