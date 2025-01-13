package com.example.chosim.chosim.domain.maimu.repository;

import com.example.chosim.chosim.domain.maimu.entity.Maimu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaimuRepository extends JpaRepository<Maimu, Long>, MaimuRepositoryCustom{
}
