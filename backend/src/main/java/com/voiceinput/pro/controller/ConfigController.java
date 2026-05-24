package com.voiceinput.pro.controller;

import com.voiceinput.pro.model.dto.AppConfigRequest;
import com.voiceinput.pro.model.dto.AppConfigResponse;
import com.voiceinput.pro.model.dto.ConfigTemplateRequest;
import com.voiceinput.pro.model.dto.ConfigTemplateResponse;
import com.voiceinput.pro.model.dto.ModelConnectionTestRequest;
import com.voiceinput.pro.model.dto.ModelConnectionTestResponse;
import com.voiceinput.pro.service.ConfigService;
import com.voiceinput.pro.service.ModelConnectionTestService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;
    private final ModelConnectionTestService modelConnectionTestService;

    @GetMapping
    public AppConfigResponse getActive() {
        return configService.getActive();
    }

    @PutMapping
    public AppConfigResponse update(@Valid @RequestBody AppConfigRequest request) {
        return configService.update(request);
    }

    @PostMapping("/reset")
    public AppConfigResponse reset() {
        return configService.resetToDefault();
    }

    @PostMapping("/test-connection")
    public ModelConnectionTestResponse testConnection(@Valid @RequestBody ModelConnectionTestRequest request) {
        return modelConnectionTestService.testConnection(request.target(), request.uploadId());
    }

    @GetMapping("/templates")
    public List<ConfigTemplateResponse> templates() {
        return configService.listTemplates();
    }

    @PostMapping("/templates")
    public ConfigTemplateResponse createTemplate(@Valid @RequestBody ConfigTemplateRequest request) {
        return configService.createTemplate(request);
    }

    @PutMapping("/templates/{id}")
    public ConfigTemplateResponse updateTemplate(@PathVariable String id, @Valid @RequestBody ConfigTemplateRequest request) {
        return configService.updateTemplate(id, request);
    }

    @PostMapping("/templates/{id}/apply")
    public AppConfigResponse applyTemplate(@PathVariable String id) {
        return configService.applyTemplate(id);
    }

    @PostMapping("/templates/{id}/default")
    public void setDefaultTemplate(@PathVariable String id) {
        configService.setDefaultTemplate(id);
    }

    @DeleteMapping("/templates/{id}")
    public void deleteTemplate(@PathVariable String id) {
        configService.deleteTemplate(id);
    }
}
