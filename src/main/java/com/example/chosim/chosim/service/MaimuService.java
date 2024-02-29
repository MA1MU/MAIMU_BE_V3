package com.example.chosim.chosim.service;

import com.example.chosim.chosim.domain.Maimu;
import com.example.chosim.chosim.domain.group.Group;
import com.example.chosim.chosim.dto.request.maimu.MaimuCreate;
import com.example.chosim.chosim.dto.request.maimu.MaimuDelete;
import com.example.chosim.chosim.dto.request.maimu.MaimuSearch;
import com.example.chosim.chosim.dto.response.maimu.MaimuResponse;
import com.example.chosim.chosim.dto.response.maimu.MaimuResponseDto;
import com.example.chosim.chosim.exception.GroupNotFound;
import com.example.chosim.chosim.exception.MaimuNotFound;
import com.example.chosim.chosim.repository.GroupRepository;
import com.example.chosim.chosim.repository.maimu.MaimuRepository;
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

    public List<MaimuResponseDto> getList(Long id){
        return maimuRepository.findByGroup_Id(id).stream()
                .map(MaimuResponseDto::new)
                .collect(Collectors.toList());
    }


    //    maimuDelete를 활용할 수 있는 방법 생각해봐야 할듯
    public void deleteMaimu(Long maimuId){
        Maimu maimu = maimuRepository.findById(maimuId)
                .orElseThrow(MaimuNotFound::new);

        maimuRepository.delete(maimu);
    }


}
