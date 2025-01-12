package com.example.chosim.chosim.domain.auth.entity;

import com.example.chosim.chosim.common.entity.BaseTimeEntity;
import com.example.chosim.chosim.domain.auth.enums.MemberRole;
import com.example.chosim.chosim.domain.group.entity.Group;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@DynamicInsert
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "members", uniqueConstraints = {
        @UniqueConstraint(name = "MEMBER_SOCIAL_LOGIN_UNIQUE", columnNames = "member_unique_id"),
        @UniqueConstraint(name = "MEMBER_NICKNAME_UNIQUE", columnNames = "member_nickname")
})
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="member_id")
    private Long id;

    //social login 식별자
    //provider + SNS 사용자 닉네임
//    @Column(name = "member_unique_id", nullable = false)
//    private String uniqueId;

    //어떤 Social Login 인지 ex)google, naver, kakao
    @Column(name = "member_provider", nullable = false)
    private String provider;

    //Social login 시 Resource Server에서 부여하는 식별 Id
    @Column(name= "member_provider_id", nullable = false)
    private String providerId;

    @Column(name = "member_name", nullable = false)
    private String name;

    //SNS 이메일
    @Column(name = "member_email", nullable = false)
    private String email;

    @Column(name = "member_role", nullable = false)
    @Enumerated(EnumType.STRING)
    @ColumnDefault(value = "'PREMEMBER'")
    private MemberRole role;

    @Column(name = "member_maimu_profile")
    private String maimuProfile;

    @Column(name = "member_birth")
    private LocalDate birth;

    @Column(name = "member_nickname")
    private String nickName;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "userEntity")
    private List<Group> groups = new ArrayList<>();

    public void updateRole(MemberRole role) {
        this.role = role;
    }

    public void updateMaimuInfo(String maimuProfile, LocalDate birth, String nickName){
        this.maimuProfile = maimuProfile;
        this.birth = birth;
        this.nickName = nickName;
    }

    @Builder
    public Member(String provider, String providerId, String name, String email, MemberRole role,
                  String maimuProfile, LocalDate birth, String nickName) {
        this.provider = provider;
        this.providerId = providerId;
        this.name = name;
        this.email = email;
        this.role = role != null ? role : MemberRole.PREMEMBER;
        this.maimuProfile = maimuProfile;
        this.birth = birth;
        this.nickName = nickName;
    }
}
