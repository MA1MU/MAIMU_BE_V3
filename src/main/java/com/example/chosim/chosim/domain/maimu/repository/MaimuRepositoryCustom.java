package com.example.chosim.chosim.domain.maimu.repository;

import com.example.chosim.chosim.domain.maimu.entity.Maimu;
import com.example.chosim.chosim.api.maimu.dto.MaimuSearch;

import java.util.List;

public interface MaimuRepositoryCustom {

    List<Maimu> getList(MaimuSearch maimuSearch);
}
