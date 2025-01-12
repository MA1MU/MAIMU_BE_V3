package com.example.chosim.chosim.domain.group.repository;

import com.example.chosim.chosim.domain.group.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroupRepository extends JpaRepository<Group, Long> {
    @Query("SELECT g FROM Group g JOIN FETCH g.member WHERE g.member.id = :id ORDER BY g.id ASC")
    List<Group> findByMember_IdOrderByIdAsc(@Param("id") Long id);
}
