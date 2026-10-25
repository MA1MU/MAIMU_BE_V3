package com.example.chosim.chosim.domain.guest.service;

import com.example.chosim.chosim.api.guest.dto.GuestRequest;
import com.example.chosim.chosim.api.guest.dto.GuestResponse;
import com.example.chosim.chosim.common.invitation.Invitation;
import com.example.chosim.chosim.common.invitation.InvitationRepository;
import com.example.chosim.chosim.domain.group.entity.Group;
import com.example.chosim.chosim.domain.group.repository.GroupRepository;
import com.example.chosim.chosim.domain.maimu.repository.MaimuRepository;
import com.example.chosim.chosim.domain.maimu.service.MaimuWriteBackBuffer;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
// 클래스 트랜잭션을 두지 않는다: 캐시 히트·버퍼 적재 경로에서 DB 커넥션을 잡지 않기 위해서다.
public class GuestService {

    private final GroupRepository groupRepository;
    private final MaimuRepository maimuRepository;
    private final InvitationRepository invitationRepository;
    private final GuestGroupCache guestGroupCache;
    private final MaimuWriteBackBuffer maimuWriteBackBuffer;

    // 링크를 연 게스트 수백 명이 같은 사물함을 본다 → Redis cache-aside. 미스일 때만 DB(잠금 없이 COUNT 한 번).
    public GuestResponse enterGroup(String token){
        Invitation invitation = verifyInvitation(token);
        return guestGroupCache.get(invitation.getGroupId(), () -> loadGroupSummary(invitation.getGroupId()));
    }

    private GuestResponse loadGroupSummary(Long groupId){
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group엔티티를 찾을 수 없음."));

        return GuestResponse.builder()
                .id(group.getId())
                .groupName(group.getGroupName())
                .groupColor(group.getGroupColor())
                .maimuCount(maimuRepository.countByGroupId(group.getId()).intValue())
                .build();
    }

    // Write-back: DB 트랜잭션 없이 Redis 버퍼에만 넣고 응답한다. DB 반영은 MaimuWriteBackBuffer 가 묶어서 한다.
    public void addMaimu(String token, GuestRequest guestRequest){
        Invitation invitation = verifyInvitation(token);
        maimuWriteBackBuffer.push(invitation.getGroupId(), guestRequest);
    }

    public Invitation verifyInvitation(String token) {
        return invitationRepository.findById(token)
                .orElseThrow(() -> new RuntimeException("만료되었거나 존재하지 않는 초대 링크입니다."));
    }
}
