package com.voiceinput.pro.controller;

import com.voiceinput.pro.model.dto.CreateExportRequest;
import com.voiceinput.pro.model.dto.ExportRecordResponse;
import com.voiceinput.pro.service.ExportService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exports")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @GetMapping
    public List<ExportRecordResponse> list(@RequestParam(required = false) String exportType) {
        return exportService.list(exportType);
    }

    @PostMapping
    public ExportRecordResponse create(@Valid @RequestBody CreateExportRequest request) {
        return exportService.createExport(request);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<ByteArrayResource> download(@PathVariable String id) {
        ExportService.ExportDownload download = exportService.download(id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.fileName() + "\"")
            .contentType(MediaType.parseMediaType(download.contentType()))
            .body(new ByteArrayResource(download.bytes()));
    }
}
