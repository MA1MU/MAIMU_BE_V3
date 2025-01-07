package com.example.chosim.chosim.domain.auth.component;

import com.example.chosim.chosim.domain.auth.entity.Member;
import com.example.chosim.chosim.domain.auth.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberReader {

    private final MemberRepository memberRepository;

    public Member findById(Long id){
        return memberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 멤버ID를 가진 회원을 찾을 수 없습니다."));
    }
}
