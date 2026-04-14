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
}
