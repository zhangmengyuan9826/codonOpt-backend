package com.codonopt.entity;

import com.codonopt.enums.SequenceType;
import com.codonopt.enums.TargetSpecies;
import com.codonopt.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 任务实体类
 */
@Entity
@Table(name = "tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    @Id
    @Column(name = "task_id", length = 36)
    private String taskId;

    @NotNull(message = "用户ID不能为空")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotBlank(message = "任务名称不能为空")
    @Column(name = "task_name", nullable = false, length = 200)
    private String taskName;

    @Lob
    @NotBlank(message = "输入序列不能为空")
    @Column(name = "input_sequence", nullable = false, columnDefinition = "TEXT")
    private String inputSequence;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "序列类型不能为空")
    @Column(name = "sequence_type", nullable = false, length = 20)
    private SequenceType sequenceType;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "目标物种不能为空")
    @Column(name = "target_species", nullable = false, length = 50)
    private TargetSpecies targetSpecies;

    @Column(columnDefinition = "JSON")
    private String parameters;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TaskStatus status = TaskStatus.QUEUED;

    @Column(name = "queue_position")
    @Builder.Default
    private Integer queuePosition = 0;

    @Column(name = "process_pid")
    private Integer processPid;

    @Column(name = "result_files_path", length = 500)
    private String resultFilesPath;

    @Column(name = "codon_frequency_file_path", length = 500)
    private String codonFrequencyFilePath;

    @Column(name = "result_summary", columnDefinition = "JSON")
    private String resultSummary;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
        if (taskId == null || taskId.isEmpty()) {
            taskId = java.util.UUID.randomUUID().toString();
        }
        if (status == null) {
            status = TaskStatus.QUEUED;
        }
        if (queuePosition == null) {
            queuePosition = 0;
        }
    }
}
