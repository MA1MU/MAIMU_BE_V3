package com.example.chosim.chosim.domain.maimu.repository;

import com.example.chosim.chosim.domain.maimu.entity.Maimu;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.example.chosim.chosim.domain.maimu.entity.QMaimu.maimu;

@RequiredArgsConstructor
public class MaimuRepositoryImpl implements MaimuRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Maimu> findMaimusByGroupId(Long groupId, Pageable pageable) {

        // 전체 카운트 쿼리
        long total = queryFactory
                .selectFrom(maimu)
                .where(maimu.group.id.eq(groupId))
                .fetchCount();

        // 데이터 조회 쿼리
        List<Maimu> maimus = queryFactory
                .selectFrom(maimu)
                .where(maimu.group.id.eq(groupId))
                .orderBy(
                        maimu.isFavorite.desc(), // 즐겨찾기 먼저
                        new CaseBuilder()
                                .when(maimu.isFavorite.isTrue())
                                .then(maimu.id) // 즐겨찾기인 경우 ID 기준 내림차순
                                .otherwise(0L)
                                .desc(),
                        maimu.id.desc() // 나머지는 최신순
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(maimus, pageable, total);
    }

}