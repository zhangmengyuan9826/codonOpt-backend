package com.codonopt.util;

import com.codonopt.enums.SequenceType;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * 序列验证工具类
 */
public class SequenceValidator {

    // 氨基酸单字母代码正则表达式
    private static final Pattern AMINO_ACID_PATTERN = Pattern.compile("^[ACDEFGHIKLMNPQRSTVWY*\\s]+$");

    // 核苷酸序列正则表达式（ATCG）
    private static final Pattern NUCLEOTIDE_PATTERN = Pattern.compile("^[ATCGNatcgn\\s]+$");

    // FASTA格式标识符
    private static final Pattern FASTA_HEADER_PATTERN = Pattern.compile("^>.+");

    /**
     * 验证序列格式
     *
     * @param sequence 序列字符串
     * @param type     序列类型
     * @return 验证结果（true表示有效）
     */
    public static boolean isValidSequence(String sequence, SequenceType type) {
        if (StringUtils.isBlank(sequence)) {
            return false;
        }

        // 移除FASTA格式的头部行（如果有）
        String[] lines = sequence.split("\n");
        StringBuilder sequenceBuilder = new StringBuilder();

        for (String line : lines) {
            line = line.trim();
            // 跳过FASTA头部行和空行
            if (FASTA_HEADER_PATTERN.matcher(line).matches() || line.isEmpty()) {
                continue;
            }
            sequenceBuilder.append(line);
        }

        String cleanSequence = sequenceBuilder.toString().toUpperCase().replaceAll("\\s+", "");

        if (cleanSequence.isEmpty()) {
            return false;
        }

        // 根据类型验证
        switch (type) {
            case AMINO_ACID:
                return AMINO_ACID_PATTERN.matcher(cleanSequence).matches();
            case NUCLEOTIDE:
                return NUCLEOTIDE_PATTERN.matcher(cleanSequence).matches();
            default:
                return false;
        }
    }

    /**
     * 清理序列字符串（移除FASTA头部、空格、换行等）
     *
     * @param sequence 原始序列
     * @return 清理后的序列
     */
    public static String cleanSequence(String sequence) {
        if (StringUtils.isBlank(sequence)) {
            return "";
        }

        String[] lines = sequence.split("\n");
        StringBuilder sequenceBuilder = new StringBuilder();

        for (String line : lines) {
            line = line.trim();
            // 跳过FASTA头部行和空行
            if (FASTA_HEADER_PATTERN.matcher(line).matches() || line.isEmpty()) {
                continue;
            }
            sequenceBuilder.append(line);
        }

        return sequenceBuilder.toString().toUpperCase().replaceAll("\\s+", "");
    }

    /**
     * 检查序列长度是否在允许范围内
     *
     * @param sequence     序列
     * @param minLength   最小长度
     * @param maxLength   最大长度
     * @return 是否在范围内
     */
    public static boolean isLengthValid(String sequence, int minLength, int maxLength) {
        String cleanSeq = cleanSequence(sequence);
        int length = cleanSeq.length();
        return length >= minLength && length <= maxLength;
    }

    /**
     * 检查是否为FASTA格式
     *
     * @param sequence 序列
     * @return 是否为FASTA格式
     */
    public static boolean isFastaFormat(String sequence) {
        if (StringUtils.isBlank(sequence)) {
            return false;
        }
        String trimmed = sequence.trim();
        return FASTA_HEADER_PATTERN.matcher(trimmed).matches();
    }

    /**
     * 获取序列长度（清理后）
     *
     * @param sequence 序列
     * @return 序列长度
     */
    public static int getSequenceLength(String sequence) {
        return cleanSequence(sequence).length();
    }
}
