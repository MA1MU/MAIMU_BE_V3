package com.example.chosim.chosim.api.admin.controller;

import com.example.chosim.chosim.common.audit.*;
import com.example.chosim.chosim.common.error.exception.AppException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminAuditControllerTest {
    @Test
    void returnsAuditPageWithExplicitMetadataAndDto() throws Exception {
        AuditLogQueryService query = mock(AuditLogQueryService.class);
        AuditLog entry = AuditLog.builder().action(AuditAction.MEMBER_WITHDRAW).actorMemberId(7L)
                .targetId(7L).result(AuditResult.FAILURE).errorMessage("실패").traceId("trace").build();
        when(query.findPage(1)).thenReturn(new PageImpl<>(List.of(entry), PageRequest.of(1, 20), 21));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AdminController(null, null, query)).build();
        mvc.perform(get("/v1/api/admin/audit-logs").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalElements").value(21))
                .andExpect(jsonPath("$.content[0].actorMemberId").value(7))
                .andExpect(jsonPath("$.content[0].result").value("FAILURE"))
                .andExpect(jsonPath("$.content[0].traceId").value("trace"));
    }

    @Test
    void negativePageIsRejectedBeforeQuery() {
        AuditLogRepository repo = mock(AuditLogRepository.class);
        assertThatThrownBy(() -> new AuditLogQueryService(repo).findPage(-1)).isInstanceOf(AppException.class);
        verifyNoInteractions(repo);
    }
}
