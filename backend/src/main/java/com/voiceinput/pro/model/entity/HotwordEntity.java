package com.voiceinput.pro.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "hotword")
public class HotwordEntity extends AuditableEntity {

    @Column(nullable = false)
    private String recognizedTerm;

    @Column(nullable = false)
    private String standardTerm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private HotwordCategoryEntity category;

    @Column(nullable = false)
    private String sceneCodes;

    @Column(nullable = false)
    private Boolean enabled = Boolean.TRUE;
}
