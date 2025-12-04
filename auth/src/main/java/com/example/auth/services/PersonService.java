package com.example.auth.services;

import com.example.auth.dtos.PersonDTO;
import com.example.auth.dtos.RegisterDTO;
import com.example.auth.dtos.builders.PersonBuilder;
import com.example.auth.dtos.builders.PersonSyncDTO;
import com.example.auth.entities.Person;
import com.example.auth.handlers.exceptions.model.ResourceNotFoundException;
import com.example.auth.repositories.PersonRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class PersonService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersonService.class);
    private final PersonRepository personRepository;
    private final PasswordEncoder passwordEncoder;
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public PersonService(PersonRepository personRepository, PasswordEncoder passwordEncoder, RabbitTemplate rabbitTemplate) {
        this.personRepository = personRepository;
        this.passwordEncoder = passwordEncoder;
        this.rabbitTemplate = rabbitTemplate;
    }

    public UUID insert(RegisterDTO user) {
        UUID newUserId = UUID.randomUUID();
        Person person = personRepository.save(
                new Person(newUserId, user.getUsername(), passwordEncoder.encode(user.getPassword()), false));
        LOGGER.debug("Person with id {} was inserted in db", person.getId());

        PersonSyncDTO userSyncRequest = new PersonSyncDTO(newUserId, user.getName(), user.getAddress(), user.getAge());
        try {
            rabbitTemplate.convertAndSend("user-exchange", "user.create", userSyncRequest);
            LOGGER.debug("Sent user creation event for id {}", newUserId);
        } catch (Exception e) {
            personRepository.deleteById(newUserId);
            throw new RuntimeException("Error creating user: " + e.getMessage());
        }
        return person.getId();
    }

    public UUID getUserId(String username) {
        Optional<Person> person = personRepository.findByUsername(username);
        return person.get().getId();
    }

    public Person getPersonByUsernameAndAdmin(String username) {
        Optional<Person> person = personRepository.findPersonByUsernameAndAdmin(username);
        if (person.isEmpty()) {
            LOGGER.debug("Person with username {} does not have administrator rights", username);
            throw new ResourceNotFoundException("User is not");
        } else {
            return person.get();
        }
    }

    @Transactional
    public void deletePerson(UUID uuid) {
        personRepository.deleteById(uuid);
        try {
            rabbitTemplate.convertAndSend("user-exchange", "user.delete", uuid);
            LOGGER.debug("Sent user deletion event for id {}", uuid);
        } catch (Exception e) {
            throw new RuntimeException("Error deleting person in user service: " + e.getMessage(), e);
        }
    }
}
