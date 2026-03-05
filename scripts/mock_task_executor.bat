@echo off
REM Mock Codon Optimization Task Execution Script (Windows)
REM For testing task queue functionality
REM Execution time: 5 minutes

setlocal enabledelayedexpansion

REM Get task information from parameters
set TASK_ID=%1
set OUTPUT_DIR=%2
set SEQUENCE_TYPE=%3
if "%SEQUENCE_TYPE%"=="" set SEQUENCE_TYPE=AMINO_ACID
set TARGET_SPECIES=%4
if "%TARGET_SPECIES%"=="" set TARGET_SPECIES=HUMAN

REM Create output directory if it doesn't exist
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

REM Log files
set LOG_FILE=%OUTPUT_DIR%\execution.log
set STATUS_FILE=%OUTPUT_DIR%\run.txt
set RESULT_FILE=%OUTPUT_DIR%\result.txt
set ERROR_FILE=%OUTPUT_DIR%\error.txt

REM Record start time
echo [%date% %time%] Task started: %TASK_ID% >> "%LOG_FILE%"
echo [%date% %time%] Output directory: %OUTPUT_DIR% >> "%LOG_FILE%"
echo [%date% %time%] Sequence type: %SEQUENCE_TYPE% >> "%LOG_FILE%"
echo [%date% %time%] Target species: %TARGET_SPECIES% >> "%LOG_FILE%"

REM Simulate task progress (5 minutes = 300 seconds)
set TOTAL_STEPS=10
set STEP_DURATION=30

echo [%date% %time%] Starting mock task execution... >> "%LOG_FILE%"

for /L %%i in (1,1,%TOTAL_STEPS%) do (
    echo [%date% %time%] Progress: %%i/%TOTAL_STEPS% >> "%LOG_FILE%"
    REM Use ping command for delay (more reliable)
    ping 127.0.0.1 -n %STEP_DURATION% > nul
)

REM Record end time
echo [%date% %time%] Task execution completed >> "%LOG_FILE%"

REM Simulate random success or failure (90% success rate)
set /a RANDOM_NUMBER=%RANDOM% %% 100
if %RANDOM_NUMBER% lss 90 (
    REM ===== Success scenario =====
    echo Task execution successful

    REM Generate run.txt
    echo startedTime:%date% %time% > "%STATUS_FILE%"
    echo completedTime:%date% %time% >> "%STATUS_FILE%"
    echo status:SUCCESS >> "%STATUS_FILE%"

    REM Generate mock optimized sequence and results
    set CAI=0.85
    set GC_CONTENT=52.3
    set MFI=-320000.0

    REM Generate result.txt
    echo sequence:ATGGCTGGATCCAACGAGCAGGTGCCATGGCTGGATCCAACGAGCAGGTGCCAAGGTTGGCACCTTCAAA> "%RESULT_FILE%"
    echo CAI:%CAI%>> "%RESULT_FILE%"
    echo GCContent:%GC_CONTENT%>> "%RESULT_FILE%"
    echo MFI:%MFI%>> "%RESULT_FILE%"

    echo [%date% %time%] Successfully generated result files >> "%LOG_FILE%"
    echo [%date% %time%] Optimized sequence length: 50 bp >> "%LOG_FILE%"

    exit /b 0
) else (
    REM ===== Failure scenario =====
    echo Task execution failed

    set ERROR_MSG=Analysis failed: Sequence format does not meet optimization requirements for %TARGET_SPECIES%. Please check if input sequence contains illegal characters or if sequence length is within allowed range.

    REM Generate run.txt (failure status)
    echo startedTime:%date% %time% > "%STATUS_FILE%"
    echo completedTime:%date% %time% >> "%STATUS_FILE%"
    echo status:FAILED >> "%STATUS_FILE%"
    echo info:%ERROR_MSG% >> "%STATUS_FILE%"

    REM Generate empty result.txt
    echo. > "%RESULT_FILE%"

    REM Generate error.txt
    echo Error type: SEQUENCE_VALIDATION_ERROR > "%ERROR_FILE%"
    echo Error time: %date% %time% >> "%ERROR_FILE%"
    echo Error message: %ERROR_MSG% >> "%ERROR_FILE%"
    echo Suggestion: Please check input sequence format and ensure it contains only valid amino acid or nucleotide characters >> "%ERROR_FILE%"

    echo [%date% %time%] Task failed: %ERROR_MSG% >> "%LOG_FILE%"

    exit /b 1
)
