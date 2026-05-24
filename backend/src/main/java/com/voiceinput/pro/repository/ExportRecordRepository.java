package com.voiceinput.pro.repository;

import com.voiceinput.pro.model.entity.ExportRecordEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExportRecordRepository extends JpaRepository<ExportRecordEntity, String> {
    List<ExportRecordEntity> findAllByOrderByCreatedAtDesc();

    List<ExportRecordEntity> findByExportTypeOrderByCreatedAtDesc(String exportType);
}
