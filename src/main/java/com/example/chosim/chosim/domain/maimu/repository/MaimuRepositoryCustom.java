package com.example.chosim.chosim.domain.maimu.repository;

import com.example.chosim.chosim.domain.maimu.entity.Maimu;
import com.example.chosim.chosim.api.maimu.dto.MaimuSearch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MaimuRepositoryCustom {
    Page<Maimu> findMaimusByGroupId(Long groupId, Pageable pageable);
}
