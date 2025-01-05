package com.example.chosim.chosim.domain.group.repository;

import com.example.chosim.chosim.domain.group.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupRepository extends JpaRepository<Group, Long> {
    List<Group> findByUserEntity_IdOrderByIdAsc(Long id);
}
