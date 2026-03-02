package com.codonopt.dto.response;

import com.codonopt.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 任务详情响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDetailResponse {

    private String taskId;
    private String taskName;
    private String inputSequence;
    private String sequenceType;
    private String targetSpecies;
    private Map<String, Object> parameters;
    private TaskStatus status;
    private Integer queuePosition;
    private Integer processPid;
    private String resultFilesPath;
    private Map<String, Object> resultSummary;
    private String errorMessage;
    private LocalDateTime submittedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    /**
     * 状态描述（中文）
     */
    public String getStatusDescription() {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case QUEUED:
                return "排队中";
            case RUNNING:
                return "运行中";
            case COMPLETED:
                return "已完成";
            case FAILED:
                return "失败";
            case CANCELLED:
                return "已取消";
            default:
                return "未知";
        }
    }

    /**
     * 计算运行时长（秒）
     */
    public Long getRunningDuration() {
        if (startedAt == null) {
            return 0L;
        }
        LocalDateTime end = completedAt != null ? completedAt : LocalDateTime.now();
        return java.time.Duration.between(startedAt, end).getSeconds();
    }
}
