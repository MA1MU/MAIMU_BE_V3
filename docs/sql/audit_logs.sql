-- 사람이 대상 DB를 확인한 뒤 직접 실행한다. 앱은 ddl-auto: none이다.
-- actor/target은 과거 식별자일 뿐이다. FK/연관관계를 절대 추가하지 않는다.
CREATE TABLE audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    action VARCHAR(32) NOT NULL,
    actor_member_id BIGINT NULL,
    target_id BIGINT NOT NULL,
    result VARCHAR(16) NOT NULL,
    error_message VARCHAR(500) NULL,
    trace_id VARCHAR(64) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_audit_logs_created_at (created_at, id),
    INDEX idx_audit_logs_trace_id (trace_id),
    CONSTRAINT chk_audit_logs_action CHECK (action IN ('MAIMU_DELETE', 'GROUP_DELETE', 'MEMBER_WITHDRAW')),
    CONSTRAINT chk_audit_logs_result CHECK (result IN ('SUCCESS', 'FAILURE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
