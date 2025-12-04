package com.example.auth.dtos;

import com.example.auth.dtos.annotation.validators.UsernameRestriction;
import jakarta.validation.constraints.NotBlank;

public class RegisterDTO {
    @NotBlank(message = "username is required")
    @UsernameRestriction()
    private String username;
    @NotBlank(message = "password is required")
    private String password;
    @NotBlank(message = "name is required")
    private String name;
    private String address;
    private int age;

    public RegisterDTO(String username, String password, String name, String address, int age) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.address = address;
        this.age = age;
    }

    public @NotBlank(message = "username is required") @UsernameRestriction() String getUsername() {
        return username;
    }

    public @NotBlank(message = "password is required") String getPassword() {
        return password;
    }

    public @NotBlank(message = "name is required") String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public int getAge() {
        return age;
    }
}
