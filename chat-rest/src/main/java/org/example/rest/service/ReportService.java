package org.example.rest.service;

import org.example.contract.dto.ReportRequest;
import org.example.contract.dto.ReportResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ReportService {
    private final Map<Long, ReportResponse> reports = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);


    private final ProfileService profileService;

    public ReportService(ProfileService profileService) {
        this.profileService = profileService;
    }

    public ReportResponse createReport(ReportRequest request) {

        profileService.findById(request.reporterId());
        profileService.findById(request.reportedId());

        long id = sequence.incrementAndGet();
        ReportResponse report = new ReportResponse(
                id, request.reporterId(), request.reportedId(),
                request.reason(), "PENDING", Instant.now()
        );
        reports.put(id, report);
        return report;
    }

    public List<ReportResponse> getAllReports() {
        return new ArrayList<>(reports.values());
    }

    public void updateReportStatus(Long reportId, String newStatus) {
        ReportResponse report = reports.get(reportId);
        if (report != null) {

            ReportResponse updated = new ReportResponse(
                    report.id(), report.reporterId(), report.reportedId(),
                    report.reason(), newStatus, report.createdAt()
            );
            reports.put(reportId, updated);
        }
    }

    public void blockUser(Long userId) {

        System.out.println(" Администратор заблокировал пользователя с ID: " + userId);
    }
}