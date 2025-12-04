package com.example.demo.repositories;

import com.example.demo.entities.Device;
import com.example.demo.entities.UserDeviceMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserDeviceMappingRepository extends JpaRepository<UserDeviceMapping, UUID> {
    Optional<UserDeviceMapping> findByUserIdAndDevice_Id(UUID userId, UUID deviceId);
    List<UserDeviceMapping> findByUserId(UUID userId);
    @Query("SELECT m from UserDeviceMapping m " +
            "where m.device.id = :id")
    UserDeviceMapping findByDevice(@Param("id")UUID id);
}