package com.placementgo.backend.autoapply.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Sends an application email to jobs that expose an apply email address.
 * Attaches the user's resume PDF (Base64-encoded) from the Resume entity.
 */
@Service
@Slf4j
public class AutoApplyEmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    private final ObjectMapper objectMapper;

    public AutoApplyEmailService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * @param toEmail           The job's apply email address
     * @param templateJson      JSON string from ApplicationTemplateService
     * @param candidateName     Candidate's full name (for the From display name)
     * @param resumePdfBase64   Base64-encoded PDF bytes from Resume.generatedPdfBase64
     * @param resumeFileName    Original file name, e.g. "john_doe_resume.pdf"
     * @return true if sent successfully
     */
    public boolean sendApplication(String toEmail,
                                   String templateJson,
                                   String candidateName,
                                   String resumePdfBase64,
                                   String resumeFileName) {
        if (mailSender == null) {
            log.warn("JavaMailSender not configured – skipping email apply to {}", toEmail);
            return false;
        }
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("No apply email provided");
            return false;
        }

        try {
            String subject = "Application";
            String body = "";

            if (templateJson != null && !templateJson.isBlank()) {
                try {
                    JsonNode node = objectMapper.readTree(templateJson);
                    subject = node.path("subject").asText("Job Application");
                    body = node.path("coverLetter").asText("");
                } catch (Exception e) {
                    log.warn("Could not parse template JSON: {}", e.getMessage());
                }
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, candidateName != null ? candidateName : "Applicant");
            helper.setTo(toEmail);
            helper.setSubject(subject);

            // Plain-text body with a simple HTML wrapper
            String htmlBody = """
                    <div style="font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;
                                max-width:640px;color:#1e293b;line-height:1.7;">
                        %s
                        <hr style="border:none;border-top:1px solid #e2e8f0;margin:24px 0;"/>
                        <p style="font-size:12px;color:#94a3b8;">
                            Sent via <a href="https://placementgo.in" style="color:#2563EB;">PlacementGO</a>
                        </p>
                    </div>
                    """.formatted(body.replace("\n", "<br/>"));

            helper.setText(htmlBody, true);

            // Attach resume if available
            if (resumePdfBase64 != null && !resumePdfBase64.isBlank()) {
                byte[] pdfBytes = java.util.Base64.getDecoder().decode(resumePdfBase64);
                String fileName = (resumeFileName != null && !resumeFileName.isBlank())
                        ? resumeFileName
                        : "resume.pdf";
                helper.addAttachment(fileName, new ByteArrayResource(pdfBytes), "application/pdf");
            }

            mailSender.send(message);
            log.info("Application email sent to {} for candidate {}", toEmail, candidateName);
            return true;

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send application email to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }

    /**
     * Sends a "we applied on your behalf" confirmation email to the PlacementGO user.
     *
     * @param userEmail   The user's own email address
     * @param jobTitle    e.g. "Backend Engineer"
     * @param company     e.g. "Stripe"
     * @param applyEmail  The employer email the application was sent to
     */
    public void sendUserConfirmation(String userEmail, String jobTitle, String company, String applyEmail) {
        if (mailSender == null || userEmail == null || userEmail.isBlank()) return;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(fromEmail, "PlacementGO");
            helper.setTo(userEmail);
            helper.setSubject("✅ Applied: " + jobTitle + " at " + company);

            String html = """
                    <div style="font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;
                                max-width:560px;color:#1e293b;line-height:1.7;">
                        <h2 style="color:#2563EB;margin-bottom:4px;">Application Submitted</h2>
                        <p>PlacementGO auto-applied to the following role on your behalf:</p>
                        <table style="border-collapse:collapse;width:100%%;margin:16px 0;">
                            <tr><td style="padding:8px 12px;background:#f1f5f9;font-weight:600;width:40%%;">Role</td>
                                <td style="padding:8px 12px;">%s</td></tr>
                            <tr><td style="padding:8px 12px;background:#f1f5f9;font-weight:600;">Company</td>
                                <td style="padding:8px 12px;">%s</td></tr>
                            <tr><td style="padding:8px 12px;background:#f1f5f9;font-weight:600;">Sent to</td>
                                <td style="padding:8px 12px;">%s</td></tr>
                        </table>
                        <p>Your tailored cover letter and resume were attached. You can track this
                           application in your
                           <a href="https://placementgo.in/tracker" style="color:#2563EB;">dashboard tracker</a>.</p>
                        <hr style="border:none;border-top:1px solid #e2e8f0;margin:24px 0;"/>
                        <p style="font-size:12px;color:#94a3b8;">
                            You received this because auto-apply is enabled on
                            <a href="https://placementgo.in" style="color:#2563EB;">PlacementGO</a>.
                            <a href="https://placementgo.in/autoapply" style="color:#94a3b8;">Manage settings</a>
                        </p>
                    </div>
                    """.formatted(jobTitle, company, applyEmail);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Confirmation email sent to user {} for {}/{}", userEmail, jobTitle, company);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.warn("Could not send confirmation email to {}: {}", userEmail, e.getMessage());
        }
    }
}
