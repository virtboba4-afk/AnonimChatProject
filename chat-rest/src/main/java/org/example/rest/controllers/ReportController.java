package org.example.rest.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.contract.dto.ReportRequest;
import org.example.contract.dto.ReportResponse;
import org.example.rest.service.ReportService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "Система жалоб на пользователей")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse createReport(@Valid @RequestBody ReportRequest request) {
        return reportService.createReport(request);
    }

    @GetMapping
    public List<ReportResponse> getAllReports() {
        return reportService.getAllReports();
    }
    @PatchMapping("/{id}/status")
    public void updateStatus(@PathVariable UUID id, @RequestParam String status) {
        reportService.updateReportStatus(id, status);
    }

    @DeleteMapping("/block/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void blockUser(@PathVariable UUID userId) {
        reportService.blockUser(userId);
    }
}
