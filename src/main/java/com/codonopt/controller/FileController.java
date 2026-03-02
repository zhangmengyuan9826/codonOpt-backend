package com.codonopt.controller;

import com.codonopt.dto.response.ApiResponse;
import com.codonopt.service.file.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "文件管理", description = "文件上传、下载等接口")
public class FileController {

    private final FileUploadService fileUploadService;

    @Value("${app.codon-frequency.demo-file-path}")
    private String demoFilePath;

    @Value("${app.upload.max-file-size}")
    private long maxFileSize;

    /**
     * 上传密码子频率表文件
     */
    @PostMapping("/codon-frequency")
    @Operation(summary = "上传密码子频率表", description = "上传自定义物种的密码子频率表文件")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadCodonFrequencyFile(
            @RequestParam("file") MultipartFile file) {

        // 验证文件
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("文件不能为空"));
        }

        // 验证文件大小
        if (file.getSize() > maxFileSize) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("文件大小超过限制（最大" + (maxFileSize / 1024) + "KB）"));
        }

        // 验证文件名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("文件名不能为空"));
        }

        // 验证文件扩展名
        String fileExtension = getFileExtension(originalFilename);
        if (!isValidFileExtension(fileExtension)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("只支持 .txt 和 .csv 格式的文件"));
        }

        try {
            // 生成唯一文件名
            String savedFileName = fileUploadService.saveCodonFrequencyFile(file);

            Map<String, String> result = new HashMap<>();
            result.put("fileName", savedFileName);
            result.put("originalFileName", originalFilename);
            result.put("fileSize", String.valueOf(file.getSize()));

            return ResponseEntity.ok(ApiResponse.success("文件上传成功", result));

        } catch (Exception e) {
            log.error("Failed to upload codon frequency file", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("文件上传失败: " + e.getMessage()));
        }
    }

    /**
     * 下载密码子频率表格式参考文件
     */
    @GetMapping("/codon-frequency/demo")
    @Operation(summary = "下载参考文件", description = "下载密码子频率表格式参考文件")
    public ResponseEntity<Resource> downloadDemoFile() {
        File demoFile = new File(demoFilePath);

        if (!demoFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(demoFile);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"species_codon_frequency_demo.txt\"")
                .contentType(MediaType.TEXT_PLAIN)
                .contentLength(demoFile.length())
                .body(resource);
    }

    /**
     * 验证密码子频率表文件内容
     */
    @PostMapping("/codon-frequency/validate")
    @Operation(summary = "验证文件格式", description = "验证上传的密码子频率表文件格式是否正确")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateCodonFrequencyFile(
            @RequestParam("file") MultipartFile file) {

        try {
            Map<String, Object> validationResult = fileUploadService.validateCodonFrequencyFile(file);

            boolean isValid = (Boolean) validationResult.get("valid");

            if (isValid) {
                return ResponseEntity.ok(ApiResponse.success("文件格式验证通过", validationResult));
            } else {
                return ResponseEntity.ok(ApiResponse.success("文件格式存在错误", validationResult));
            }

        } catch (Exception e) {
            log.error("Failed to validate codon frequency file", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("文件验证失败: " + e.getMessage()));
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * 验证文件扩展名是否有效
     */
    private boolean isValidFileExtension(String extension) {
        return "txt".equals(extension) || "csv".equals(extension);
    }
}
