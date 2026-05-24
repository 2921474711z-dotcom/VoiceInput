package com.voiceinput.pro.controller;

import com.voiceinput.pro.model.dto.HotwordCategoryResponse;
import com.voiceinput.pro.model.dto.HotwordRequest;
import com.voiceinput.pro.model.dto.HotwordResponse;
import com.voiceinput.pro.model.dto.PagedHotwordResponse;
import com.voiceinput.pro.service.HotwordService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/hotwords")
@RequiredArgsConstructor
public class HotwordController {

    private final HotwordService hotwordService;

    @GetMapping("/categories")
    public List<HotwordCategoryResponse> categories() {
        return hotwordService.listCategories();
    }

    @GetMapping
    public PagedHotwordResponse list(
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return hotwordService.search(categoryId, keyword, page, size);
    }

    @PostMapping
    public HotwordResponse create(@Valid @RequestBody HotwordRequest request) {
        return hotwordService.create(request);
    }

    @PutMapping("/{id}")
    public HotwordResponse update(@PathVariable String id, @Valid @RequestBody HotwordRequest request) {
        return hotwordService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        hotwordService.delete(id);
    }

    @PostMapping("/import")
    public void importJson(@RequestParam("file") MultipartFile file) {
        hotwordService.importJson(file);
    }

    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> exportJson() {
        byte[] bytes = hotwordService.exportJson();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"hotwords.json\"")
            .contentType(MediaType.APPLICATION_JSON)
            .contentLength(bytes.length)
            .body(new ByteArrayResource(bytes));
    }
}

