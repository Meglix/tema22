package com.example.auth.dtos;

import java.util.UUID;

public class PersonDTO {
    private UUID uuid;
    private String username;
    private String password;
    private boolean admin;

    public PersonDTO(UUID uuid, String username, String password, boolean admin) {
        this.uuid = uuid;
        this.username = username;
        this.password = password;
        this.admin = admin;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isAdmin() {
        return admin;
    }
}
