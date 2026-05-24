package com.voiceinput.pro.repository;

import com.voiceinput.pro.model.entity.ProofreadRevisionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProofreadRevisionRepository extends JpaRepository<ProofreadRevisionEntity, String> {
    List<ProofreadRevisionEntity> findByTaskIdOrderByCreatedAtDesc(String taskId);
}
