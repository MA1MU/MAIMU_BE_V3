package com.example.chosim.chosim.service;

import com.example.chosim.chosim.domain.entity.UserEntity;
import com.example.chosim.chosim.domain.entity.repository.UserRepository;
import com.example.chosim.chosim.domain.group.Group;
import com.example.chosim.chosim.domain.group.GroupEditor;
import com.example.chosim.chosim.dto.request.GuestRequest;
import com.example.chosim.chosim.dto.request.group.GroupCreate;
import com.example.chosim.chosim.dto.request.group.GroupEdit;
import com.example.chosim.chosim.dto.response.group.GroupResponse;
import com.example.chosim.chosim.dto.response.group.GuestResponse;
import com.example.chosim.chosim.exception.GroupNotFound;
import com.example.chosim.chosim.exception.InvalidRequest;
import com.example.chosim.chosim.exception.UserEntityNotFound;
import com.example.chosim.chosim.repository.GroupRepository;
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

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    public void createGroup(String username, GroupCreate groupCreate){
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(UserEntityNotFound::new);

        Group group = Group.builder()
                .groupName(groupCreate.getGroupName())
                .groupColor(groupCreate.getGroupColor())
                .build();
        group.setUserEntity(userEntity);

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
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(UserEntityNotFound::new);

        return groupRepository.findByUserEntity_IdOrderByIdAsc(userEntity.getId()).stream()
                .map(GroupResponse::new)
                .collect(Collectors.toList());
    }

    public GuestResponse getForGuest(GuestRequest request){

        UserEntity userEntity = userRepository.findByUsername(request.getUsername())
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

