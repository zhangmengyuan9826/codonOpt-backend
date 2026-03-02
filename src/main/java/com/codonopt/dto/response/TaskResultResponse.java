package com.codonopt.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务结果响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResultResponse {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 任务状态（SUCCESS/FAILED）
     */
    private String status;

    /**
     * 开始时间
     */
    private LocalDateTime startedTime;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 优化后的序列（成功时）
     */
    private String sequence;

    /**
     * CAI值（成功时）
     */
    private String cai;

    /**
     * GC含量（成功时）
     */
    private String gcContent;

    /**
     * MFI值（成功时）
     */
    private String mfi;

    /**
     * 错误信息（失败时）
     */
    private String errorMessage;

    /**
     * 是否可以下载
     */
    private Boolean downloadable;

    /**
     * 文件夹路径（用于下载）
     */
    private String folderPath;
}
