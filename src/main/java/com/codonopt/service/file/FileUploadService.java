package com.codonopt.service.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * 文件上传服务
 */
@Slf4j
@Service
public class FileUploadService {

    @Value("${app.codon-frequency.upload-directory}")
    private String uploadDirectory;

    @Value("${app.codon-frequency.max-file-size}")
    private long maxFileSize;

    /**
     * 保存密码子频率表文件
     *
     * @param file 上传的文件
     * @return 保存的文件名
     * @throws Exception 保存失败
     */
    public String saveCodonFrequencyFile(MultipartFile file) throws Exception {
        // 确保上传目录存在
        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 生成唯一文件名：原始文件名 + 时间戳
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String baseName = getBaseName(originalFilename);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String savedFileName = baseName + "_" + timestamp + "." + fileExtension;

        // 保存文件
        Path targetPath = uploadPath.resolve(savedFileName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        log.info("Saved codon frequency file: {} as {}", originalFilename, savedFileName);

        return savedFileName;
    }

    /**
     * 验证密码子频率表文件格式
     *
     * @param file 上传的文件
     * @return 验证结果
     * @throws Exception 验证失败
     */
    public Map<String, Object> validateCodonFrequencyFile(MultipartFile file) throws Exception {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int validCodonCount = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            int lineNumber = 0;
            Set<String> seenCodons = new HashSet<>();

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // 跳过空行
                if (line.trim().isEmpty()) {
                    continue;
                }

                // 解析行
                String[] parts = line.trim().split("\\s+");
                if (parts.length < 4) {
                    errors.add("第 " + lineNumber + " 行：格式不正确，至少需要4列（氨基酸、密码子、频率、比例）");
                    continue;
                }

                String aminoAcid = parts[0];
                String codon = parts[1];
                String frequencyStr = parts[2];
                String fractionStr = parts[3];

                // 验证密码子（3个核苷酸）
                if (!codon.matches("[ATCGU]{3}")) {
                    errors.add("第 " + lineNumber + " 行：密码子 '" + codon + "' 格式不正确，应为3个ATCG/U字母");
                    continue;
                }

                // 检查重复密码子
                if (seenCodons.contains(codon)) {
                    warnings.add("第 " + lineNumber + " 行：密码子 '" + codon + "' 重复出现");
                    continue;
                }
                seenCodons.add(codon);

                // 验证频率数值
                try {
                    double frequency = Double.parseDouble(frequencyStr.replace(",", ""));
                    double fraction = Double.parseDouble(fractionStr);

                    if (frequency < 0) {
                        errors.add("第 " + lineNumber + " 行：频率不能为负数");
                    }

                    if (fraction < 0 || fraction > 1) {
                        errors.add("第 " + lineNumber + " 行：比例必须在0-1之间");
                    }
                } catch (NumberFormatException e) {
                    errors.add("第 " + lineNumber + " 行：频率或比例数值格式不正确");
                }

                validCodonCount++;
            }
        }

        // 检查密码子数量（应该至少有61个密码子，不包括终止密码子）
        if (validCodonCount < 61) {
            warnings.add("密码子数量较少（" + validCodonCount + "），标准表应有61-64个密码子");
        }

        result.put("valid", errors.isEmpty());
        result.put("codonCount", validCodonCount);
        result.put("errors", errors);
        result.put("warnings", warnings);

        return result;
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
        return filename.substring(lastDotIndex + 1);
    }

    /**
     * 获取文件基础名（不含扩展名）
     */
    private String getBaseName(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return filename;
        }
        return filename.substring(0, lastDotIndex);
    }

    /**
     * 删除文件
     *
     * @param fileName 文件名
     * @return 是否成功
     */
    public boolean deleteFile(String fileName) {
        try {
            Path filePath = Paths.get(uploadDirectory, fileName);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Deleted file: {}", fileName);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to delete file: {}", fileName, e);
            return false;
        }
    }

    /**
     * 将上传的文件复制到任务结果目录
     *
     * @param fileName 上传的文件名
     * @param targetDir 目标目录
     * @return 新的文件路径
     * @throws Exception 复制失败
     */
    public String copyFileToTaskDirectory(String fileName, String targetDir) throws Exception {
        Path sourcePath = Paths.get(uploadDirectory, fileName);
        Path targetPath = Paths.get(targetDir, "codon_frequency.txt");

        if (!Files.exists(sourcePath)) {
            throw new Exception("源文件不存在: " + fileName);
        }

        // 确保目标目录存在
        Path targetDirPath = Paths.get(targetDir);
        if (!Files.exists(targetDirPath)) {
            Files.createDirectories(targetDirPath);
        }

        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

        log.info("Copied codon frequency file from {} to {}", sourcePath, targetPath);

        return targetPath.toString();
    }
}
