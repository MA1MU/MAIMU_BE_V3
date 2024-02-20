package com.example.chosim.chosim.repository.maimu;

import com.example.chosim.chosim.domain.Maimu;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaimuRepository extends JpaRepository<Maimu, Long>, MaimuRepositoryCustom{
}
