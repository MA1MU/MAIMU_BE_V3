package com.example.chosim.chosim.domain.maimu.repository;

import com.example.chosim.chosim.domain.maimu.entity.Maimu;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MaimuRepository extends JpaRepository<Maimu, Long>, MaimuRepositoryCustom{

    @Query("SELECT COUNT(m) FROM Maimu m WHERE m.group.id = :groupId AND m.isRead = false")
    Long countByGroupIdAndIsReadFalse(@Param("groupId") Long groupId);

    @Modifying
    @Query("DELETE FROM Maimu m WHERE m.group.id = :groupId")
    void deleteByGroupId(@Param("groupId") Long groupId);

    @Query("SELECT m FROM Maimu m JOIN FETCH m.group g JOIN FETCH g.member WHERE m.isRead = false AND m.isNotified = false")
    List<Maimu> findAllIsNotifiedMaimusWithMember();

    @Transactional
    @Modifying(clearAutomatically = true) // 쿼리 실행 후 영속성 컨텍스트를 비워 데이터 정합성 유지
    @Query("UPDATE Maimu m SET m.isNotified = true WHERE m.id IN :ids")
    void updateNotifiedStatus(@Param("ids") List<Long> ids);
}
