package com.mindcompanion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.emergency.contact.email:admin@mindcompanion.com}")
    private String emergencyContactEmail;

    @Value("${app.name:Mind Companion}")
    private String appName;

    /**
     * Sends a crisis alert email to the configured emergency contact.
     *
     * @param username       the username of the user in crisis
     * @param triggerKeyword the keyword that triggered the alert
     * @param triggerReason  human-readable reason for the alert
     */
    public void sendCrisisAlertEmail(String username,
                                     String triggerKeyword,
                                     String triggerReason) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(emergencyContactEmail);
            helper.setSubject("🚨 [" + appName + "] Crisis Alert — Immediate Attention Required");
            helper.setText(buildEmailBody(username, triggerKeyword, triggerReason), true);

            mailSender.send(message);
            log.info("📧 Crisis alert email sent to {} for user '{}'", emergencyContactEmail, username);

        } catch (MessagingException e) {
            log.error("❌ Failed to send crisis alert email for user '{}': {}", username, e.getMessage());
        }
    }

    /**
     * Sends a welcome/confirmation email to a newly registered user.
     *
     * @param toEmail   recipient email address
     * @param username  the new user's username
     */
    public void sendWelcomeEmail(String toEmail, String username) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to " + appName + " 💚");
            helper.setText(buildWelcomeEmailBody(username), true);

            mailSender.send(message);
            log.info("📧 Welcome email sent to {} for user '{}'", toEmail, username);

        } catch (MessagingException e) {
            log.error("❌ Failed to send welcome email to '{}': {}", toEmail, e.getMessage());
        }
    }

    /**
     * Sends an email verification link to a newly registered user.
     */
    public void sendVerificationEmail(String toEmail, String username, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String verifyUrl = "http://localhost:8080/api/auth/verify?token=" + token;

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Verify your " + appName + " account 💙");
            helper.setText(buildVerificationEmailBody(username, verifyUrl), true);

            mailSender.send(message);
            log.info("📧 Verification email sent to {}", toEmail);

        } catch (MessagingException e) {
            log.error("❌ Failed to send verification email to '{}': {}", toEmail, e.getMessage());
        }
    }
    public void sendPasswordResetEmail(String toEmail, String username, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            String resetUrl = "http://localhost:8080/reset-password?token=" + token;
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Reset your " + appName + " password");
            helper.setText(buildPasswordResetEmailBody(username, resetUrl), true);
            mailSender.send(message);
            log.info("📧 Password reset email sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("❌ Failed to send reset email to '{}': {}", toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // Private HTML builders
    // ─────────────────────────────────────────────

    private String buildEmailBody(String username,
                                  String triggerKeyword,
                                  String triggerReason) {
        String time = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

        return """
                <html>
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                  <div style="max-width: 600px; margin: auto; background: white;
                              border-radius: 8px; padding: 30px;
                              border-left: 6px solid #e74c3c;">

                    <h2 style="color: #e74c3c;">🚨 Crisis Alert Detected</h2>
                    <p style="color: #555; font-size: 15px;">
                      The <strong>%s</strong> system has detected a potential crisis situation.
                      Immediate attention may be required.
                    </p>

                    <table style="width:100%%; border-collapse: collapse; margin-top: 20px;">
                      <tr style="background-color: #fdecea;">
                        <td style="padding: 10px; font-weight: bold; width: 40%%;">User</td>
                        <td style="padding: 10px;">%s</td>
                      </tr>
                      <tr>
                        <td style="padding: 10px; font-weight: bold;">Trigger Keyword</td>
                        <td style="padding: 10px; color: #e74c3c;"><strong>%s</strong></td>
                      </tr>
                      <tr style="background-color: #fdecea;">
                        <td style="padding: 10px; font-weight: bold;">Reason</td>
                        <td style="padding: 10px;">%s</td>
                      </tr>
                      <tr>
                        <td style="padding: 10px; font-weight: bold;">Time</td>
                        <td style="padding: 10px;">%s</td>
                      </tr>
                    </table>

                    <div style="margin-top: 25px; padding: 15px;
                                background-color: #fff3cd; border-radius: 6px;
                                border-left: 4px solid #ffc107;">
                      <strong>⚠️ Recommended Action:</strong>
                      <p style="margin: 5px 0 0;">
                        Please log in to the admin dashboard and review this user's
                        recent activity. If needed, reach out directly or escalate
                        to a licensed professional.
                      </p>
                    </div>

                    <p style="margin-top: 25px; font-size: 12px; color: #aaa;">
                      This is an automated alert from %s. Do not reply to this email.
                    </p>
                  </div>
                </body>
                </html>
                """.formatted(appName, username, triggerKeyword, triggerReason, time, appName);
    }
    private String buildVerificationEmailBody(String username, String verifyUrl) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                  <div style="max-width: 600px; margin: auto; background: white;
                              border-radius: 8px; padding: 30px;
                              border-left: 6px solid #e94560;">

                    <h2 style="color: #e94560;">💙 Verify your email</h2>
                    <p style="color: #555; font-size: 15px;">
                      Hi <strong>%s</strong>, welcome to <strong>%s</strong>!
                    </p>
                    <p style="color: #555; font-size: 15px;">
                      Please click the button below to verify your email address
                      and activate your account.
                    </p>

                    <div style="text-align: center; margin: 30px 0;">
                      <a href="%s"
                         style="background: linear-gradient(135deg, #e94560, #c62a47);
                                color: white; padding: 14px 32px; border-radius: 8px;
                                text-decoration: none; font-weight: bold; font-size: 16px;">
                        ✅ Verify Email
                      </a>
                    </div>

                    <p style="color: #888; font-size: 13px;">
                      If you didn't create an account, you can safely ignore this email.
                    </p>
                    <p style="font-size: 12px; color: #aaa;">
                      This link will remain active. — %s
                    </p>
                  </div>
                </body>
                </html>
                """.formatted(username, appName, verifyUrl, appName);
    }

    private String buildPasswordResetEmailBody(String username, String resetUrl) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                  <div style="max-width: 600px; margin: auto; background: white;
                              border-radius: 8px; padding: 30px;
                              border-left: 6px solid #e94560;">
                    <h2 style="color: #e94560;">🔐 Reset your password</h2>
                    <p style="color: #555; font-size: 15px;">Hi <strong>%s</strong>,</p>
                    <p style="color: #555; font-size: 15px;">
                      We received a request to reset your password. Click the button below.
                      This link expires in <strong>1 hour</strong>.
                    </p>
                    <div style="text-align: center; margin: 30px 0;">
                      <a href="%s"
                         style="background: linear-gradient(135deg, #e94560, #c62a47);
                                color: white; padding: 14px 32px; border-radius: 8px;
                                text-decoration: none; font-weight: bold; font-size: 16px;">
                        🔑 Reset Password
                      </a>
                    </div>
                    <p style="color: #888; font-size: 13px;">
                      If you didn't request this, ignore this email. Your password won't change.
                    </p>
                    <p style="font-size: 12px; color: #aaa;">— %s</p>
                  </div>
                </body>
                </html>
                """.formatted(username, resetUrl, appName);
    }

    private String buildWelcomeEmailBody(String username) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                  <div style="max-width: 600px; margin: auto; background: white;
                              border-radius: 8px; padding: 30px;
                              border-left: 6px solid #27ae60;">

                    <h2 style="color: #27ae60;">💚 Welcome to %s</h2>
                    <p style="color: #555; font-size: 15px;">
                      Hi <strong>%s</strong>, we're so glad you're here.
                    </p>
                    <p style="color: #555; font-size: 15px;">
                      Your mental wellness journey starts today. Serenity, your AI companion,
                      is ready to listen, support, and guide you — any time you need.
                    </p>
                    <p style="color: #555; font-size: 15px;">
                      Remember: you are never alone. 💙
                    </p>
                    <p style="margin-top: 25px; font-size: 12px; color: #aaa;">
                      This is an automated message from %s.
                    </p>
                  </div>
                </body>
                </html>
                """.formatted(appName, username, appName);
    }
}