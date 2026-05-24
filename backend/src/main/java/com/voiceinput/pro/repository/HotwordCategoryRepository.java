package com.voiceinput.pro.repository;

import com.voiceinput.pro.model.entity.HotwordCategoryEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotwordCategoryRepository extends JpaRepository<HotwordCategoryEntity, Long> {

    List<HotwordCategoryEntity> findByEnabledTrueOrderBySortOrderAsc();

    Optional<HotwordCategoryEntity> findByCode(String code);
}

