package com.voiceinput.pro.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "export_record")
public class ExportRecordEntity extends AuditableEntity {

    @Column(nullable = false)
    private String taskId;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String objectKey;

    @Column(nullable = false)
    private String exportType;
}
