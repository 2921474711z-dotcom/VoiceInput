package com.voiceinput.pro.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "uploaded_asset")
public class UploadedAssetEntity extends AuditableEntity {

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String objectKey;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long sizeBytes;

    @Column(precision = 10, scale = 2)
    private BigDecimal durationSeconds;
}
