package com.example.demo.dtos;

import java.util.Objects;
import java.util.UUID;

public class DeviceDTO {
    private UUID id;
    private String name;
    private String manufacturer;
    private int consumption;

    public DeviceDTO() {}
    public DeviceDTO(UUID id, String name, String manufacturer, int consumption) {
        this.id = id; this.name = name; this.manufacturer = manufacturer; this.consumption = consumption;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public int getConsumption() { return consumption; }
    public void setConsumption(int consumption) { this.consumption = consumption; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceDTO that = (DeviceDTO) o;
        return consumption == that.consumption && Objects.equals(name, that.name);
    }
    @Override public int hashCode() { return Objects.hash(name, consumption); }
}
