package com.example.chosim.chosim.domain.maimu.service;

import com.example.chosim.chosim.common.sse.SseEmitters;
import com.example.chosim.chosim.domain.auth.entity.Member;
import com.example.chosim.chosim.domain.group.entity.Group;
import com.example.chosim.chosim.domain.group.repository.GroupRepository;
import com.example.chosim.chosim.domain.guest.service.GuestGroupCache;
import com.example.chosim.chosim.domain.maimu.service.MaimuWriteBackBuffer.PendingMaimu;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// 살아있는 Redis/MySQL 없이 플러시의 분기만 검증한다.
@SuppressWarnings("unchecked")
class MaimuWriteBackBufferTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
    private final ListOperations<String, String> listOps = mock(ListOperations.class);
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
    private final GroupRepository groupRepository = mock(GroupRepository.class);
    private final SseEmitters sseEmitters = mock(SseEmitters.class);
    private final GuestGroupCache guestGroupCache = mock(GuestGroupCache.class);

    private final MaimuWriteBackBuffer buffer = new MaimuWriteBackBuffer(
            redisTemplate, objectMapper, jdbcTemplate, transactionTemplate, groupRepository, sseEmitters, guestGroupCache);

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForList()).thenReturn(listOps);
        doAnswer(inv -> {
            ((Consumer<TransactionStatus>) inv.getArgument(0)).accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        Member owner = mock(Member.class);
        when(owner.getId()).thenReturn(7L);
        Group group = mock(Group.class);
        when(group.getId()).thenReturn(1L);
        when(group.getMember()).thenReturn(owner);
        when(group.getGroupName()).thenReturn("1학년 3반");
        when(group.getGroupColor()).thenReturn("핑크");
        when(groupRepository.findAllByIdInWithMember(anyList())).thenReturn(List.of(group));
    }

    @Test
    void 정상_배치는_한번에_넣고_버퍼를_잘라낸_뒤_주인에게_알린다() throws Exception {
        when(listOps.range(MaimuWriteBackBuffer.BUFFER_KEY, 0, 999))
                .thenReturn(List.of(json("a"), json("b"), json("c")));

        buffer.flush();

        verify(jdbcTemplate, times(1)).batchUpdate(anyString(), anyList());
        verify(listOps).trim(MaimuWriteBackBuffer.BUFFER_KEY, 3, -1);
        verify(guestGroupCache).evict(1L);
        verify(sseEmitters).send(eq(7L), eq("maimu"), argThat(m -> ((Map<?, ?>) m).get("count").equals(3L)));
    }

    @Test
    void 제약을_어기는_한건은_격리하고_나머지는_반영한다() throws Exception {
        String bad = json("bad");
        when(listOps.range(MaimuWriteBackBuffer.BUFFER_KEY, 0, 999)).thenReturn(List.of(json("a"), bad, json("c")));
        when(jdbcTemplate.batchUpdate(anyString(), anyList())).thenAnswer(inv -> {
            List<Object[]> rows = inv.getArgument(1);
            if (rows.stream().anyMatch(r -> "bad".equals(r[1]))) {
                throw new DataIntegrityViolationException("Data too long");
            }
            return new int[rows.size()];
        });

        buffer.flush();

        verify(listOps).rightPush(MaimuWriteBackBuffer.DEAD_LETTER_KEY, bad);
        verify(listOps, times(3)).leftPop(MaimuWriteBackBuffer.BUFFER_KEY); // 처리한 3건 모두 버퍼에서 빠진다
        verify(listOps, never()).trim(anyString(), anyLong(), anyLong());
        verify(sseEmitters).send(eq(7L), eq("maimu"), argThat(m -> ((Map<?, ?>) m).get("count").equals(2L)));
    }

    private String json(String title) throws Exception {
        return objectMapper.writeValueAsString(new PendingMaimu(
                1L, title, "내용", "RED", "익명", true, 70, LocalDateTime.of(2026, 3, 2, 9, 0)));
    }
}
