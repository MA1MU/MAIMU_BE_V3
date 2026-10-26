package com.example.chosim.chosim.common.audit;

import com.example.chosim.chosim.common.error.ErrorCode;
import com.example.chosim.chosim.common.error.exception.AppException;
import com.example.chosim.chosim.domain.auth.entity.Member;
import com.example.chosim.chosim.domain.auth.service.MemberService;
import com.example.chosim.chosim.domain.group.entity.Group;
import com.example.chosim.chosim.domain.group.repository.GroupRepository;
import com.example.chosim.chosim.domain.group.service.GroupService;
import com.example.chosim.chosim.domain.guest.service.GuestGroupCache;
import com.example.chosim.chosim.domain.maimu.entity.Maimu;
import com.example.chosim.chosim.domain.maimu.repository.MaimuRepository;
import com.example.chosim.chosim.domain.maimu.service.MaimuService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeleteOwnershipTest {
    private final GroupRepository groups = mock(GroupRepository.class);
    private final MaimuRepository maimus = mock(MaimuRepository.class);
    private final GuestGroupCache cache = mock(GuestGroupCache.class);
    private final Group group = mock(Group.class);

    @BeforeEach
    void actorAndOwner() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(7L, "", List.of()));
        Member owner = mock(Member.class);
        when(owner.getId()).thenReturn(8L);
        when(group.getMember()).thenReturn(owner);
    }

    @AfterEach
    void cleanUp() { SecurityContextHolder.clearContext(); }

    @Test
    void cannotDeleteAnotherMembersMaimu() {
        Maimu maimu = mock(Maimu.class);
        when(maimu.getGroup()).thenReturn(group);
        when(maimus.findById(1L)).thenReturn(Optional.of(maimu));
        assertThatThrownBy(() -> new MaimuService(maimus, groups, cache).deleteMaimu(1L))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DELETE_FORBIDDEN));
        verify(maimus, never()).delete(any(Maimu.class));
        verifyNoInteractions(cache);
    }

    @Test
    void cannotDeleteAnotherMembersGroup() {
        when(groups.findByIdWithLock(1L)).thenReturn(Optional.of(group));
        assertThatThrownBy(() -> new GroupService(null, groups, maimus, null, cache).deleteGroup(1L))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DELETE_FORBIDDEN));
        verify(groups, never()).delete(any(Group.class));
        verifyNoInteractions(cache);
    }

    @Test
    void cannotWithdrawAnotherMember() {
        MemberService service = new MemberService(null, groups, maimus, null, null, null);
        assertThatThrownBy(() -> service.deleteMember(8L))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DELETE_FORBIDDEN));
        verifyNoInteractions(groups, maimus);
    }
}
