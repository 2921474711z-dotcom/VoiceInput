package com.voiceinput.pro.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.voiceinput.pro.model.dto.HotwordCategoryResponse;
import com.voiceinput.pro.model.dto.HotwordRequest;
import com.voiceinput.pro.model.dto.HotwordResponse;
import com.voiceinput.pro.model.dto.HotwordSampleResponse;
import com.voiceinput.pro.model.dto.PagedHotwordResponse;
import com.voiceinput.pro.model.entity.HotwordCategoryEntity;
import com.voiceinput.pro.model.entity.HotwordEntity;
import com.voiceinput.pro.model.entity.HotwordSampleEntity;
import com.voiceinput.pro.model.enums.SceneType;
import com.voiceinput.pro.repository.HotwordCategoryRepository;
import com.voiceinput.pro.repository.HotwordRepository;
import com.voiceinput.pro.repository.HotwordSampleRepository;
import com.voiceinput.pro.support.ApiException;
import com.voiceinput.pro.support.JsonSupport;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class HotwordService {

    private final HotwordCategoryRepository hotwordCategoryRepository;
    private final HotwordRepository hotwordRepository;
    private final HotwordSampleRepository hotwordSampleRepository;
    private final JsonSupport jsonSupport;

    @Transactional(readOnly = true)
    public List<HotwordCategoryResponse> listCategories() {
        Map<Long, Long> counts = hotwordRepository.findAllWithCategory().stream()
            .collect(java.util.stream.Collectors.groupingBy(item -> item.getCategory().getId(), java.util.stream.Collectors.counting()));

        return hotwordCategoryRepository.findAll().stream()
            .sorted(java.util.Comparator.comparing(HotwordCategoryEntity::getSortOrder))
            .map(category -> new HotwordCategoryResponse(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.getIcon(),
                category.getSortOrder(),
                category.getEnabled(),
                counts.getOrDefault(category.getId(), 0L)
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public PagedHotwordResponse search(Long categoryId, String keyword, int page, int size) {
        var result = hotwordRepository.search(categoryId, blankToNull(keyword), PageRequest.of(page, size));
        return new PagedHotwordResponse(
            result.getContent().stream().map(this::toResponse).toList(),
            result.getTotalElements(),
            page,
            size
        );
    }

    @Transactional
    public HotwordResponse create(HotwordRequest request) {
        HotwordEntity entity = new HotwordEntity();
        bind(request, entity);
        HotwordEntity saved = hotwordRepository.save(entity);
        replaceSamples(saved, request);
        return toResponse(saved);
    }

    @Transactional
    public HotwordResponse update(String id, HotwordRequest request) {
        HotwordEntity entity = hotwordRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "未找到热词"));
        bind(request, entity);
        HotwordEntity saved = hotwordRepository.save(entity);
        replaceSamples(saved, request);
        return toResponse(saved);
    }

    @Transactional
    public void delete(String id) {
        hotwordSampleRepository.deleteByHotwordId(id);
        hotwordRepository.deleteById(id);
    }

    @Transactional
    public void importJson(MultipartFile file) {
        try {
            List<HotwordImportItem> items = jsonSupport.fromJson(
                new String(file.getBytes(), StandardCharsets.UTF_8),
                new TypeReference<List<HotwordImportItem>>() {
                }
            );
            for (HotwordImportItem item : items) {
                HotwordCategoryEntity category = hotwordCategoryRepository.findById(item.categoryId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "导入分类不存在"));
                HotwordEntity entity = new HotwordEntity();
                entity.setRecognizedTerm(item.recognizedTerm());
                entity.setStandardTerm(item.standardTerm());
                entity.setCategory(category);
                entity.setSceneCodes(String.join(",", item.scenes()));
                entity.setEnabled(item.enabled());
                HotwordEntity saved = hotwordRepository.save(entity);
                if (item.samples() != null) {
                    for (String sampleBefore : item.samples()) {
                        HotwordSampleEntity sample = new HotwordSampleEntity();
                        sample.setHotword(saved);
                        sample.setSampleBefore(sampleBefore);
                        sample.setSampleAfter(applySingleReplacement(sampleBefore, saved.getRecognizedTerm(), saved.getStandardTerm()));
                        hotwordSampleRepository.save(sample);
                    }
                }
            }
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "导入热词失败：" + ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportJson() {
        List<Map<String, Object>> payload = hotwordRepository.findAllWithCategory().stream()
            .map(item -> Map.of(
                "id", item.getId(),
                "recognizedTerm", item.getRecognizedTerm(),
                "standardTerm", item.getStandardTerm(),
                "categoryId", item.getCategory().getId(),
                "categoryName", item.getCategory().getName(),
                "scenes", Arrays.asList(item.getSceneCodes().split(",")),
                "enabled", item.getEnabled(),
                "samples", hotwordSampleRepository.findByHotwordIdOrderByIdAsc(item.getId()).stream()
                    .map(HotwordSampleEntity::getSampleBefore)
                    .toList()
            ))
            .toList();
        return jsonSupport.toJson(payload).getBytes(StandardCharsets.UTF_8);
    }

    public HotwordApplyResult applyHotwords(String text, SceneType sceneType) {
        String output = text == null ? "" : text;
        List<Match> matches = new ArrayList<>();
        for (HotwordEntity hotword : hotwordRepository.findAllEnabled()) {
            boolean applicable = Arrays.stream(hotword.getSceneCodes().split(","))
                .anyMatch(scene -> scene.equals(sceneType.name()));
            if (!applicable) {
                continue;
            }
            Pattern pattern = Pattern.compile(Pattern.quote(hotword.getRecognizedTerm()), Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(output);
            int hit = 0;
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                hit++;
                matcher.appendReplacement(sb, Matcher.quoteReplacement(hotword.getStandardTerm()));
            }
            matcher.appendTail(sb);
            output = sb.toString();
            if (hit > 0) {
                matches.add(new Match(hotword.getRecognizedTerm(), hotword.getStandardTerm(), hit));
            }
        }
        return new HotwordApplyResult(output, matches);
    }

    @Transactional(readOnly = true)
    public List<HotwordResponse> listEnabled() {
        return hotwordRepository.findAllEnabled().stream().map(this::toResponse).toList();
    }

    private void bind(HotwordRequest request, HotwordEntity entity) {
        HotwordCategoryEntity category = hotwordCategoryRepository.findById(request.categoryId())
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "热词分类不存在"));
        entity.setRecognizedTerm(request.recognizedTerm().trim());
        entity.setStandardTerm(request.standardTerm().trim());
        entity.setCategory(category);
        entity.setSceneCodes(String.join(",", request.scenes()));
        entity.setEnabled(request.enabled());
    }

    private void replaceSamples(HotwordEntity hotword, HotwordRequest request) {
        hotwordSampleRepository.deleteByHotwordId(hotword.getId());
        if (request.samples() == null) {
            return;
        }
        for (var sampleRequest : request.samples()) {
            HotwordSampleEntity sampleEntity = new HotwordSampleEntity();
            sampleEntity.setHotword(hotword);
            sampleEntity.setSampleBefore(sampleRequest.sampleBefore());
            sampleEntity.setSampleAfter(applySingleReplacement(
                sampleRequest.sampleBefore(),
                hotword.getRecognizedTerm(),
                hotword.getStandardTerm()
            ));
            hotwordSampleRepository.save(sampleEntity);
        }
    }

    private String applySingleReplacement(String text, String from, String to) {
        return text.replaceAll("(?i)" + Pattern.quote(from), Matcher.quoteReplacement(to));
    }

    private HotwordResponse toResponse(HotwordEntity entity) {
        List<HotwordSampleResponse> samples = hotwordSampleRepository.findByHotwordIdOrderByIdAsc(entity.getId()).stream()
            .map(sample -> new HotwordSampleResponse(sample.getId(), sample.getSampleBefore(), sample.getSampleAfter()))
            .toList();
        return new HotwordResponse(
            entity.getId(),
            entity.getRecognizedTerm(),
            entity.getStandardTerm(),
            entity.getCategory().getId(),
            entity.getCategory().getName(),
            Arrays.asList(entity.getSceneCodes().split(",")),
            entity.getEnabled(),
            samples,
            entity.getCreatedAt()
        );
    }

    private String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    public record Match(String recognizedTerm, String standardTerm, int hitCount) {
    }

    public record HotwordApplyResult(String text, List<Match> matches) {
    }

    private record HotwordImportItem(
        String recognizedTerm,
        String standardTerm,
        Long categoryId,
        List<String> scenes,
        Boolean enabled,
        List<String> samples
    ) {
    }
}
