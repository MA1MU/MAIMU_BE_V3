package com.example.chosim.chosim.domain.maimu.service;

import com.example.chosim.chosim.api.maimu.dto.MaimuFavoriteResponse;
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

    public Page<MaimuResponse> getList(Long groupId, int page){
        Pageable pageable = PageRequest.of(page, 18);
        Page<Maimu> maimuPage = maimuRepository.findMaimusByGroupId(groupId, pageable);
        return maimuPage.map(MaimuResponse::new);
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
