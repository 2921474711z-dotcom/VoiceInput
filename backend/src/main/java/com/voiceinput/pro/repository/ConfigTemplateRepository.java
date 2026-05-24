package com.voiceinput.pro.repository;

import com.voiceinput.pro.model.entity.ConfigTemplateEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigTemplateRepository extends JpaRepository<ConfigTemplateEntity, String> {

    List<ConfigTemplateEntity> findAllByOrderByCreatedAtDesc();

    ConfigTemplateEntity findFirstByIsDefaultTemplateTrue();
}
