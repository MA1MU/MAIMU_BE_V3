package com.example.chosim.chosim.domain.auth.repository;

import com.example.chosim.chosim.domain.auth.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByProviderAndProviderId(String provider, String providerId);
    Optional<Member> findByProviderId(String providerId);
    Optional<Member> findByNickName(String nickName);
}
