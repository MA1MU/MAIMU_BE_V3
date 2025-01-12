package com.example.chosim.chosim.domain.group.service;

import com.example.chosim.chosim.domain.auth.entity.Member;
import com.example.chosim.chosim.domain.auth.repository.MemberRepository;
import com.example.chosim.chosim.domain.group.entity.Group;
import com.example.chosim.chosim.api.member.dto.GuestRequest;
import com.example.chosim.chosim.api.group.dto.GroupRequest;
import com.example.chosim.chosim.api.group.dto.GroupResponse;
import com.example.chosim.chosim.api.group.dto.GuestResponse;
import com.example.chosim.chosim.domain.group.repository.GroupRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupService {

    private final MemberRepository memberRepository;
    private final GroupRepository groupRepository;
    private final EntityManager entityManger;

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
            return new GroupResponse(savedGroup);
        }catch (DataIntegrityViolationException e){
            throw new DataIntegrityViolationException("데이터 무결성 위반으로 저장 실패");
        }
    }

    public List<GroupResponse> getList(Long memberId){
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Member엔티티를 찾을 수 없음."));

        return groupRepository.findByMember_IdOrderByIdAsc(member.getId()).stream()
                .map(GroupResponse::new)
                .collect(Collectors.toList());
    }

    public GroupResponse getGroup(Long id){
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Group엔티티를 찾을 수 없음."));

        return GroupResponse.builder()
                .id(group.getId())
                .groupName(group.getGroupName())
                .groupColor(group.getGroupColor())
                .build();
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
        return new GroupResponse(group);
    }

    //TODO: Group List에서 지우는 방법 말고 다른 방법 생각해보기
    @Transactional
    public void deleteGroup(Long id){
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Group엔티티를 찾을 수 없음."));
        entityManger.refresh(group);
        groupRepository.delete(group);
    }
}

