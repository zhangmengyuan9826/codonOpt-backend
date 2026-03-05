package com.codonopt.service.mail;

import javax.mail.MessagingException;
import java.io.File;
import java.util.Map;

/**
 * 邮件服务接口
 */
public interface MailService {

    /**
     * 发送纯文本邮件
     *
     * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    void sendEmail(String to, String subject, String content);

    /**
     * 发送带附件的邮件
     *
     * @param to           收件人邮箱
     * @param subject      邮件主题
     * @param content      邮件内容
     * @param attachment   附件文件
     */
    void sendEmailWithAttachment(String to, String subject, String content, File attachment);

    /**
     * 发送HTML邮件
     *
     * @param to          收件人邮箱
     * @param subject     邮件主题
     * @param htmlContent HTML内容
     */
    void sendHtmlEmail(String to, String subject, String htmlContent);

    /**
     * 发送模板邮件
     *
     * @param to         收件人邮箱
     * @param subject    邮件主题
     * @param templateName 模板名称（不带.html后缀）
     * @param variables  模板变量
     */
    void sendTemplateEmail(String to, String subject, String templateName, Map<String, Object> variables);

    /**
     * 发送带附件的模板邮件
     *
     * @param to         收件人邮箱
     * @param subject    邮件主题
     * @param templateName 模板名称
     * @param variables  模板变量
     * @param attachment  附件文件
     */
    void sendTemplateEmailWithAttachment(String to, String subject, String templateName,
                                       Map<String, Object> variables, File attachment);
}
