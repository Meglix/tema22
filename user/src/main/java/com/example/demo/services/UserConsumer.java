package com.example.demo.services;

import com.example.demo.dtos.PersonDetailsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserConsumer.class);
    private final PersonService personService;

    public UserConsumer(PersonService personService) {
        this.personService = personService;
    }

    @RabbitListener(queues = "user.create.queue")
    public void receiveCreateUser(com.example.demo.dtos.PersonSyncDTO personSyncDTO) {
        LOGGER.info("Received create user event: {}", personSyncDTO);
        try {
            PersonDetailsDTO personDetailsDTO = new PersonDetailsDTO(
                personSyncDTO.getId(),
                personSyncDTO.getName(),
                personSyncDTO.getAddress(),
                personSyncDTO.getAge()
            );
            personService.insert(personDetailsDTO);
        } catch (Exception e) {
            LOGGER.error("Error processing create user event", e);
        }
    }

    @RabbitListener(queues = "user.delete.user-queue")
    public void receiveDeleteUser(UUID userId) {
        LOGGER.info("Received delete user event: {}", userId);
        try {
            personService.delete(userId);
        } catch (Exception e) {
            LOGGER.error("Error processing delete user event", e);
        }
    }
}
