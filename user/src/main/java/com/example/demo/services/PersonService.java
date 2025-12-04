package com.example.demo.services;


import com.example.demo.dtos.PersonDTO;
import com.example.demo.dtos.PersonDetailsDTO;
import com.example.demo.dtos.builders.PersonBuilder;
import com.example.demo.entities.Person;
import com.example.demo.handlers.exceptions.model.ResourceNotFoundException;
import com.example.demo.repositories.PersonRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PersonService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersonService.class);
    private final PersonRepository personRepository;

    @Autowired
    public PersonService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    public List<PersonDTO> findPersons() {
        List<Person> personList = personRepository.findAll();
        return personList.stream()
                .map(PersonBuilder::toPersonDTO)
                .collect(Collectors.toList());
    }

    public PersonDetailsDTO findPersonById(UUID id) {
        Optional<Person> prosumerOptional = personRepository.findById(id);
        if (prosumerOptional.isEmpty()) {
            LOGGER.error("Person with id {} was not found in db", id);
            throw new ResourceNotFoundException(Person.class.getSimpleName() + " with id: " + id);
        }
        return PersonBuilder.toPersonDetailsDTO(prosumerOptional.get());
    }

    public UUID insert(PersonDetailsDTO personDTO) {
        Person person = PersonBuilder.toEntity(personDTO);
        person = personRepository.save(person);
        LOGGER.debug("Person with id {} was inserted in db", person.getId());
        return person.getId();
    }
    @Transactional
    public PersonDetailsDTO update(PersonDetailsDTO personDTO) {
        Person person = personRepository.findById(personDTO.getId())
            .map(existingPerson -> {
                LOGGER.debug("Person with id {} will be updated in db", personDTO.getId());
                existingPerson.setName(personDTO.getName());
                existingPerson.setAddress(personDTO.getAddress());
                existingPerson.setAge(personDTO.getAge());
                return existingPerson;
            })
            .orElseGet(() -> {
                LOGGER.debug("Person with id {} will be inserted in db", personDTO.getId());
                return PersonBuilder.toEntity(personDTO);
            });

        Person savedPerson = personRepository.save(person);

        return PersonBuilder.toPersonDetailsDTO(savedPerson);
    }

    @Transactional
    public boolean delete(UUID id) {
        if (personRepository.existsById(id)) {
            personRepository.deleteById(id);
            LOGGER.debug("Person with id {} was deleted from db", id);
            return true;
        } else {
            LOGGER.debug("Person with id {} was not found in db", id);
            return false;
        }
    }
}
