package com.example.chosim.chosim.domain;

import com.example.chosim.chosim.domain.group.Group;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class Maimu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maimu_id")
    private Long id;

    private String title;

    @Lob
    private String message;

    private String maimuColor;

    private String writerName;

    private Integer sugarContent;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;


    @Builder
    public Maimu(String title, String message, String maimuColor, String writerName, Group group, Integer sugarContent){
        this.title = title;
        this.message = message;
        this.maimuColor = maimuColor;
        this.writerName = writerName;
        this.group = group;
        this.sugarContent = sugarContent;
    }

    public void updateGroup(Group group){
        this.group = group;
    }


}
