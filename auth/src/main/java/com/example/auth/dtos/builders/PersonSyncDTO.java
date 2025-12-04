package com.example.auth.dtos.builders;

import java.util.UUID;

public class PersonSyncDTO {
    private UUID id;
    private String name;
    private String address;
    private int age;

    public PersonSyncDTO(UUID id, String name, String address, int age) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.age = age;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public int getAge() {
        return age;
    }
}
