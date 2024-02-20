package com.example.chosim.chosim.service;

import com.example.chosim.chosim.domain.group.Group;
import com.example.chosim.chosim.domain.group.GroupEditor;
import com.example.chosim.chosim.dto.request.group.GroupCreate;
import com.example.chosim.chosim.dto.request.group.GroupEdit;
import com.example.chosim.chosim.dto.response.group.GroupResponse;
import com.example.chosim.chosim.exception.GroupNotFound;
import com.example.chosim.chosim.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;

    public void createGroup(GroupCreate groupCreate){
        Group group = Group.builder()
                .groupName(groupCreate.getGroupName())
                .groupColor(groupCreate.getGroupColor())
                .build();
        groupRepository.save(group);
    }

    public GroupResponse get(long id){
        Group group = groupRepository.findById(id)
                .orElseThrow(GroupNotFound::new);

        return GroupResponse.builder()
                .id(group.getId())
                .groupName(group.getGroupName())
                .groupColor(group.getGroupColor())
                .build();
    }

    @Transactional
    public void edit(Long id, GroupEdit groupEdit){
        Group group = groupRepository.findById(id)
                .orElseThrow(GroupNotFound::new);

        GroupEditor.GroupEditorBuilder groupEditorBuilder = group.toEditor();

        GroupEditor groupEditor = groupEditorBuilder.groupName(groupEdit.getGroupName())
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

