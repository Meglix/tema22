package com.example.demo.controllers;

import com.example.demo.dtos.PersonDTO;
import com.example.demo.dtos.PersonDetailsDTO;
import com.example.demo.entities.Person;
import com.example.demo.services.JwtService;
import com.example.demo.services.PersonService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/people")
@Validated
public class PersonController {

    private final PersonService personService;
    private final JwtService jwtService;

    public PersonController(PersonService personService, JwtService jwtService) {
        this.personService = personService;
        this.jwtService = jwtService;
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

    private void checkUser(String authHeader, UUID uuid) {
        try {
            String id = jwtService.getIdFromToken(authHeader);
            if (id == null || !id.equals(uuid.toString())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Access Denied: You cannot fetch other clients' data");
            }
        } catch (ParseException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Token");
        }

    }

    @GetMapping
    public ResponseEntity<List<PersonDTO>> getPeople(@RequestHeader("Authorization") String authHeader) {
        checkAdminRole(authHeader);
        try {
            String username = jwtService.getUsernameFromToken(authHeader);
        } catch (ParseException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(personService.findPersons());
    }

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody PersonDetailsDTO person) {
        UUID id = personService.insert(person);

        // Publish user create event for synchronization
        rabbitTemplate.convertAndSend("user-exchange", "user.create", id.toString());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
        return ResponseEntity.created(location).build(); // 201 + Location header
    }

    @GetMapping("/{id}")
    public ResponseEntity<PersonDetailsDTO> getPerson(@PathVariable UUID id,
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (!jwtService.getRoleFromToken(authHeader).equals("ROLE_ADMIN"))
                checkUser(authHeader, id);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok(personService.findPersonById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePerson(@PathVariable UUID id) {
        boolean deleted = personService.delete(id);
        if (deleted) {
            // Publish user delete event for synchronization
            rabbitTemplate.convertAndSend("user-exchange", "user.delete", id.toString());
            return ResponseEntity.status(204).build();
        } else {
            return ResponseEntity.status(404).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<PersonDetailsDTO> updatePerson(@Valid @RequestBody PersonDetailsDTO personDetailsDTO,
            @PathVariable UUID id, @RequestHeader("Authorization") String authHeader) {
        checkAdminRole(authHeader);
        PersonDetailsDTO personDTO = personService.update(personDetailsDTO);
        if (id.equals(personDTO.getId())) {
            return ResponseEntity.status(204).body(personDTO);
        } else {
            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(personDTO.getId())
                    .toUri();
            return ResponseEntity.created(location).build();
        }
    }

}
