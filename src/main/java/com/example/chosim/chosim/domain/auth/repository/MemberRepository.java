package com.example.chosim.chosim.domain.auth.repository;

import com.example.chosim.chosim.domain.auth.entity.Member;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByProviderAndProviderId(String provider, String providerId);
    Optional<Member> findByProviderId(String providerId);
    Optional<Member> findByNickName(String nickName);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :id")
    Member findByIdWithLock(@Param("id") Long id);
}
