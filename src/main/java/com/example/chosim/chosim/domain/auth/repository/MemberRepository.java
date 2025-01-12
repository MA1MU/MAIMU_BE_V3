package com.example.chosim.chosim.domain.auth.repository;

import com.example.chosim.chosim.domain.auth.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    //대신 ProviderId로 뽑기
//    Optional<Member> findByUniqueId(String uniqueId);

    Optional<Member> findByProviderId(String providerId);
    Optional<Member> findByNickName(String nickName);
}
