package com.voiceinput.pro.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "model_connection_test")
public class ModelConnectionTestEntity extends AuditableEntity {

    @Column(nullable = false, length = 16)
    private String target;

    private String provider;

    private String baseUrl;

    private String modelName;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String message;

    private Long durationMs;

    private String uploadId;
}
