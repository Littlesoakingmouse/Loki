package com.nihongu.lokibe.Controller;

import com.nihongu.lokibe.Dto.Report.ReportRequest;
import com.nihongu.lokibe.Service.Report.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    // Báo cáo một người dùng (kèm ảnh nếu có)
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void report(@Valid @RequestBody ReportRequest request) {
        reportService.report(request);
    }
}
