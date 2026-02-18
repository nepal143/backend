package com.placementgo.backend.dashboard.controller;

import com.placementgo.backend.dashboard.dto.*;
import com.placementgo.backend.dashboard.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    // Replace later with actual JWT extraction
    private UUID getLoggedInUserId() {
        return UUID.fromString("11111111-1111-1111-1111-111111111111");
    }

    @PostMapping
    public ApplicationResponse create(@RequestBody CreateApplicationRequest request) {
        return applicationService.create(request, getLoggedInUserId());
    }

    @GetMapping
    public List<ApplicationResponse> getAll() {
        return applicationService.getAll(getLoggedInUserId());
    }

    @PutMapping("/{id}")
    public ApplicationResponse update(
            @PathVariable UUID id,
            @RequestBody UpdateApplicationRequest request) {
        return applicationService.update(id, request, getLoggedInUserId());
    }

    @PutMapping("/{id}/status")
    public ApplicationResponse updateStatus(
            @PathVariable UUID id,
            @RequestBody UpdateStatusRequest request) {
        return applicationService.updateStatus(id, request, getLoggedInUserId());
    }

    @PostMapping("/{id}/notes")
    public void addNote(
            @PathVariable UUID id,
            @RequestBody CreateNoteRequest request) {
        applicationService.addNote(id, request, getLoggedInUserId());
    }
}
