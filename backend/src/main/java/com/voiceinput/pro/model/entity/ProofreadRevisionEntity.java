package com.voiceinput.pro.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "proofread_revision")
public class ProofreadRevisionEntity extends AuditableEntity {

    @Column(nullable = false)
    private String taskId;

    @Column(columnDefinition = "TEXT")
    private String beforeRawText;

    @Column(columnDefinition = "TEXT")
    private String beforeOptimizedText;

    @Column(columnDefinition = "TEXT")
    private String beforeMarkdownContent;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String afterRawText;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String afterOptimizedText;

    @Column(columnDefinition = "TEXT")
    private String afterMarkdownContent;
}
