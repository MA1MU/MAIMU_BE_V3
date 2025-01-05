package com.example.chosim.chosim.domain.maimu.service;

import com.example.chosim.chosim.domain.maimu.entity.Maimu;
import com.example.chosim.chosim.domain.group.entity.Group;
import com.example.chosim.chosim.api.maimu.dto.MaimuCreate;
import com.example.chosim.chosim.api.maimu.dto.MaimuResponse;
import com.example.chosim.chosim.exception.GroupNotFound;
import com.example.chosim.chosim.exception.MaimuNotFound;
import com.example.chosim.chosim.domain.group.repository.GroupRepository;
import com.example.chosim.chosim.domain.maimu.repository.MaimuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class MaimuService {

    private final MaimuRepository maimuRepository;
    private final GroupRepository groupRepository;

    @Transactional
    public void writeMaimu(Long groupId, MaimuCreate maimuCreate){
        Group group = groupRepository.findById(groupId)
                .orElseThrow(GroupNotFound::new);

        Maimu maimu = Maimu.builder()
                .title(maimuCreate.getTitle())
                .message(maimuCreate.getMessage())
                .maimuColor(maimuCreate.getMaimuColor())
                .writerName(maimuCreate.getWriterName())
                .sugarContent(maimuCreate.getSugarContent())
                .build();
        group.addMaimu(maimu);
    }

    public MaimuResponse get(Long id){
        Maimu maimu = maimuRepository.findById(id)
                .orElseThrow(MaimuNotFound::new);

        return MaimuResponse.builder()
                .id(maimu.getId())
                .title(maimu.getTitle())
                .message(maimu.getMessage())
                .maimuColor(maimu.getMaimuColor())
                .writerName(maimu.getWriterName())
                .sugarContent(maimu.getSugarContent())
                .build();
    }

    public List<MaimuResponse> getList(Long id){
        return maimuRepository.findByGroup_IdOrderByIdAsc(id).stream()
                .map(MaimuResponse::new)
                .collect(Collectors.toList());
    }


    public void deleteMaimu(Long maimuId){
        Maimu maimu = maimuRepository.findById(maimuId)
                .orElseThrow(MaimuNotFound::new);

        maimuRepository.delete(maimu);
    }


}
