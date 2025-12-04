package com.example.demo.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import java.util.UUID;
import java.io.Serializable;

@Entity
@Table(name = "user_device_mapping",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = { "user_id", "device_id" })
       })
public class UserDeviceMapping implements Serializable {

    @Id
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID mappingId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    public UserDeviceMapping(UUID userId, Device device) {
        this.userId = userId;
        this.device = device;
    }

    public UserDeviceMapping() {

    }

    public UUID getMappingId() {
        return mappingId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }
}