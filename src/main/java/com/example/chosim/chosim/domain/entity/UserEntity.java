package com.example.chosim.chosim.domain.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="user_id")
    private Long id;

    //social login 식별자
    private String username;

    private String name;

    private String email;

    @ColumnDefault("ROLE_GUEST")
    private String role;

    private String maimuProfile;

    private LocalDate birth;

    private String maimuName;

    public void authUser(String role){
        this.role = "ROLE_USER";
    }

    public void initMaimuInfo(String maimuProfile, LocalDate birth, String maimuName){
        this.maimuProfile = maimuProfile;
        this.birth = birth;
        this.maimuName = maimuName;
    }

}
