package com.voiceinput.pro.repository;

import com.voiceinput.pro.model.entity.ModelConnectionTestEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModelConnectionTestRepository extends JpaRepository<ModelConnectionTestEntity, String> {
    List<ModelConnectionTestEntity> findTop10ByOrderByCreatedAtDesc();
}
