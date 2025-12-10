package com.example.demo.services;

import com.example.demo.entities.User;
import com.example.demo.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DeviceConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceConsumer.class);
    private final DeviceService deviceService;
    private final UserRepository userRepository;

    public DeviceConsumer(DeviceService deviceService, UserRepository userRepository) {
        this.deviceService = deviceService;
        this.userRepository = userRepository;
    }

    @RabbitListener(queues = "user.create.device-queue")
    public void receiveCreateUser(String message) {
        LOGGER.info("Received create user event in device service: {}", message);
        try {
            String userIdStr = message;
            // Check if message is JSON (starts with {)
            if (message.trim().startsWith("{")) {
                // primitive JSON parsing to avoid adding Jackson dependency if not present
                // assumes format {"id":"uuid",...}
                int idIndex = message.indexOf("\"id\"");
                if (idIndex != -1) {
                    int colonIndex = message.indexOf(":", idIndex);
                    int quoteStart = message.indexOf("\"", colonIndex + 1);
                    int quoteEnd = message.indexOf("\"", quoteStart + 1);
                    userIdStr = message.substring(quoteStart + 1, quoteEnd);
                }
            } else {
                // It's likely a simple string, clean it
                userIdStr = message.replaceAll("\"", "");
            }

            // Extract UUID from result (keep only valid UUID characters)
            String cleanUuid = userIdStr.replaceAll("[^a-fA-F0-9\\-]", "");
            UUID userId = UUID.fromString(cleanUuid);

            // Insert user ID in local users table
            User user = new User(userId);
            userRepository.save(user);
            LOGGER.info("Synchronized user {} in device service database", userId);
        } catch (Exception e) {
            LOGGER.error("Error processing create user event in device service", e);
        }
    }

    @RabbitListener(queues = "user.delete.device-queue")
    public void receiveDeleteUser(String userIdStr) {
        LOGGER.info("Received delete user event in device service: {}", userIdStr);
        try {
            // Extract UUID from message (keep only valid UUID characters: a-f, A-F, 0-9,
            // and -)
            String cleanUuid = userIdStr.replaceAll("[^a-fA-F0-9\\-]", "");
            UUID userId = UUID.fromString(cleanUuid);
            // Delete user mappings first
            deviceService.deleteMappingsByUserId(userId);
            // Delete user from users table
            userRepository.deleteById(userId);
            LOGGER.info("Deleted user {} from device service database", userId);
        } catch (Exception e) {
            LOGGER.error("Error processing delete user event in device service", e);
        }
    }
}
