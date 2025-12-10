package com.example.demo.controllers;

import com.example.demo.dtos.DeviceDTO;
import com.example.demo.dtos.DeviceDetailsDTO;
import com.example.demo.entities.Device;
import com.example.demo.entities.UserDeviceMapping;
import com.example.demo.services.DeviceService;
import com.example.demo.services.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/devices")
@Validated
public class DeviceController {

    private final DeviceService deviceService;
    private final JwtService jwtService;
    private final org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    public DeviceController(DeviceService deviceService, JwtService jwtService,
            org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate) {
        this.deviceService = deviceService;
        this.jwtService = jwtService;
        this.rabbitTemplate = rabbitTemplate;
    }

    private void checkAdminRole(String authHeader) {
        try {
            String role = jwtService.getRoleFromToken(authHeader);
            if (role == null || !role.equals("ROLE_ADMIN")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Admin role required");
            }
        } catch (ParseException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Token");
        }
    }

    @GetMapping
    public ResponseEntity<List<DeviceDTO>> getDevices(@RequestHeader("Authorization") String authHeader) {
        checkAdminRole(authHeader);
        return ResponseEntity.ok(deviceService.findDevices());
    }

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody DeviceDetailsDTO device,
            @RequestHeader("Authorization") String authHeader) {
        checkAdminRole(authHeader);
        UUID id = deviceService.insert(device);

        // Publish device create event for synchronization
        rabbitTemplate.convertAndSend("device-exchange", "device.create", id.toString());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
        return ResponseEntity.created(location).build(); // 201 + Location header
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeviceDetailsDTO> getDevice(@PathVariable UUID id) {
        return ResponseEntity.ok(deviceService.findDevicesById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable UUID id, @RequestHeader("Authorization") String authHeader) {
        checkAdminRole(authHeader);
        boolean deleted = deviceService.delete(id);
        if (deleted) {
            // Publish device delete event for synchronization
            rabbitTemplate.convertAndSend("device-exchange", "device.delete", id.toString());
            return ResponseEntity.status(204).build();
        } else {
            return ResponseEntity.status(404).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeviceDetailsDTO> updateDevice(@Valid @RequestBody DeviceDetailsDTO deviceDetailsDTO,
            @PathVariable UUID id, @RequestHeader("Authorization") String authHeader) {
        checkAdminRole(authHeader);
        DeviceDetailsDTO deviceDTO = deviceService.update(deviceDetailsDTO);
        if (id.equals(deviceDTO.getId())) {
            return ResponseEntity.status(204).body(deviceDTO);
        } else {
            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(deviceDTO.getId())
                    .toUri();
            return ResponseEntity.created(location).build();
        }
    }

    @PostMapping("/mapping")
    public ResponseEntity<Void> assignDeviceToUser(@RequestParam UUID userId, @RequestParam UUID deviceId,
            @RequestHeader("Authorization") String authHeader) {
        checkAdminRole(authHeader);
        deviceService.assignDeviceToUser(userId, deviceId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/mapping")
    public ResponseEntity<Void> unassignDeviceFromUser(@RequestParam UUID userId, @RequestParam UUID deviceId,
            @RequestHeader("Authorization") String authHeader) {
        checkAdminRole(authHeader);
        deviceService.unassignDeviceFromUser(userId, deviceId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DeviceDTO>> getDevicesForUser(@PathVariable UUID userId) {// ,
                                                                                         // @RequestHeader("Authorization")
                                                                                         // String authHeader) {
        // checkAdminRole(authHeader);
        return ResponseEntity.ok(deviceService.findDevicesByUserId(userId));
    }

    @GetMapping("/user-mapping/{deviceId}")
    public ResponseEntity<String> getUserByDevice(@PathVariable UUID deviceId) {// , @RequestHeader("Authorization")
                                                                                // String authHeader) {
        // checkAdminRole(authHeader);
        UserDeviceMapping udm = deviceService.findAssignDevice(deviceId);
        try {
            return ResponseEntity.ok().body(udm.getUserId().toString());
        } catch (Exception e) {
            return ResponseEntity.ok().body("");
        }
    }

}
