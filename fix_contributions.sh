#!/bin/bash
# 修正Git提交的作者信息
# 警告：这会重写历史，仅在新仓库或确认安全时使用

CORRECT_NAME="zhangmengyuan9826"
CORRECT_EMAIL="your_github_primary_email@example.com"  # 替换为你的GitHub主邮箱

# 修正最近的提交（使用 --commits=<数量> 只修正最近的N个提交）
git filter-branch --env-filter '
    OLD_EMAIL="zhangmengyuan9826@gmail.com"
    OLD_NAME="zhangmengyuan1"
    
    if [ "$GIT_COMMITTER_EMAIL" = "$OLD_EMAIL" ]; then
        export GIT_COMMITTER_NAME="$CORRECT_NAME"
        export GIT_COMMITTER_EMAIL="$CORRECT_EMAIL"
    fi
    
    if [ "$GIT_AUTHOR_EMAIL" = "$OLD_EMAIL" ]; then
        export GIT_AUTHOR_NAME="$CORRECT_NAME"
        export GIT_AUTHOR_EMAIL="$CORRECT_EMAIL"
    fi
' --commits=10 --tag-name-filter cat -- --all

echo "修正完成！现在强制推送到远程仓库："
echo "git push origin --force --all"
echo "git push origin --force --tags"
