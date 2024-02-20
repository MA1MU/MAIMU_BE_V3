package com.example.chosim.chosim.repository.maimu;

import com.example.chosim.chosim.domain.Maimu;
import com.example.chosim.chosim.dto.request.maimu.MaimuSearch;

import java.util.List;

public interface MaimuRepositoryCustom {

    List<Maimu> getList(MaimuSearch maimuSearch);
}
