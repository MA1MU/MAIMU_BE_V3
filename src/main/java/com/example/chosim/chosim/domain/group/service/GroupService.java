package com.example.chosim.chosim.domain.group.service;

import com.example.chosim.chosim.domain.auth.entity.Member;
import com.example.chosim.chosim.domain.auth.repository.MemberRepository;
import com.example.chosim.chosim.domain.group.entity.Group;
import com.example.chosim.chosim.api.member.dto.GuestRequest;
import com.example.chosim.chosim.api.group.dto.GroupCreate;
import com.example.chosim.chosim.api.group.dto.GroupEdit;
import com.example.chosim.chosim.api.group.dto.GroupResponse;
import com.example.chosim.chosim.api.group.dto.GuestResponse;
import com.example.chosim.chosim.exception.GroupNotFound;
import com.example.chosim.chosim.exception.InvalidRequest;
import com.example.chosim.chosim.exception.UserEntityNotFound;
import com.example.chosim.chosim.domain.group.repository.GroupRepository;
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
public class GroupService {

    private final MemberRepository memberRepository;
    private final GroupRepository groupRepository;

    public void createGroup(String username, GroupCreate groupCreate){
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(UserEntityNotFound::new);

        Group group = Group.builder()
                .groupName(groupCreate.getGroupName())
                .groupColor(groupCreate.getGroupColor())
                .build();
        group.setMember(member);

        try{
            groupRepository.save(group);
        }catch (DataIntegrityViolationException e){
            throw new InvalidRequest();
        }
    }

    public GroupResponse get(Long id){
        Group group = groupRepository.findById(id)
                .orElseThrow(GroupNotFound::new);

        return GroupResponse.builder()
                .id(group.getId())
                .groupName(group.getGroupName())
                .groupColor(group.getGroupColor())
                .build();
    }

    public List<GroupResponse> getList(String username){
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(UserEntityNotFound::new);

        return groupRepository.findByUserEntity_IdOrderByIdAsc(member.getId()).stream()
                .map(GroupResponse::new)
                .collect(Collectors.toList());
    }

    public GuestResponse getForGuest(GuestRequest request){

        Member member = memberRepository.findByUsername(request.getUsername())
                .orElseThrow(UserEntityNotFound::new);

        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(GroupNotFound::new);

        return GuestResponse.builder()
                .id(group.getId())
                .groupName(group.getGroupName())
                .groupColor(group.getGroupColor())
                .maimuCount(group.getMaimus().size())
                .build();
    }


    @Transactional
    public void edit(Long id, GroupEdit groupEdit){
        Group group = groupRepository.findById(id)
                .orElseThrow(GroupNotFound::new);

        GroupEditor.GroupEditorBuilder groupEditorBuilder = group.toEditor();

        GroupEditor groupEditor = groupEditorBuilder
                .groupName(groupEdit.getGroupName())
                .groupColor(groupEdit.getGroupColor())
                .build();
        group.edit(groupEditor);
    }

    public void delete(Long id){
        Group group = groupRepository.findById(id)
                .orElseThrow(GroupNotFound::new);
        groupRepository.delete(group);
    }


}

