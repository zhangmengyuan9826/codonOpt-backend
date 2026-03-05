package com.codonopt.constants;

import java.util.Arrays;
import java.util.List;

/**
 * 邮件相关常量
 */
public class EmailConstants {

    /**
     * 邮件主题
     */
    public static final String VERIFICATION_SUBJECT = "【密码子优化系统】邮箱验证";
    public static final String TASK_SUBMITTED_SUBJECT = "【密码子优化系统】任务提交成功";
    public static final String TASK_COMPLETION_SUBJECT = "【密码子优化系统】任务完成通知";
    public static final String TASK_FAILURE_SUBJECT = "【密码子优化系统】任务失败通知";

    /**
     * 邮件模板名称
     */
    public static final String VERIFICATION_TEMPLATE = "email/verification";
    public static final String TASK_SUBMITTED_TEMPLATE = "email/task_submitted";
    public static final String TASK_COMPLETION_TEMPLATE = "email/task_completed";
    public static final String TASK_FAILURE_TEMPLATE = "email/task_failed";

    /**
     * 前端URL（用于邮件中的链接）
     */
    public static final String FRONTEND_URL = "http://localhost:8080";

    /**
     * 测试模式：所有邮件发送到测试邮箱列表
     */
    public static final boolean TEST_MODE = true;

    /**
     * 测试邮箱列表
     */
    public static final List<String> TEST_EMAILS = Arrays.asList(
            "zhangmengyuan1@genomics.cn",
            "zhangmengyuan9826@gmail.com"
    );

    /**
     * 邮件模板（保留用于向后兼容）
     */
    public static final String VERIFICATION_EMAIL_TEMPLATE =
            "尊敬的用户 %s：\n\n" +
            "您好！\n\n" +
            "您正在注册密码子优化系统，您的验证码是：%s\n\n" +
            "验证码有效期为5分钟，请尽快完成验证。\n\n" +
            "如果这不是您本人的操作，请忽略此邮件。\n\n" +
            "此致\n" +
            "密码子优化系统团队";

    /**
     * 系统发件人信息
     */
    public static final String FROM_NAME = "密码子优化系统";
}
