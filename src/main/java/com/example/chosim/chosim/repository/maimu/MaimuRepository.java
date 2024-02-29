package com.example.chosim.chosim.repository.maimu;

import com.example.chosim.chosim.domain.Maimu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaimuRepository extends JpaRepository<Maimu, Long>, MaimuRepositoryCustom{
    List<Maimu> findByGroup_Id(Long id);
}
