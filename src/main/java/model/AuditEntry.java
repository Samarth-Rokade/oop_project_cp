package model;

import java.time.LocalDateTime;

/** One audit_log row with the acting user's name (null for system events). */
public record AuditEntry(long logId, LocalDateTime actionTime, String userName, String action,
                         String entityType, Integer entityId, String details) {
}
