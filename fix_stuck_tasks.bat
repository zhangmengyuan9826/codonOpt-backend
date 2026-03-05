@echo off
echo ========================================
echo 检查并修复卡住的任务
echo ========================================
echo.

REM 连接数据库并查看运行中的任务
echo 1. 查看所有RUNNING状态的任务:
mysql -u root -p12345678 -D codon_opt -e "SELECT task_id, task_name, status, started_at, TIMESTAMPDIFF(MINUTE, started_at, NOW()) as running_minutes FROM tasks WHERE status = 'RUNNING' ORDER BY started_at DESC;"
echo.

echo 2. 将运行超过10分钟的任务标记为完成:
mysql -u root -p12345678 -D codon_opt -e "UPDATE tasks SET status = 'COMPLETED', completed_at = NOW() WHERE status = 'RUNNING' AND started_at < DATE_SUB(NOW(), INTERVAL 10 MINUTE);"
echo.

echo 3. 查看更新结果:
mysql -u root -p12345678 -D codon_opt -e "SELECT COUNT(*) as count, status FROM tasks GROUP BY status;"
echo.

echo ========================================
echo 修复完成！
echo ========================================
pause
