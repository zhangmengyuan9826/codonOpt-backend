#!/bin/bash

# 密码子优化任务模拟执行脚本
# 用于测试任务队列功能
# 执行时间：5分钟

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 从参数中获取任务信息
TASK_ID="$1"
OUTPUT_DIR="$2"
SEQUENCE_TYPE="${3:-AMINO_ACID}"
TARGET_SPECIES="${4:-HUMAN}"

# 如果输出目录不存在，创建它
mkdir -p "$OUTPUT_DIR"

# 日志文件
LOG_FILE="$OUTPUT_DIR/execution.log"
STATUS_FILE="$OUTPUT_DIR/run.txt"
RESULT_FILE="$OUTPUT_DIR/result.txt"
ERROR_FILE="$OUTPUT_DIR/error.txt"

# 记录开始时间
START_TIME=$(date'+%Y-%m-%d %H:%M:%S')
echo "[$(date'+%Y-%m-%d %H:%M:%S')] 任务开始执行: $TASK_ID" >> "$LOG_FILE"
echo "[$(date'+%Y-%m-%d %H:%M:%S')] 输出目录: $OUTPUT_DIR" >> "$LOG_FILE"
echo "[$(date'+%Y-%m-%d %H:%M:%S')] 序列类型: $SEQUENCE_TYPE" >> "$LOG_FILE"
echo "[$(date'+%Y-%m-%d %H:%M:%S')] 目标物种: $TARGET_SPECIES" >> "$LOG_FILE"

# 模拟任务进度（5分钟 = 300秒）
TOTAL_STEPS=10
STEP_DURATION=30  # 每个步骤30秒

echo "[$(date'+%Y-%m-%d %H:%M:%S')] 开始模拟任务执行 ..." >> "$LOG_FILE"

for i in $(seq 1 $TOTAL_STEPS); do
    echo "[$(date'+%Y-%m-%d %H:%M:%S')] 执行进度: $i/$TOTAL_STEPS ($((i * 10))%)" >> "$LOG_FILE"
    sleep $STEP_DURATION
done

# 记录结束时间
END_TIME=$(date'+%Y-%m-%d %H:%M:%S')
echo "[$(date'+%Y-%m-%d %H:%M:%S')] 任务执行完成" >> "$LOG_FILE"

# 模拟随机成功或失败（90%成功率）
RANDOM_NUMBER=$((RANDOM % 100))
if [ $RANDOM_NUMBER -lt 90 ]; then
    # ===== 成功场景 =====
    echo -e "${GREEN}任务执行成功${NC}"

    # 生成run.txt
    cat > "$STATUS_FILE" <<EOF
startedTime:$START_TIME
completedTime:$END_TIME
status:SUCCESS
EOF

    # 生成模拟的优化序列
    OPTIMIZED_SEQ=""
    CODONS=("ATG" "GCT" "GGA" "TCC" "AAC" "GAG" "CAG" "GTG" "CCC" "AAG" "GTT" "GGC" "TTC" "ACC" "AAA")
    SEQ_LENGTH=50
    for i in $(seq 1 $SEQ_LENGTH); do
        IDX=$((RANDOM % ${#CODONS[@]}))
        OPTIMIZED_SEQ="${OPTIMIZED_SEQ}${CODONS[$IDX]}"
    done

    # 生成随机指标
    CAI=$(awk -v min=65 -v max=95 'BEGIN{srand(); printf "%.2f", min+rand()*(max-min)}')
    GC_CONTENT=$(awk -v min=40 -v max=65 'BEGIN{srand(); printf "%.2f", min+rand()*(max-min)}')
    MFI=$(awk -v min=-500000 -v max=-200000 'BEGIN{srand(); printf "%.2f", min+rand()*(max-min)}')

    # 生成result.txt
    cat > "$RESULT_FILE" <<EOF
sequence:$OPTIMIZED_SEQ
CAI:$CAI
GCContent:$GC_CONTENT
MFI:$MFI
EOF

    echo "[$(date '+%Y-%m-%d %H:%M:%S')] 成功生成结果文件" >> "$LOG_FILE"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] 优化序列长度: ${#OPTIMIZED_SEQ} bp" >> "$LOG_FILE"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] CAI值: $CAI" >> "$LOG_FILE"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] GC含量: $GC_CONTENT%" >> "$LOG_FILE"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] MFI值: $MFI" >> "$LOG_FILE"

    # 成功退出
    exit 0

else
    # ===== 失败场景 =====
    echo -e "${RED}任务执行失败${NC}"

    ERROR_MSG="模拟分析失败：序列格式不符合${TARGET_SPECIES}的优化要求，请检查输入序列是否包含非法字符或序列长度是否在允许范围内。"

    # 生成run.txt（失败状态）
    cat > "$STATUS_FILE" <<EOF
startedTime:$START_TIME
completedTime:$END_TIME
status:FAILED
info:$ERROR_MSG
EOF

    # 生成空的result.txt
    echo "" > "$RESULT_FILE"

    # 生成error.txt
    cat > "$ERROR_FILE" <<EOF
错误类型: SEQUENCE_VALIDATION_ERROR
错误时间: $END_TIME
错误信息: $ERROR_MSG
建议: 请检查输入序列格式，确保只包含合法的氨基酸或核苷酸字符
EOF

    echo "[$(date '+%Y-%m-%d %H:%M:%S')] 任务失败: $ERROR_MSG" >> "$LOG_FILE"

    # 失败退出
    exit 1
fi
