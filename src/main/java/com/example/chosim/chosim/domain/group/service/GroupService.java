package com.example.chosim.chosim.domain.group.service;

import com.example.chosim.chosim.common.invitation.Invitation;
import com.example.chosim.chosim.common.invitation.InvitationRepository;
import com.example.chosim.chosim.domain.auth.entity.Member;
import com.example.chosim.chosim.domain.auth.repository.MemberRepository;
import com.example.chosim.chosim.domain.group.entity.Group;
import com.example.chosim.chosim.api.group.dto.GroupRequest;
import com.example.chosim.chosim.api.group.dto.GroupResponse;
import com.example.chosim.chosim.domain.group.repository.GroupRepository;
import com.example.chosim.chosim.domain.maimu.repository.MaimuRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupService {

    private final MemberRepository memberRepository;
    private final GroupRepository groupRepository;
    private final MaimuRepository maimuRepository;
    private final InvitationRepository invitationRepository;

    @Transactional
    public GroupResponse createGroup(Long memberId, GroupRequest groupRequest){
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Member엔티티를 찾을 수 없음."));

        Group group = Group.builder()
                .groupName(groupRequest.getGroupName())
                .groupColor(groupRequest.getGroupColor())
                .build();
        group.setMember(member);

        try{
            Group savedGroup = groupRepository.save(group);
            return GroupResponse.from(savedGroup);
        }catch (DataIntegrityViolationException e){
            throw new DataIntegrityViolationException("데이터 무결성 위반으로 저장 실패");
        }
    }

    public List<GroupResponse> getList(Long memberId) {
        return groupRepository.findGroupsWithUnreadCount(memberId);
    }

    public GroupResponse getGroup(Long groupId) {
        return groupRepository.findGroupWithUnreadCount(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group엔티티를 찾을 수 없음."));
    }


    @Transactional
    public GroupResponse editGroup(Long id, GroupRequest groupRequest){
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Group엔티티를 찾을 수 없음."));

        GroupEditor.GroupEditorBuilder groupEditorBuilder = group.toEditor();
        GroupEditor groupEditor = groupEditorBuilder
                .groupName(groupRequest.getGroupName())
                .groupColor(groupRequest.getGroupColor())
                .build();
        group.edit(groupEditor);

        Long unreadCount = maimuRepository.countByGroupIdAndIsReadFalse(id);
        return new GroupResponse(
                group.getId(),
                group.getGroupName(),
                group.getGroupColor(),
                unreadCount
        );
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void deleteGroup(Long id){
        Group group = groupRepository.findByIdWithLock(id)
                .orElseThrow(() -> new EntityNotFoundException("Group엔티티를 찾을 수 없음."));
        groupRepository.delete(group);
    }

    public String createInvitation(Long groupId, Long senderId) {
        String token = UUID.randomUUID().toString();
        Invitation invitation = Invitation.builder()
                .token(token)
                .groupId(groupId)
                .senderId(senderId)
                .build();

        invitationRepository.save(invitation);
        return token;
    }

}

