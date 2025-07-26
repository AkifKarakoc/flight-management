package com.flightmanagement.reference.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightmanagement.reference.entity.AuditLog;
import com.flightmanagement.reference.repository.AuditLogRepository;
import com.flightmanagement.reference.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper defaultObjectMapper;

    @Async
    public void logAction(String entityType, Long entityId, String action,
                          UserContext userContext, Object oldData, Object newData) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .userId(userContext != null ? userContext.getUserId() : null)
                    .username(userContext != null ? userContext.getUsername() : "system")
                    .airlineId(userContext != null ? userContext.getAirlineId() : null)
                    .oldData(oldData != null ? defaultObjectMapper.writeValueAsString(oldData) : null)
                    .newData(newData != null ? defaultObjectMapper.writeValueAsString(newData) : null)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log created: {} {} for entity {}:{}", action, entityType, entityType, entityId);

        } catch (Exception e) {
            log.error("Failed to create audit log for {}:{} - {}", entityType, entityId, e.getMessage());
        }
    }
}