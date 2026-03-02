package com.codonopt.constants;

/**
 * 邮件相关常量
 */
public class EmailConstants {

    /**
     * 邮件主题
     */
    public static final String VERIFICATION_SUBJECT = "【密码子优化系统】邮箱验证码";
    public static final String TASK_COMPLETION_SUBJECT = "【密码子优化系统】任务完成通知";
    public static final String TASK_FAILURE_SUBJECT = "【密码子优化系统】任务失败通知";

    /**
     * 邮件模板
     */
    public static final String VERIFICATION_EMAIL_TEMPLATE =
            "尊敬的用户 %s：\n\n" +
            "您好！\n\n" +
            "您正在注册密码子优化系统，您的验证码是：%s\n\n" +
            "验证码有效期为5分钟，请尽快完成验证。\n\n" +
            "如果这不是您本人的操作，请忽略此邮件。\n\n" +
            "此致\n" +
            "密码子优化系统团队";

    public static final String TASK_COMPLETION_TEMPLATE =
            "尊敬的用户 %s：\n\n" +
            "您好！\n\n" +
            "您提交的密码子优化任务已完成。\n\n" +
            "任务信息：\n" +
            "- 任务ID：%s\n" +
            "- 序列类型：%s\n" +
            "- 目标物种：%s\n" +
            "- 提交时间：%s\n" +
            "- 完成时间：%s\n\n" +
            "优化结果已作为附件发送，请查收。\n\n" +
            "感谢您使用密码子优化系统！\n\n" +
            "此致\n" +
            "密码子优化系统团队";

    public static final String TASK_FAILURE_TEMPLATE =
            "尊敬的用户 %s：\n\n" +
            "您好！\n\n" +
            "很抱歉，您提交的密码子优化任务执行失败。\n\n" +
            "任务信息：\n" +
            "- 任务ID：%s\n" +
            "- 序列类型：%s\n" +
            "- 目标物种：%s\n" +
            "- 提交时间：%s\n" +
            "- 失败原因：%s\n\n" +
            "请检查输入参数后重试，或联系技术支持。\n\n" +
            "感谢您使用密码子优化系统！\n\n" +
            "此致\n" +
            "密码子优化系统团队";

    /**
     * 系统发件人信息
     */
    public static final String FROM_NAME = "密码子优化系统";
}
