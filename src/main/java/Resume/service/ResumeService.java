package Resume.service;

import Resume.model.Resume;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class ResumeService {

    public Resume uploadResume(UUID userId, MultipartFile file) {
        // temporary stub logic
        Resume resume = new Resume();
        resume.setId(UUID.randomUUID());
        resume.setUserId(userId);
        resume.setOriginalFileName(file.getOriginalFilename());
        resume.setStatus("UPLOADED");

        return resume;
    }

    public Resume getResumeById(UUID resumeId) {
        // temporary stub logic
        Resume resume = new Resume();
        resume.setId(resumeId);
        resume.setStatus("UPLOADED");

        return resume;
    }
}