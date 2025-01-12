package com.example.chosim.chosim.domain.group.entity;

import com.example.chosim.chosim.domain.group.service.GroupEditor;
import com.example.chosim.chosim.domain.maimu.entity.Maimu;
import com.example.chosim.chosim.domain.auth.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minidev.json.annotate.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name ="groups", uniqueConstraints = {@UniqueConstraint( name = "NAME_UNIQUE", columnNames = {"member_id", "group_name"} )})
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long id;

    @Column(name = "group_name", nullable = false)
    private String groupName;

    @Column(name = "group_color", nullable = false)
    private String groupColor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "group")
    @JsonIgnore
    private List<Maimu> maimus = new ArrayList<>();

    @Builder
    public Group(String groupName, String groupColor, Member member){
        this.groupName = groupName;
        this.groupColor = groupColor;
        this.member = member;
    }

    public GroupEditor.GroupEditorBuilder toEditor(){
        return GroupEditor.builder()
                .groupName(groupName)
                .groupColor(groupColor);
    }

    public void edit (GroupEditor groupEditor){
        this.groupName = groupEditor.getGroupName();
        this.groupColor = groupEditor.getGroupColor();
    }

    public void addMaimu(Maimu maimu){
        maimu.updateGroup(this);
        this.maimus.add(maimu);
    }

    public void setMember(Member member){
        this.member = member;
        member.getGroups().add(this);
    }

}
