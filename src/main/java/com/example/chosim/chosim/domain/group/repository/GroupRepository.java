package com.example.chosim.chosim.domain.group.repository;

import com.example.chosim.chosim.api.group.dto.GroupResponse;
import com.example.chosim.chosim.domain.group.entity.Group;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {
    @Query("SELECT new com.example.chosim.chosim.api.group.dto.GroupResponse(" +
            "g.id, g.groupName, g.groupColor, " +
            "COUNT(CASE WHEN m.isRead = false THEN 1 END)) " +
            "FROM Group g " +
            "LEFT JOIN g.maimus m " +
            "WHERE g.member.id = :memberId " +
            "GROUP BY g.id, g.groupName, g.groupColor " +
            "ORDER BY g.id ASC")
    List<GroupResponse> findGroupsWithUnreadCount(@Param("memberId") Long memberId);

    @Query("SELECT new com.example.chosim.chosim.api.group.dto.GroupResponse(" +
            "g.id, g.groupName, g.groupColor, " +
            "COUNT(CASE WHEN m.isRead = false THEN 1 END)) " +
            "FROM Group g " +
            "LEFT JOIN g.maimus m " +
            "WHERE g.id = :groupId " +
            "GROUP BY g.id, g.groupName, g.groupColor")
    Optional<GroupResponse> findGroupWithUnreadCount(@Param("groupId") Long groupId);

    @Modifying
    @Query("DELETE FROM Group g WHERE g.member.id = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Group g where g.member.id = :memberId")
    List<Group> findByMemberIdWithLock(@Param("memberId") Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM Group g WHERE g.id = :id")
    Optional<Group> findByIdWithLock(@Param("id") Long id);
}
