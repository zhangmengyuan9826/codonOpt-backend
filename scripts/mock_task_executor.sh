#!/bin/bash

# Mock Codon Optimization Task Execution Script
# For testing task queue functionality
# Execution time: 5 minutes

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get task information from parameters
TASK_ID="$1"
OUTPUT_DIR="$2"
SEQUENCE_TYPE="${3:-AMINO_ACID}"
TARGET_SPECIES="${4:-HUMAN}"

# Create output directory if it doesn't exist
mkdir -p "$OUTPUT_DIR"

# Log files
LOG_FILE="$OUTPUT_DIR/execution.log"
STATUS_FILE="$OUTPUT_DIR/run.txt"
RESULT_FILE="$OUTPUT_DIR/result.txt"
ERROR_FILE="$OUTPUT_DIR/error.txt"

# Record start time
START_TIME=$(date '+%Y-%m-%d %H:%M:%S')
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Task started: $TASK_ID" >> "$LOG_FILE"
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Output directory: $OUTPUT_DIR" >> "$LOG_FILE"
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Sequence type: $SEQUENCE_TYPE" >> "$LOG_FILE"
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Target species: $TARGET_SPECIES" >> "$LOG_FILE"

# Simulate task progress (5 minutes = 300 seconds)
TOTAL_STEPS=10
STEP_DURATION=30  # 30 seconds per step

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Starting mock task execution..." >> "$LOG_FILE"

for i in $(seq 1 $TOTAL_STEPS); do
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Progress: $i/$TOTAL_STEPS ($((i * 10))%)" >> "$LOG_FILE"
    sleep $STEP_DURATION
done

# Record end time
END_TIME=$(date '+%Y-%m-%d %H:%M:%S')
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Task execution completed" >> "$LOG_FILE"

# Simulate random success or failure (90% success rate)
RANDOM_NUMBER=$((RANDOM % 100))
if [ $RANDOM_NUMBER -lt 90 ]; then
    # ===== Success scenario =====
    echo -e "${GREEN}Task execution successful${NC}"

    # Generate run.txt
    cat > "$STATUS_FILE" <<EOF
startedTime:$START_TIME
completedTime:$END_TIME
status:SUCCESS
EOF

    # Generate mock optimized sequence
    OPTIMIZED_SEQ=""
    CODONS=("ATG" "GCT" "GGA" "TCC" "AAC" "GAG" "CAG" "GTG" "CCC" "AAG" "GTT" "GGC" "TTC" "ACC" "AAA")
    SEQ_LENGTH=50
    for i in $(seq 1 $SEQ_LENGTH); do
        IDX=$((RANDOM % ${#CODONS[@]}))
        OPTIMIZED_SEQ="${OPTIMIZED_SEQ}${CODONS[$IDX]}"
    done

    # Generate random metrics
    CAI=$(awk -v min=65 -v max=95 'BEGIN{srand(); printf "%.2f", min+rand()*(max-min)}')
    GC_CONTENT=$(awk -v min=40 -v max=65 'BEGIN{srand(); printf "%.2f", min+rand()*(max-min)}')
    MFI=$(awk -v min=-500000 -v max=-200000 'BEGIN{srand(); printf "%.2f", min+rand()*(max-min)}')

    # Generate result.txt
    cat > "$RESULT_FILE" <<EOF
sequence:$OPTIMIZED_SEQ
CAI:$CAI
GCContent:$GC_CONTENT
MFI:$MFI
EOF

    # shellcheck disable=SC2129
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Successfully generated result files" >> "$LOG_FILE"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Optimized sequence length: ${#OPTIMIZED_SEQ} bp" >> "$LOG_FILE"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] CAI value: $CAI" >> "$LOG_FILE"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] GC content: $GC_CONTENT%" >> "$LOG_FILE"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] MFI value: $MFI" >> "$LOG_FILE"

    # Exit successfully
    exit 0

else
    # ===== Failure scenario =====
    echo -e "${RED}Task execution failed${NC}"

    ERROR_MSG="Analysis failed: Sequence format does not meet optimization requirements for ${TARGET_SPECIES}. Please check if input sequence contains illegal characters or if sequence length is within allowed range."

    # Generate run.txt (failure status)
    cat > "$STATUS_FILE" <<EOF
startedTime:$START_TIME
completedTime:$END_TIME
status:FAILED
info:$ERROR_MSG
EOF

    # Generate empty result.txt
    echo "" > "$RESULT_FILE"

    # Generate error.txt
    cat > "$ERROR_FILE" <<EOF
Error type: SEQUENCE_VALIDATION_ERROR
Error time: $END_TIME
Error message: $ERROR_MSG
Suggestion: Please check input sequence format and ensure it contains only valid amino acid or nucleotide characters
EOF

    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Task failed: $ERROR_MSG" >> "$LOG_FILE"

    # Exit with failure
    exit 1
fi
