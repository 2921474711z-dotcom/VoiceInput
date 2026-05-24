package com.voiceinput.pro.service;

import com.voiceinput.pro.config.AppProperties;
import com.voiceinput.pro.model.entity.ExportRecordEntity;
import com.voiceinput.pro.model.entity.UploadedAssetEntity;
import com.voiceinput.pro.repository.ExportRecordRepository;
import com.voiceinput.pro.repository.UploadedAssetRepository;
import com.voiceinput.pro.support.ApiException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final MinioClient minioClient;
    private final AppProperties appProperties;
    private final UploadedAssetRepository uploadedAssetRepository;
    private final ExportRecordRepository exportRecordRepository;

    @PostConstruct
    public void ensureBucket() {
        try {
            String bucket = appProperties.getStorage().getBucket();
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception ex) {
            throw new IllegalStateException("初始化 MinIO Bucket 失败", ex);
        }
    }

    @Transactional
    public UploadedAssetEntity uploadAudio(MultipartFile file, BigDecimal durationSeconds) {
        try {
            String objectKey = "uploads/%s/%s-%s".formatted(
                LocalDate.now(),
                UUID.randomUUID(),
                file.getOriginalFilename()
            );

            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(appProperties.getStorage().getBucket())
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );

            UploadedAssetEntity entity = new UploadedAssetEntity();
            entity.setOriginalFileName(file.getOriginalFilename());
            entity.setObjectKey(objectKey);
            entity.setContentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
            entity.setSizeBytes(file.getSize());
            entity.setDurationSeconds(durationSeconds);
            return uploadedAssetRepository.save(entity);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "上传音频失败：" + ex.getMessage());
        }
    }

    public UploadedAssetEntity getAsset(String id) {
        return uploadedAssetRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "未找到上传文件"));
    }

    public InputStream getObjectStream(String objectKey) {
        try {
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(appProperties.getStorage().getBucket())
                    .object(objectKey)
                    .build()
            );
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "读取对象存储文件失败");
        }
    }

    @Transactional
    public ExportRecordEntity saveMarkdownExport(String taskId, String fileName, String markdown) {
        try {
            byte[] bytes = markdown.getBytes(StandardCharsets.UTF_8);
            String objectKey = "exports/%s/%s".formatted(LocalDate.now(), fileName);
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(appProperties.getStorage().getBucket())
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .contentType("text/markdown; charset=utf-8")
                    .build()
            );

            ExportRecordEntity record = new ExportRecordEntity();
            record.setTaskId(taskId);
            record.setFileName(fileName);
            record.setObjectKey(objectKey);
            record.setExportType("markdown");
            return exportRecordRepository.save(record);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "导出 Markdown 失败");
        }
    }

    @Transactional
    public ExportRecordEntity saveExport(
        String taskId,
        String taskTitle,
        String fileName,
        byte[] bytes,
        String exportType,
        String contentSource,
        String contentType,
        String objectPrefix
    ) {
        try {
            String objectKey = "%s/%s/%s".formatted(objectPrefix, LocalDate.now(), fileName);
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(appProperties.getStorage().getBucket())
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .contentType(contentType)
                    .build()
            );

            ExportRecordEntity record = new ExportRecordEntity();
            record.setTaskId(taskId);
            record.setTaskTitle(taskTitle);
            record.setFileName(fileName);
            record.setObjectKey(objectKey);
            record.setExportType(exportType);
            record.setContentSource(contentSource);
            record.setContentType(contentType);
            record.setSizeBytes((long) bytes.length);
            record.setStatus("SUCCESS");
            return exportRecordRepository.save(record);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "导出文件写入对象存储失败：" + ex.getMessage());
        }
    }

    public byte[] readExportBytes(ExportRecordEntity record) {
        try (InputStream inputStream = getObjectStream(record.getObjectKey());
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            inputStream.transferTo(outputStream);
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "读取导出文件失败：" + ex.getMessage());
        }
    }
}
