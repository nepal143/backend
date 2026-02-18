package com.placementgo.backend.dashboard.service;

import com.placementgo.backend.dashboard.dto.*;
import com.placementgo.backend.dashboard.entity.*;
import com.placementgo.backend.dashboard.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationNoteRepository noteRepository;

    public ApplicationResponse create(CreateApplicationRequest request, UUID userId) {

        Application application = Application.builder()
                .company(request.getCompany())
                .role(request.getRole())
                .jobLink(request.getJobLink())
                .appliedDate(request.getAppliedDate())
                .status(ApplicationStatus.APPLIED)
                .userId(userId)
                .build();

        return mapToResponse(applicationRepository.save(application));
    }

    public List<ApplicationResponse> getAll(UUID userId) {
        return applicationRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ApplicationResponse update(UUID id, UpdateApplicationRequest request, UUID userId) {

        Application app = getOwnedApplication(id, userId);

        app.setCompany(request.getCompany());
        app.setRole(request.getRole());
        app.setJobLink(request.getJobLink());
        app.setAppliedDate(request.getAppliedDate());

        return mapToResponse(applicationRepository.save(app));
    }

    public ApplicationResponse updateStatus(UUID id, UpdateStatusRequest request, UUID userId) {

        Application app = getOwnedApplication(id, userId);
        app.setStatus(request.getStatus());

        return mapToResponse(applicationRepository.save(app));
    }

    public void addNote(UUID applicationId, CreateNoteRequest request, UUID userId) {

        getOwnedApplication(applicationId, userId);

        ApplicationNote note = ApplicationNote.builder()
                .applicationId(applicationId)
                .userId(userId)
                .note(request.getNote())
                .build();

        noteRepository.save(note);
    }

    private Application getOwnedApplication(UUID id, UUID userId) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if (!app.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        return app;
    }

    private ApplicationResponse mapToResponse(Application app) {
        return ApplicationResponse.builder()
                .id(app.getId())
                .company(app.getCompany())
                .role(app.getRole())
                .jobLink(app.getJobLink())
                .appliedDate(app.getAppliedDate())
                .status(app.getStatus())
                .createdAt(app.getCreatedAt())
                .build();
    }
}
