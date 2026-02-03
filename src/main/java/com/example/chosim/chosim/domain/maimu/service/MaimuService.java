package com.example.chosim.chosim.domain.maimu.service;

import com.example.chosim.chosim.api.maimu.dto.MaimuFavoriteResponse;
import com.example.chosim.chosim.api.maimu.dto.PageMaimuResponse;
import com.example.chosim.chosim.domain.group.entity.Group;
import com.example.chosim.chosim.domain.maimu.entity.Maimu;
import com.example.chosim.chosim.api.maimu.dto.MaimuResponse;
import com.example.chosim.chosim.domain.group.repository.GroupRepository;
import com.example.chosim.chosim.domain.maimu.repository.MaimuRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaimuService {

    private final MaimuRepository maimuRepository;
    private final GroupRepository groupRepository;

    public PageMaimuResponse<MaimuResponse> getList(Long groupId, int page){
        Group group = groupRepository.findByIdWithMember(groupId)
                .orElseThrow(() -> new IllegalArgumentException("해당 그룹이 존재하지 않습니다."));
        String nickName = group.getMember().getNickName();

        Pageable pageable = PageRequest.of(page, 18);
        Page<Maimu> maimuPage = maimuRepository.findMaimusByGroupId(groupId, pageable);

        Page<MaimuResponse> maimuResponses = maimuPage.map(MaimuResponse::new);
        return new PageMaimuResponse<>(nickName, maimuResponses);
    }

    @Transactional
    public MaimuResponse getMaimu(Long maimuId){
        Maimu maimu = maimuRepository.findById(maimuId)
                .orElseThrow(() -> new EntityNotFoundException("Maimu엔티티를 찾을 수 없습니다."));
        maimu.markAsRead();
        return new MaimuResponse(maimu);
    }

    @Transactional
    public void deleteMaimu(Long maimuId){
        Maimu maimu = maimuRepository.findById(maimuId)
                .orElseThrow(() -> new EntityNotFoundException("Maimu엔티티를 찾을 수 없음"));
        maimuRepository.delete(maimu);
    }

    @Transactional
    public MaimuFavoriteResponse updateFavorite(Long maimuId){
        Maimu maimu = maimuRepository.findById(maimuId)
                .orElseThrow(() -> new EntityNotFoundException("Maimu엔티티를 찾을 수 없음"));
        maimu.toggleFavorite();

        maimuRepository.save(maimu);

        return new MaimuFavoriteResponse(maimu.isFavorite());
    }
    
}
