package com.voiceinput.pro.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "config_template")
public class ConfigTemplateEntity extends AuditableEntity {

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String recognitionModel;

    @Column(nullable = false)
    private String languageType;

    @Column(nullable = false)
    private String domainModel;

    @Column(nullable = false)
    private String outputFormat;

    @Column(nullable = false)
    private String stabilityMode;

    @Column(nullable = false)
    private String optimizationModel;

    @Column(nullable = false)
    private String optimizationGoal;

    @Column(nullable = false)
    private String toneStyle;

    @Column(nullable = false)
    private String lengthPreference;

    @Column(nullable = false)
    private Boolean hotwordEnabled = Boolean.TRUE;

    @Column(nullable = false)
    private String costMode;

    @Column(nullable = false)
    private String asrModelRoute;

    @Column(nullable = false)
    private String llmModelRoute;

    @Column(nullable = false)
    private Boolean isDefaultTemplate = Boolean.FALSE;
}
