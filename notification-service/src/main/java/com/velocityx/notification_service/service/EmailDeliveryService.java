package com.velocityx.notification_service.service;

import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailDeliveryService {
    
    private final JavaMailSender mailSender;
    private final Timer emailDeliveryTimer;
    
    @Value("${notification.email.from}")
    private String fromEmail;
    
    @Value("${notification.email.from-name}")
    private String fromName;
    
    public void sendEmail(String to, String subject, String htmlContent, String textContent) {
        emailDeliveryTimer.record(() -> {
            try {
                if (htmlContent != null && !htmlContent.isEmpty()) {
                    sendHtmlEmail(to, subject, htmlContent);
                } else {
                    sendTextEmail(to, subject, textContent);
                }
                log.info("Email sent successfully to: {}", to);
            } catch (Exception e) {
                log.error("Failed to send email to: {}", to, e);
                throw new RuntimeException("Email delivery failed", e);
            }
        });
    }
    
    private void sendTextEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
    
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail, fromName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        
        mailSender.send(message);
    }
    
    public String buildEmailFromTemplate(String templateName, Map<String, Object> data) {
        return switch (templateName) {
            case "transaction_success" -> buildTransactionSuccessEmail(data);
            case "transaction_failed" -> buildTransactionFailedEmail(data);
            case "account_created" -> buildAccountCreatedEmail(data);
            default -> buildGenericEmail(data);
        };
    }
    
    private String buildTransactionSuccessEmail(Map<String, Object> data) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif;">
                <h2 style="color: #28a745;">Transaction Successful!</h2>
                <p>Dear %s,</p>
                <p>Your transaction has been completed successfully.</p>
                <div style="background: #f8f9fa; padding: 15px; border-radius: 5px;">
                    <p><strong>Transaction ID:</strong> %s</p>
                    <p><strong>Amount:</strong> %s</p>
                    <p><strong>Date:</strong> %s</p>
                </div>
                <p>Thank you for using VelocityX!</p>
            </body>
            </html>
            """,
            data.getOrDefault("recipientName", "Customer"),
            data.getOrDefault("transactionId", "N/A"),
            data.getOrDefault("amount", "N/A"),
            data.getOrDefault("date", "N/A")
        );
    }
    
    private String buildTransactionFailedEmail(Map<String, Object> data) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif;">
                <h2 style="color: #dc3545;">Transaction Failed</h2>
                <p>Dear %s,</p>
                <p>We're sorry, but your transaction could not be completed.</p>
                <div style="background: #f8f9fa; padding: 15px; border-radius: 5px;">
                    <p><strong>Transaction ID:</strong> %s</p>
                    <p><strong>Reason:</strong> %s</p>
                </div>
                <p>Please try again or contact support.</p>
            </body>
            </html>
            """,
            data.getOrDefault("recipientName", "Customer"),
            data.getOrDefault("transactionId", "N/A"),
            data.getOrDefault("reason", "Unknown error")
        );
    }
    
    private String buildAccountCreatedEmail(Map<String, Object> data) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif;">
                <h2 style="color: #007bff;">Welcome to VelocityX!</h2>
                <p>Dear %s,</p>
                <p>Your account has been created successfully.</p>
                <p>You can now start using our services.</p>
                <p>Best regards,<br>VelocityX Team</p>
            </body>
            </html>
            """,
            data.getOrDefault("recipientName", "Customer")
        );
    }
    
    private String buildGenericEmail(Map<String, Object> data) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif;">
                <h2>Notification from VelocityX</h2>
                <p>Dear %s,</p>
                <p>%s</p>
            </body>
            </html>
            """,
            data.getOrDefault("recipientName", "Customer"),
            data.getOrDefault("message", "You have a new notification.")
        );
    }
}
