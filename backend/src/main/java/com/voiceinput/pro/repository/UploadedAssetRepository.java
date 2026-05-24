package com.voiceinput.pro.repository;

import com.voiceinput.pro.model.entity.UploadedAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadedAssetRepository extends JpaRepository<UploadedAssetEntity, String> {
}

