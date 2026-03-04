@echo off
REM 密码子优化任务模拟执行脚本 (Windows版本)
REM 用于测试任务队列功能
REM 执行时间：5分钟

setlocal enabledelayedexpansion

REM 从参数中获取任务信息
set TASK_ID=%1
set OUTPUT_DIR=%2
set SEQUENCE_TYPE=%3
if "%SEQUENCE_TYPE%"=="" set SEQUENCE_TYPE=AMINO_ACID
set TARGET_SPECIES=%4
if "%TARGET_SPECIES%"=="" set TARGET_SPECIES=HUMAN

REM 如果输出目录不存在，创建它
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

REM 日志文件
set LOG_FILE=%OUTPUT_DIR%\execution.log
set STATUS_FILE=%OUTPUT_DIR%\run.txt
set RESULT_FILE=%OUTPUT_DIR%\result.txt
set ERROR_FILE=%OUTPUT_DIR%\error.txt

REM 记录开始时间
echo [%date% %time%] 任务开始执行: %TASK_ID% >> "%LOG_FILE%"
echo [%date% %time%] 输出目录: %OUTPUT_DIR% >> "%LOG_FILE%"
echo [%date% %time%] 序列类型: %SEQUENCE_TYPE% >> "%LOG_FILE%"
echo [%date% %time%] 目标物种: %TARGET_SPECIES% >> "%LOG_FILE%"

REM 模拟任务进度（5分钟 = 300秒）
set TOTAL_STEPS=10
set STEP_DURATION=30

echo [%date% %time%] 开始模拟任务执行... >> "%LOG_FILE%"

for /L %%i in (1,1,%TOTAL_STEPS%) do (
    echo [%date% %time%] 执行进度: %%i/%TOTAL_STEPS% >> "%LOG_FILE%"
    REM 使用ping命令来实现延迟（更可靠）
    ping 127.0.0.1 -n %STEP_DURATION% > nul
)

REM 记录结束时间
echo [%date% %time%] 任务执行完成 >> "%LOG_FILE%"

REM 模拟随机成功或失败（90%成功率）
set /a RANDOM_NUMBER=%RANDOM% %% 100
if %RANDOM_NUMBER% lss 90 (
    REM ===== 成功场景 =====
    echo 任务执行成功

    REM 生成run.txt
    echo startedTime:%date% %time% > "%STATUS_FILE%"
    echo completedTime:%date% %time% >> "%STATUS_FILE%"
    echo status:SUCCESS >> "%STATUS_FILE%"

    REM 生成模拟的优化序列和结果
    set CAI=0.85
    set GC_CONTENT=52.3
    set MFI=-320000.0

    REM 生成result.txt（使用延迟变量扩展）
    echo sequence:ATGGCTGGATCCAACGAGCAGGTGCCATGGCTGGATCCAACGAGCAGGTGCCAAGGTTGGCACCTTCAAA> "%RESULT_FILE%"
    echo CAI:%CAI%>> "%RESULT_FILE%"
    echo GCContent:%GC_CONTENT%>> "%RESULT_FILE%"
    echo MFI:%MFI%>> "%RESULT_FILE%"

    echo [%date% %time%] 成功生成结果文件 >> "%LOG_FILE%"
    echo [%date% %time%] 优化序列长度: 50 bp >> "%LOG_FILE%"

    exit /b 0
) else (
    REM ===== 失败场景 =====
    echo 任务执行失败

    set ERROR_MSG=模拟分析失败：序列格式不符合%TARGET_SPECIES%的优化要求，请检查输入序列是否包含非法字符或序列长度是否在允许范围内。

    REM 生成run.txt（失败状态）
    echo startedTime:%date% %time% > "%STATUS_FILE%"
    echo completedTime:%date% %time% >> "%STATUS_FILE%"
    echo status:FAILED >> "%STATUS_FILE%"
    echo info:%ERROR_MSG% >> "%STATUS_FILE%"

    REM 生成空的result.txt
    echo. > "%RESULT_FILE%"

    REM 生成error.txt
    echo 错误类型: SEQUENCE_VALIDATION_ERROR > "%ERROR_FILE%"
    echo 错误时间: %date% %time% >> "%ERROR_FILE%"
    echo 错误信息: %ERROR_MSG% >> "%ERROR_FILE%"
    echo 建议: 请检查输入序列格式，确保只包含合法的氨基酸或核苷酸字符 >> "%ERROR_FILE%"

    echo [%date% %time%] 任务失败: %ERROR_MSG% >> "%LOG_FILE%"

    exit /b 1
)
