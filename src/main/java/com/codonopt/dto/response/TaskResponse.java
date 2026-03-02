package com.codonopt.dto.response;

import com.codonopt.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务列表项响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    private String taskId;
    private String userName;
    private String taskName;
    private String sequenceType;
    private String targetSpecies;
    private TaskStatus status;
    private Integer queuePosition;
    private LocalDateTime submittedAt;
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
}
