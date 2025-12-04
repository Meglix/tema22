package com.example.auth.dtos.builders;

import com.example.auth.dtos.PersonDTO;
import com.example.auth.entities.Person;

public class PersonBuilder {

    private PersonBuilder() {
    }

    public static PersonDTO toPersonDTO(Person person) {
        return new PersonDTO(person.getId(), person.getUsername(), person.getPassword(), person.isAdmin());
    }

}
