package com.voiceinput.pro.controller;

import com.voiceinput.pro.model.dto.UploadAssetResponse;
import com.voiceinput.pro.model.entity.UploadedAssetEntity;
import com.voiceinput.pro.service.StorageService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final StorageService storageService;

    @PostMapping("/audio")
    public UploadAssetResponse uploadAudio(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "durationSeconds", required = false) BigDecimal durationSeconds
    ) {
        UploadedAssetEntity saved = storageService.uploadAudio(file, durationSeconds);
        return new UploadAssetResponse(
            saved.getId(),
            saved.getOriginalFileName(),
            saved.getSizeBytes(),
            saved.getDurationSeconds(),
            saved.getCreatedAt()
        );
    }
}

