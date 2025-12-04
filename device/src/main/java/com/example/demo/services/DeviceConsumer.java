package com.example.demo.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DeviceConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceConsumer.class);
    private final DeviceService deviceService;

    public DeviceConsumer(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @RabbitListener(queues = "user.delete.device-queue")
    public void receiveDeleteUser(UUID userId) {
        LOGGER.info("Received delete user event in device service: {}", userId);
        try {
            deviceService.deleteMappingsByUserId(userId);
        } catch (Exception e) {
            LOGGER.error("Error processing delete user event in device service", e);
        }
    }
}
