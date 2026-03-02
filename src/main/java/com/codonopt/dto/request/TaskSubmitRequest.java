package com.codonopt.dto.request;

import com.codonopt.enums.SequenceType;
import com.codonopt.enums.TargetSpecies;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 任务提交请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSubmitRequest {

    @NotBlank(message = "任务名称不能为空")
    private String taskName;

    @NotBlank(message = "输入序列不能为空")
    private String inputSequence;

    @NotNull(message = "序列类型不能为空")
    private SequenceType sequenceType;

    @NotNull(message = "目标物种不能为空")
    private TargetSpecies targetSpecies;

    /**
     * 密码子频率表文件名（仅当targetSpecies为OTHERS时需要）
     */
    private String codonFrequencyFileName;

    /**
     * 可选参数，包括：
     * - gcContent: 目标GC含量
     * - caiThreshold: CAI阈值
     * - avoidRestrictionSites: 是否避免限制性酶切位点
     * - avoidHairpin: 是否避免发夹结构
     * - maxRepeatLength: 最大重复长度
     * - optimizationPriority: 优化优先级
     */
    private Map<String, Object> parameters;
}
