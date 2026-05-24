package com.voiceinput.pro.repository;

import com.voiceinput.pro.model.entity.AppConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppConfigRepository extends JpaRepository<AppConfigEntity, String> {
}

