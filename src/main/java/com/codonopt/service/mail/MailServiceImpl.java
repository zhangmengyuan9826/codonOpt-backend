package com.codonopt.service.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.util.Map;

/**
 * 邮件服务实现（支持HTML模板和附件）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String fromEmail;

    @Autowired(required = false)
    private TemplateEngine templateEngine;

    @Override
    public void sendEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true); // true表示HTML

            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    @Override
    public void sendEmailWithAttachment(String to, String subject, String content, File attachment) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true); // HTML email

            if (attachment != null && attachment.exists()) {
                helper.addAttachment(attachment.getName(), new org.springframework.core.io.FileSystemResource(attachment));
                log.info("Attachment added: {}", attachment.getName());
            }

            mailSender.send(message);
            log.info("Email with attachment sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email with attachment to: {}", to, e);
            throw new RuntimeException("Failed to send email with attachment: " + e.getMessage());
        }
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // HTML content

            mailSender.send(message);
            log.info("HTML email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to: {}", to, e);
            throw new RuntimeException("Failed to send HTML email: " + e.getMessage());
        }
    }

    @Override
    public void sendTemplateEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            if (templateEngine == null) {
                log.warn("TemplateEngine not configured, falling back to plain text email");
                sendEmail(to, subject, variables.toString());
                return;
            }

            // 使用Thymeleaf模板引擎生成HTML内容
            Context context = new Context();
            context.setVariables(variables);

            String htmlContent = templateEngine.process(templateName, context);

            sendHtmlEmail(to, subject, htmlContent);
            log.info("Template email sent successfully to: {} using template: {}", to, templateName);
        } catch (Exception e) {
            log.error("Failed to send template email to: {}", to, e);
            throw new RuntimeException("Failed to send template email: " + e.getMessage());
        }
    }

    @Override
    public void sendTemplateEmailWithAttachment(String to, String subject, String templateName,
                                                   Map<String, Object> variables, File attachment) {
        try {
            if (templateEngine == null) {
                log.warn("TemplateEngine not configured, falling back to plain text email");
                sendEmailWithAttachment(to, subject, variables.toString(), attachment);
                return;
            }

            // 使用Thymeleaf模板引擎生成HTML内容
            Context context = new Context();
            context.setVariables(variables);

            String htmlContent = templateEngine.process(templateName, context);

            // 发送带附件的HTML邮件
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // HTML content

            if (attachment != null && attachment.exists()) {
                helper.addAttachment(attachment.getName(), new org.springframework.core.io.FileSystemResource(attachment));
                log.info("Attachment added: {}", attachment.getName());
            }

            mailSender.send(message);
            log.info("Template email with attachment sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send template email with attachment to: {}", to, e);
            throw new RuntimeException("Failed to send template email with attachment: " + e.getMessage());
        }
    }
}
