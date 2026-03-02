package com.codonopt.service.mail;

import javax.mail.MessagingException;
import java.io.File;

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
}
