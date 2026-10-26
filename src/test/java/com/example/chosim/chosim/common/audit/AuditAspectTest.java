package com.example.chosim.chosim.common.audit;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.chosim.chosim.common.logging.MdcFilter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditAspectTest {
    private final AuditLogService writer = mock(AuditLogService.class);
    private final AuditAspect aspect = new AuditAspect(writer);
    private final ProceedingJoinPoint invocation = mock(ProceedingJoinPoint.class);
    private final Audited annotation = mock(Audited.class);
    private final Logger logger = (Logger) LoggerFactory.getLogger(AuditAspect.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void setUp() {
        when(annotation.value()).thenReturn(AuditAction.GROUP_DELETE);
        when(invocation.getArgs()).thenReturn(new Object[]{9L});
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(7L, "", List.of()));
        MDC.put(MdcFilter.TRACE_ID, "trace-123");
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void cleanUp() {
        logger.detachAppender(appender);
        appender.stop();
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void auditCommitFailureKeepsSuccessfulResponseAndOnlyWarns() throws Throwable {
        Object response = new Object();
        when(invocation.proceed()).thenReturn(response);
        doThrow(new IllegalStateException("저장 커밋 실패"))
                .when(writer).record(AuditAction.GROUP_DELETE, 7L, 9L, AuditResult.SUCCESS, null, "trace-123");
        assertThat(aspect.audit(invocation, annotation)).isSameAs(response);
        assertThat(appender.list).hasSize(1).allSatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getFormattedMessage()).contains("감사 로그 저장 실패", "trace-123")
                    .doesNotContain("저장 커밋 실패");
        });
    }

    @Test
    void failureAuditUsesOriginalActorTargetAndTrace() throws Throwable {
        RuntimeException failure = new IllegalArgumentException("삭제 실패");
        when(invocation.proceed()).thenThrow(failure);
        assertThatThrownBy(() -> aspect.audit(invocation, annotation)).isSameAs(failure);
        verify(writer).record(AuditAction.GROUP_DELETE, 7L, 9L, AuditResult.FAILURE,
                "IllegalArgumentException: 삭제 실패", "trace-123");
    }

    @Test
    void limitsMessagesWithoutSplittingUnicodeOrLeavingControlCharacters() {
        AuditLog log = AuditLog.builder().errorMessage("\n" + "😀".repeat(600))
                .traceId("t".repeat(100)).build();
        assertThat(log.getErrorMessage().codePointCount(0, log.getErrorMessage().length())).isEqualTo(500);
        assertThat(log.getErrorMessage()).doesNotContain("\n");
        assertThat(log.getTraceId()).hasSize(64);
    }
}
