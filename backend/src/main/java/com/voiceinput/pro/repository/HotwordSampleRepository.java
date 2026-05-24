package com.voiceinput.pro.repository;

import com.voiceinput.pro.model.entity.HotwordSampleEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotwordSampleRepository extends JpaRepository<HotwordSampleEntity, Long> {

    List<HotwordSampleEntity> findByHotwordIdOrderByIdAsc(String hotwordId);

    void deleteByHotwordId(String hotwordId);
}

