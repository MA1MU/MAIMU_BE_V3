package com.example.chosim.chosim.domain.maimu.repository;

import com.example.chosim.chosim.domain.maimu.entity.Maimu;
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
}
