package com.example.chosim.chosim.domain.guest.service;

import com.example.chosim.chosim.api.guest.dto.GuestRequest;
import com.example.chosim.chosim.domain.group.entity.Group;
import com.example.chosim.chosim.domain.group.repository.GroupRepository;
import com.example.chosim.chosim.domain.maimu.entity.Maimu;
import com.example.chosim.chosim.domain.maimu.repository.MaimuRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class GuestService {

//    private final GroupRepository groupRepository;
//    private final MaimuRepository maimuRepository;
//
//    @Transactional
//    public void writeMaimu(Long groupId, GuestRequest guestRequest){
//        Group group = groupRepository.findById(groupId)
//                .orElseThrow(() -> new EntityNotFoundException("Group엔티티를 찾을 수 없음"));
//
//        Maimu maimu = Maimu.builder()
//                .title(guestRequest.getTitle())
//                .message(guestRequest.getMessage())
//                .maimuColor(guestRequest.getMaimuColor())
//                .writerName(guestRequest.getWriterName())
//                .sugarContent(guestRequest.getSugarContent())
//                .build();
//        group.addMaimu(maimu);
//    }
}
