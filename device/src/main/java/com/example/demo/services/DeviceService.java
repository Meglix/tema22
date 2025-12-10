package com.example.demo.services;

import com.example.demo.dtos.DeviceDTO;
import com.example.demo.dtos.DeviceDetailsDTO;
import com.example.demo.dtos.builders.DeviceBuilder;
import com.example.demo.entities.Device;
import com.example.demo.entities.UserDeviceMapping;
import com.example.demo.handlers.exceptions.model.ResourceNotFoundException;
import com.example.demo.repositories.DeviceRepository;
import com.example.demo.repositories.UserDeviceMappingRepository;
import com.example.demo.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DeviceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceService.class);
    private final DeviceRepository deviceRepository;
    private final UserDeviceMappingRepository mappingRepository;
    private final UserRepository userRepository;

    @Autowired
    public DeviceService(DeviceRepository deviceRepository, UserDeviceMappingRepository mappingRepository,
            UserRepository userRepository) {
        this.deviceRepository = deviceRepository;
        this.mappingRepository = mappingRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public List<DeviceDTO> findDevices() {
        List<Device> deviceList = deviceRepository.findAll();
        return deviceList.stream()
                .map(DeviceBuilder::toDeviceDTO)
                .collect(Collectors.toList());
    }

    public DeviceDetailsDTO findDevicesById(UUID id) {
        Optional<Device> prosumerOptional = deviceRepository.findById(id);
        if (prosumerOptional.isEmpty()) {
            LOGGER.error("Device with id {} was not found in db", id);
            throw new ResourceNotFoundException(Device.class.getSimpleName() + " with id: " + id);
        }
        return DeviceBuilder.toDeviceDetailsDTO(prosumerOptional.get());
    }

    public UUID insert(DeviceDetailsDTO deviceDTO) {
        Device device = DeviceBuilder.toEntity(deviceDTO);
        device = deviceRepository.save(device);
        LOGGER.debug("Device with id {} was inserted in db", device.getId());
        return device.getId();
    }

    @Transactional
    public DeviceDetailsDTO update(DeviceDetailsDTO deviceDTO) {
        Device device = deviceRepository.findById(deviceDTO.getId())
                .map(existingDevice -> {
                    LOGGER.debug("Device with id {} will be updated in db", deviceDTO.getId());
                    existingDevice.setName(deviceDTO.getName());
                    existingDevice.setManufacturer(deviceDTO.getManufacturer());
                    existingDevice.setConsumption(deviceDTO.getConsumption());
                    return existingDevice;
                })
                .orElseGet(() -> {
                    LOGGER.debug("Device with id {} will be inserted in db", deviceDTO.getId());
                    return DeviceBuilder.toEntity(deviceDTO);
                });

        Device savedDevice = deviceRepository.save(device);
        return DeviceBuilder.toDeviceDetailsDTO(savedDevice);
    }

    @Transactional
    public boolean delete(UUID id) {
        if (deviceRepository.existsById(id)) {
            deviceRepository.deleteById(id);
            LOGGER.debug("Device with id {} was deleted from db", id);
            return true;
        } else {
            LOGGER.debug("Device with id {} was not found in db", id);
            return false;
        }
    }

    @Transactional
    public void assignDeviceToUser(UUID userId, UUID deviceId) {
        // Check if user exists in local users table (synchronized from user service)
        if (!userRepository.existsById(userId)) {
            LOGGER.error("User with id {} was not found in device service database", userId);
            throw new ResourceNotFoundException("User with id: " + userId + " not found. User must be created first.");
        }

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> {
                    LOGGER.error("Device with id {} was not found in db", deviceId);
                    return new ResourceNotFoundException(Device.class.getSimpleName() + " with id: " + deviceId);
                });

        UserDeviceMapping newMapping = new UserDeviceMapping(userId, device);
        try {
            mappingRepository.save(newMapping);
            LOGGER.debug("Assigned device {} to user {}", deviceId, userId);
        } catch (DataIntegrityViolationException e) {
            LOGGER.warn("Device {} is already assigned to user {}", deviceId, userId);
            throw new RuntimeException("Device is already assigned to this user");
        }
    }

    @Transactional
    public void unassignDeviceFromUser(UUID userId, UUID deviceId) {
        UserDeviceMapping mapping = mappingRepository.findByUserIdAndDevice_Id(userId, deviceId)
                .orElseThrow(() -> {
                    LOGGER.error("Mapping not found for user {} and device {}", userId, deviceId);
                    return new ResourceNotFoundException("Mapping not found for this user and device");
                });

        mappingRepository.delete(mapping);
        LOGGER.debug("Unassigned device {} from user {}", deviceId, userId);
    }

    @Transactional
    public List<DeviceDTO> findDevicesByUserId(UUID userId) {
        List<UserDeviceMapping> mappings = mappingRepository.findByUserId(userId);

        return mappings.stream()
                .map(UserDeviceMapping::getDevice)
                .map(DeviceBuilder::toDeviceDTO)
                .collect(Collectors.toList());
    }

    public @Nullable UserDeviceMapping findAssignDevice(UUID deviceId) {
        Optional<Device> device = deviceRepository.findById(deviceId);
        if (device.isEmpty())
            throw new ResourceNotFoundException("Device is not assigned");

        return mappingRepository.findByDevice(device.get().getId());
    }

    @Transactional
    public void deleteMappingsByUserId(UUID userId) {
        List<UserDeviceMapping> mappings = mappingRepository.findByUserId(userId);
        mappingRepository.deleteAll(mappings);
        LOGGER.debug("Deleted all mappings for user {}", userId);
    }
}
