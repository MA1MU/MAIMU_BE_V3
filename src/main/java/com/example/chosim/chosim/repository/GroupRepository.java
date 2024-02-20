package com.example.chosim.chosim.repository;

import com.example.chosim.chosim.domain.group.Group;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, Long> {
}
