package com.example.demo.dtos;


import com.example.demo.dtos.validators.annotation.ConsumptionLimit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;
import java.util.UUID;

public class DeviceDetailsDTO {

    private UUID id;

    @NotBlank(message = "name is required")
    private String name;
    @NotBlank(message = "manufacturer is required")
    private String manufacturer;
    @NotNull(message = "consumption is required")
    @ConsumptionLimit(value = 18)
    private Integer consumption;

    public DeviceDetailsDTO() {
    }

    public DeviceDetailsDTO(String name, String manufacturer, int consumption) {
        this.name = name;
        this.manufacturer = manufacturer;
        this.consumption = consumption;
    }

    public DeviceDetailsDTO(UUID id, String name, String manufacturer, int consumption) {
        this.id = id;
        this.name = name;
        this.manufacturer = manufacturer;
        this.consumption = consumption;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public int getConsumption() {
        return consumption;
    }

    public void setConsumption(int consumption) {
        this.consumption = consumption;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceDetailsDTO that = (DeviceDetailsDTO) o;
        return consumption == that.consumption &&
                Objects.equals(name, that.name) &&
                Objects.equals(manufacturer, that.manufacturer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, manufacturer, consumption);
    }
}
