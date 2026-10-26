package com.example.chosim.chosim.common.audit;

import com.example.chosim.chosim.common.logging.MdcFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.*;

/** 애플리케이션 전체를 기동하지 않는다. H2 + 실제 Spring AOP/JPA 트랜잭션만 사용한다. */
@SpringJUnitConfig(AuditTransactionTest.Config.class)
class AuditTransactionTest {
    @Autowired Boundary boundary;
    @Autowired AuditLogService auditLogService;
    @Autowired AuditLogRepository auditLogRepository;
    @Autowired AuditLogQueryService queryService;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;

    @BeforeEach
    void prepare() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS audit_business (id BIGINT PRIMARY KEY)");
        jdbc.update("DELETE FROM audit_business");
        jdbc.update("INSERT INTO audit_business VALUES (1)");
        jdbc.execute("DROP TABLE IF EXISTS audit_logs");
        // 배포할 DDL 자체도 H2의 MySQL 호환 모드에서 실행해 엔티티와 함께 읽는다.
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            ScriptUtils.executeSqlScript(connection, new FileSystemResource("docs/sql/audit_logs.sql"));
            return null;
        });
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(42L, "", List.of()));
        MDC.put(MdcFilter.TRACE_ID, "audit-test-trace");
    }

    @AfterEach
    void cleanContext() {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void businessRollbackStillLeavesFailureAudit() {
        assertThatThrownBy(() -> boundary.delete(1L, true, false))
                .isInstanceOf(IllegalStateException.class).hasMessage("삭제 중 실패");
        assertThat(businessCount()).isEqualTo(1);
        AuditLog audit = singleAudit();
        assertThat(audit.getAction()).isEqualTo(AuditAction.MAIMU_DELETE);
        assertThat(audit.getResult()).isEqualTo(AuditResult.FAILURE);
        assertThat(audit.getActorMemberId()).isEqualTo(42L);
        assertThat(audit.getTargetId()).isEqualTo(1L);
        assertThat(audit.getTraceId()).isEqualTo("audit-test-trace");
        assertThat(audit.getErrorMessage()).contains("삭제 중 실패");
    }

    @Test
    void commitFailureIsNotRecordedAsSuccess() {
        assertThatThrownBy(() -> boundary.delete(1L, false, true))
                .isInstanceOf(IllegalStateException.class).hasMessage("커밋 직전 실패");
        assertThat(businessCount()).isEqualTo(1);
        assertThat(singleAudit().getResult()).isEqualTo(AuditResult.FAILURE);
    }

    @Test
    void successfulBusinessCommitLeavesSuccessAudit() {
        assertThat(boundary.delete(1L, false, false)).isEqualTo("삭제 완료");
        assertThat(businessCount()).isZero();
        AuditLog audit = singleAudit();
        assertThat(audit.getResult()).isEqualTo(AuditResult.SUCCESS);
        assertThat(audit.getErrorMessage()).isNull();
        assertThat(audit.getCreatedAt()).isNotNull();
    }

    @Test
    void missingAuditTableDoesNotRollbackSuccessfulBusiness() {
        jdbc.execute("DROP TABLE audit_logs");
        assertThat(boundary.delete(1L, false, false)).isEqualTo("삭제 완료");
        assertThat(businessCount()).isZero();
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
    }

    @Test
    void missingAuditTableDoesNotReplaceBusinessException() {
        jdbc.execute("DROP TABLE audit_logs");
        assertThatThrownBy(() -> boundary.delete(1L, true, false))
                .isInstanceOf(IllegalStateException.class).hasMessage("삭제 중 실패");
        assertThat(businessCount()).isEqualTo(1);
    }

    @Test
    void requiresNewSurvivesOuterRollbackWhileBusinessRowIsLocked() {
        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        assertThatThrownBy(() -> outer.executeWithoutResult(status -> {
            jdbc.queryForObject("SELECT id FROM audit_business WHERE id = 1 FOR UPDATE", Long.class);
            jdbc.update("DELETE FROM audit_business WHERE id = 1");
            auditLogService.record(AuditAction.MEMBER_WITHDRAW, 42L, 1L, AuditResult.FAILURE,
                    "롤백 예정", MDC.get(MdcFilter.TRACE_ID));
            throw new IllegalStateException("바깥 롤백");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(businessCount()).isEqualTo(1);
        assertThat(singleAudit().getAction()).isEqualTo(AuditAction.MEMBER_WITHDRAW);
        assertThat(jdbc.execute((ConnectionCallback<Boolean>) connection -> {
            try (var keys = connection.getMetaData().getImportedKeys(null, null, "AUDIT_LOGS")) {
                return keys.next();
            }
        })).isFalse();
    }

    @Test
    void successAuditAndStableNewestFirstPagination() {
        for (int i = 0; i < 22; i++) {
            auditLogService.record(AuditAction.GROUP_DELETE, 42L, (long) i, AuditResult.SUCCESS, null, "trace");
        }
        // 같은 시각도 id 내림차순으로 순서가 확정된다.
        jdbc.update("UPDATE audit_logs SET created_at = '2026-01-01 00:00:00'");
        Page<AuditLog> first = queryService.findPage(0);
        Page<AuditLog> second = queryService.findPage(1);
        assertThat(first.getTotalElements()).isEqualTo(22);
        assertThat(first.getContent()).hasSize(20);
        assertThat(second.getContent()).hasSize(2);
        assertThat(first.getContent().get(0).getTargetId()).isEqualTo(21L);
        assertThat(second.getContent().get(1).getTargetId()).isZero();
    }

    private int businessCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM audit_business", Integer.class);
    }

    private AuditLog singleAudit() {
        List<AuditLog> rows = auditLogRepository.findAll();
        assertThat(rows).hasSize(1);
        return rows.get(0);
    }

    public static class Business {
        private final JdbcTemplate jdbc;
        Business(JdbcTemplate jdbc) { this.jdbc = jdbc; }

        @Transactional
        public void delete(Long id, boolean fail, boolean commitFail) {
            jdbc.update("DELETE FROM audit_business WHERE id = ?", id);
            if (fail) throw new IllegalStateException("삭제 중 실패");
            if (commitFail) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void beforeCommit(boolean readOnly) {
                        throw new IllegalStateException("커밋 직전 실패");
                    }
                });
            }
        }
    }

    public static class Boundary {
        private final Business business;
        Boundary(Business business) { this.business = business; }

        @Audited(AuditAction.MAIMU_DELETE)
        public String delete(Long id, boolean fail, boolean commitFail) {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            business.delete(id, fail, commitFail);
            return "삭제 완료";
        }
    }

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = AuditLogRepository.class)
    static class Config {
        @Bean DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:audit;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan(AuditLog.class.getPackageName());
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            Properties properties = new Properties();
            properties.setProperty("hibernate.hbm2ddl.auto", "none");
            factory.setJpaProperties(properties);
            return factory;
        }
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory emf, DataSource dataSource) {
            var manager = new JpaTransactionManager(emf);
            manager.setDataSource(dataSource);
            return manager;
        }
        @Bean JdbcTemplate jdbcTemplate(DataSource dataSource) { return new JdbcTemplate(dataSource); }
        @Bean AuditLogService auditLogService(JdbcTemplate jdbc) { return new AuditLogService(jdbc); }
        @Bean AuditAspect auditAspect(AuditLogService service) { return new AuditAspect(service); }
        @Bean AuditLogQueryService queryService(AuditLogRepository repo) { return new AuditLogQueryService(repo); }
        @Bean Business business(JdbcTemplate jdbc) { return new Business(jdbc); }
        @Bean Boundary boundary(Business business) { return new Boundary(business); }
    }
}
