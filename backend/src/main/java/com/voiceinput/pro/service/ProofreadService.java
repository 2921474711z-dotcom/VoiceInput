package com.voiceinput.pro.service;

import com.voiceinput.pro.model.dto.ProofreadTaskRequest;
import com.voiceinput.pro.model.dto.ProofreadTaskResponse;
import com.voiceinput.pro.model.entity.ProcessingTaskEntity;
import com.voiceinput.pro.model.entity.ProofreadRevisionEntity;
import com.voiceinput.pro.repository.ProcessingTaskRepository;
import com.voiceinput.pro.repository.ProofreadRevisionRepository;
import com.voiceinput.pro.support.ApiException;
import com.voiceinput.pro.support.TextSupport;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProofreadService {

    private final ProcessingTaskRepository processingTaskRepository;
    private final ProofreadRevisionRepository proofreadRevisionRepository;

    @Transactional
    public ProofreadTaskResponse saveProofread(String taskId, ProofreadTaskRequest request) {
        ProcessingTaskEntity task = processingTaskRepository.findByIdAndDeletedFalse(taskId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "未找到需要校对的任务"));

        String rawText = TextSupport.normalizeWhitespace(request.rawText());
        String optimizedText = TextSupport.normalizeWhitespace(request.optimizedText());
        String markdown = request.markdownContent() == null || request.markdownContent().isBlank()
            ? "# " + TextSupport.buildTitle(optimizedText) + "\n\n" + optimizedText
            : request.markdownContent();

        ProofreadRevisionEntity revision = new ProofreadRevisionEntity();
        revision.setTaskId(task.getId());
        revision.setBeforeRawText(task.getProofreadRawText() == null ? task.getRawText() : task.getProofreadRawText());
        revision.setBeforeOptimizedText(task.getProofreadOptimizedText() == null ? task.getOptimizedText() : task.getProofreadOptimizedText());
        revision.setBeforeMarkdownContent(task.getProofreadMarkdownContent() == null ? task.getMarkdownContent() : task.getProofreadMarkdownContent());
        revision.setAfterRawText(rawText);
        revision.setAfterOptimizedText(optimizedText);
        revision.setAfterMarkdownContent(markdown);
        ProofreadRevisionEntity savedRevision = proofreadRevisionRepository.save(revision);

        LocalDateTime proofreadAt = LocalDateTime.now();
        task.setProofreadRawText(rawText);
        task.setProofreadOptimizedText(optimizedText);
        task.setProofreadMarkdownContent(markdown);
        task.setProofreadRevisionId(savedRevision.getId());
        task.setProofreadAt(proofreadAt);
        processingTaskRepository.save(task);

        return new ProofreadTaskResponse(
            task.getId(),
            savedRevision.getId(),
            rawText,
            optimizedText,
            markdown,
            proofreadAt
        );
    }
}
