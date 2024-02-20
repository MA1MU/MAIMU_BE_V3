package com.example.chosim.chosim.domain.entity.repository;

import com.example.chosim.chosim.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findBymaimuName(String maimuName);
}
