package com.example.chosim.chosim.repository;

import com.example.chosim.chosim.domain.group.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupRepository extends JpaRepository<Group, Long> {
    List<Group> findByUserEntity_Username(String username);
}
