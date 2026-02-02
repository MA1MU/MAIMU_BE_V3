package com.example.chosim.chosim.domain.maimu.entity;

import com.example.chosim.chosim.common.entity.BaseTimeEntity;
import com.example.chosim.chosim.domain.group.entity.Group;
import com.example.chosim.chosim.domain.maimu.enums.MaimuColor;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "maimus")
public class Maimu extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maimu_id")
    private Long id;

    @Column(name = "maimu_title", nullable = false)
    private String title;

    @Lob
    @Column(name = "maimu_message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "maimu_color", nullable = false)
    @Enumerated(EnumType.STRING)
    private MaimuColor maimuColor;

    @Column(name = "maimu_writer_name", nullable = false)
    private String writerName;

    @Column(name = "is_anonymous", nullable = false)
    private boolean isAnonymous;

    @Column(name = "maimu_sugar_content", nullable = false)
    private Integer sugarContent;

    @Column(name = "is_favorite", nullable = false)
    private boolean isFavorite = false;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "is_notified", nullable = false)
    private boolean isNotified = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;


    @Builder
    public Maimu(String title, String message, MaimuColor maimuColor, String writerName,
                 Group group, Integer sugarContent, boolean isAnonymous) {
        this.title = title;
        this.message = message;
        this.maimuColor = maimuColor;
        this.isAnonymous = isAnonymous;
        this.writerName = isAnonymous ? "익명" : writerName;
        this.group = group;
        this.sugarContent = sugarContent;
        this.isFavorite = false;
        this.isNotified = false;
        this.isRead = false;
    }

    public void updateGroup(Group group){
        this.group = group;
    }

    public void toggleFavorite() {
        this.isFavorite = !this.isFavorite;
    }

    public void markAsRead() {
        this.isRead = true;
    }


}
