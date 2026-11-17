package com.nihongu.lokibe.Service.Report;

import com.nihongu.lokibe.Dto.Report.ReportRequest;
import com.nihongu.lokibe.Entity.ReportEntity;
import com.nihongu.lokibe.Exception.BadRequestException;
import com.nihongu.lokibe.Exception.ResourceNotFoundException;
import com.nihongu.lokibe.Repositoty.ReportRepository;
import com.nihongu.lokibe.Repositoty.UserRepository;
import com.nihongu.lokibe.Util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void report(ReportRequest request) {
        Long reporterId = SecurityUtils.getCurrentUserId();
        if (reporterId.equals(request.getUserId())) {
            throw new BadRequestException("Không thể tự báo cáo chính mình.");
        }
        if (!userRepository.existsById(request.getUserId())) {
            throw new ResourceNotFoundException("Không tìm thấy người dùng.");
        }
        ReportEntity report = reportRepository.save(ReportEntity.builder()
                .reporterId(reporterId)
                .reportedUserId(request.getUserId())
                .photoId(request.getPhotoId())
                .reason(request.getReason())
                .build());
        log.warn("Báo cáo #{}: user {} báo cáo user {} (ảnh {})", report.getId(), reporterId, request.getUserId(), request.getPhotoId());
    }
}
