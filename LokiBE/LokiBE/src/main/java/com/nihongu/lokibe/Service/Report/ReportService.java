package com.nihongu.lokibe.Service.Report;

import com.nihongu.lokibe.Dto.Report.ReportRequest;

public interface ReportService {
    // Lưu báo cáo vi phạm để admin xử lý
    void report(ReportRequest request);
}
