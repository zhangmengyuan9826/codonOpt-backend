package com.codonopt.util;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 文件操作工具类
 */
public class FileUtil {

    /**
     * 创建ZIP压缩文件
     *
     * @param sourceDirPath 源目录路径
     * @param zipFilePath   ZIP文件路径
     * @throws IOException IO异常
     */
    public static void createZip(String sourceDirPath, String zipFilePath) throws IOException {
        Path sourcePath = Paths.get(sourceDirPath);
        if (!Files.exists(sourcePath)) {
            throw new IOException("源目录不存在: " + sourceDirPath);
        }

        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFilePath))) {
            Files.walk(sourcePath)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(sourcePath.relativize(path).toString());
                        try {
                            zipOut.putNextEntry(zipEntry);
                            Files.copy(path, zipOut);
                            zipOut.closeEntry();
                        } catch (IOException e) {
                            throw new RuntimeException("添加文件到ZIP失败: " + path, e);
                        }
                    });
        }
    }

    /**
     * 读取文件内容
     *
     * @param filePath 文件路径
     * @return 文件内容
     * @throws IOException IO异常
     */
    public static String readFile(String filePath) throws IOException {
        return IOUtils.toString(new File(filePath).toURI(), "UTF-8");
    }

    /**
     * 写入文件内容
     *
     * @param filePath 文件路径
     * @param content  文件内容
     * @throws IOException IO异常
     */
    public static void writeFile(String filePath, String content) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(content);
        }
    }

    /**
     * 创建目录（如果不存在）
     *
     * @param dirPath 目录路径
     * @throws IOException IO异常
     */
    public static void createDirectoryIfNotExists(String dirPath) throws IOException {
        Path path = Paths.get(dirPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    /**
     * 删除目录及其内容
     *
     * @param dirPath 目录路径
     * @throws IOException IO异常
     */
    public static void deleteDirectory(String dirPath) throws IOException {
        Path path = Paths.get(dirPath);
        if (Files.exists(path)) {
            Files.walk(path)
                    .sorted((a, b) -> b.compareTo(a)) // 反向排序，先删除文件再删除目录
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            throw new RuntimeException("删除文件失败: " + p, e);
                        }
                    });
        }
    }

    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 扩展名（不含点号）
     */
    public static String getFileExtension(String filename) {
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
     * 格式化文件大小
     *
     * @param bytes 字节数
     * @return 格式化后的字符串（如：1.5 MB）
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 检查文件是否存在
     *
     * @param filePath 文件路径
     * @return 是否存在
     */
    public static boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }
}
