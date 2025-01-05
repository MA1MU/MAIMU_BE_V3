package com.example.chosim.chosim.domain.maimu.repository;

import com.example.chosim.chosim.domain.maimu.entity.Maimu;
import com.example.chosim.chosim.api.maimu.dto.MaimuSearch;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class MaimuRepositoryImpl implements MaimuRepositoryCustom{

//    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<Maimu> getList(MaimuSearch maimuSearch) {
//        return jpaQueryFactory.selectFrom(maimu)
//                .limit(maimuSearch.getSize())
//                .offset(maimuSearch.getOffset())
//                .orderBy(maimu.id.desc())
//                .fetch();
        return null;
    }

}