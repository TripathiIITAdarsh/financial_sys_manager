package com.financemanager.controller;

import com.financemanager.dto.ReportResponse;
import com.financemanager.security.CurrentUserProvider;
import com.financemanager.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final CurrentUserProvider currentUserProvider;

    public ReportController(ReportService reportService, CurrentUserProvider currentUserProvider) {
        this.reportService = reportService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/monthly/{year}/{month}")
    public ResponseEntity<ReportResponse> monthly(@PathVariable int year, @PathVariable int month) {
        return ResponseEntity.ok(reportService.monthlyReport(currentUserProvider.getCurrentUser(), year, month));
    }

    @GetMapping("/yearly/{year}")
    public ResponseEntity<ReportResponse> yearly(@PathVariable int year) {
        return ResponseEntity.ok(reportService.yearlyReport(currentUserProvider.getCurrentUser(), year));
    }
}
