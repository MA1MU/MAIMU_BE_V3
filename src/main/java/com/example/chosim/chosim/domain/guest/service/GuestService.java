package com.example.chosim.chosim.domain.guest.service;

import com.example.chosim.chosim.api.guest.dto.GuestRequest;
import com.example.chosim.chosim.api.guest.dto.GuestResponse;
import com.example.chosim.chosim.common.invitation.Invitation;
import com.example.chosim.chosim.common.invitation.InvitationRepository;
import com.example.chosim.chosim.domain.group.entity.Group;
import com.example.chosim.chosim.domain.group.repository.GroupRepository;
import com.example.chosim.chosim.domain.maimu.entity.Maimu;
import com.example.chosim.chosim.domain.maimu.enums.MaimuColor;
import com.example.chosim.chosim.domain.maimu.repository.MaimuRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class GuestService {

    private final GroupRepository groupRepository;
    private final MaimuRepository maimuRepository;
    private final InvitationRepository invitationRepository;

    @Transactional
    public GuestResponse enterGroup(String token){
        Invitation invitation = verifyInvitation(token);
        Group group = groupRepository.findByIdWithLock(invitation.getGroupId())
                .orElseThrow(() -> new EntityNotFoundException("Group엔티티를 찾을 수 없음."));

        return new GuestResponse(group);
    }

    @Transactional
    public void addMaimu(String token, GuestRequest guestRequest){
        Invitation invitation = verifyInvitation(token);
        Group group = groupRepository.findByIdWithLock(invitation.getGroupId())
                .orElseThrow(() -> new EntityNotFoundException("Group엔티티를 찾을 수 없음."));

        group.addMaimu(Maimu.builder()
                .title(guestRequest.getTitle())
                .message(guestRequest.getMessage())
                .maimuColor(MaimuColor.fromString(guestRequest.getMaimuColor()))
                .isAnonymous(guestRequest.isAnonymous())
                .writerName(guestRequest.getWriterName())
                .group(group)
                .sugarContent(guestRequest.getSugarContent())
                .build());
    }

    public Invitation verifyInvitation(String token) {
        return invitationRepository.findById(token)
                .orElseThrow(() -> new RuntimeException("만료되었거나 존재하지 않는 초대 링크입니다."));
    }
}
