package com.codonopt.enums;

/**
 * 目标物种枚举
 */
public enum TargetSpecies {
    /**
     * 人类
     */
    HUMAN("人类", "Homo sapiens"),

    /**
     * 小鼠
     */
    MOUSE("小鼠", "Mus musculus"),

    /**
     * 大肠杆菌
     */
    E_COLI("大肠杆菌", "Escherichia coli"),

    /**
     * 酵母
     */
    YEAST("酵母", "Saccharomyces cerevisiae"),

    /**
     * 昆虫细胞
     */
    INSECT("昆虫细胞", "Spodoptera frugiperda"),

    /**
     * 其他（自定义物种）
     */
    OTHERS("其他", "Custom species with user-provided codon frequency table");

    private final String displayName;
    private final String scientificName;

    TargetSpecies(String displayName, String scientificName) {
        this.displayName = displayName;
        this.scientificName = scientificName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getScientificName() {
        return scientificName;
    }
}
