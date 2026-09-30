package org.example.rest.service;

import org.example.contract.dto.ReportRequest;
import org.example.contract.dto.ReportResponse;
import org.example.contract.exception.ResourceNotFoundException;
import org.example.rest.entity.ReportEntity;
import org.example.rest.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final ProfileService profileService;

    public ReportService(ReportRepository reportRepository, ProfileService profileService) {
        this.reportRepository = reportRepository;
        this.profileService = profileService;
    }

    @Transactional
    public ReportResponse createReport(ReportRequest request) {

        profileService.findById(request.reporterId());
        profileService.findById(request.reportedId());

        ReportEntity entity = new ReportEntity();
        entity.setId(UUID.randomUUID());
        entity.setReporterId(request.reporterId());
        entity.setReportedId(request.reportedId());
        entity.setReason(request.reason());
        entity.setStatus("PENDING");
        entity.setCreatedAt(Instant.now());

        ReportEntity saved = reportRepository.save(entity);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> getAllReports() {
        return reportRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateReportStatus(UUID reportId, String newStatus) {
        ReportEntity entity = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", reportId));

        entity.setStatus(newStatus);
        reportRepository.save(entity);
    }

    @Transactional
    public void blockUser(UUID userId) {
        // Вызываем блокировку в ProfileService (меняет canSearch на false)
        profileService.blockUser(userId);
        System.out.println("🛡️ Администратор заблокировал пользователя с ID: " + userId);
    }

    private ReportResponse toDto(ReportEntity entity) {
        return new ReportResponse(
                entity.getId(),
                entity.getReporterId(),
                entity.getReportedId(),
                entity.getReason(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}