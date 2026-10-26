package com.example.chosim.chosim.common.audit;

import com.example.chosim.chosim.common.error.ErrorCode;
import com.example.chosim.chosim.common.error.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogQueryService {
    private final AuditLogRepository auditLogRepository;

    public Page<AuditLog> findPage(int page) {
        if (page < 0) throw new AppException(ErrorCode.INVALID_INPUT);
        return auditLogRepository.findAll(PageRequest.of(page, 20,
                Sort.by(Sort.Direction.DESC, "createdAt", "id")));
    }
}
