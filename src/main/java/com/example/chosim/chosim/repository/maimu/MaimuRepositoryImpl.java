package com.example.chosim.chosim.repository.maimu;

import com.example.chosim.chosim.domain.Maimu;
import com.example.chosim.chosim.dto.request.maimu.MaimuSearch;
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