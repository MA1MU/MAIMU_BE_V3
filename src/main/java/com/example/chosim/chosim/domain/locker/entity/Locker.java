package com.example.chosim.chosim.domain.locker.entity;

//@Entity
//@Getter
//@NoArgsConstructor(access = AccessLevel.PUBLIC)
//public class Locker {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "locker_id")
//    private Long id;
//
//    private String lockerName;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "user_id")
//    private UserEntity userEntity;
//
//    @OneToMany(cascade = CascadeType.ALL, mappedBy = "locker")
//    private List<Group> groups = new ArrayList<>();
//
//    @Builder
//    public Locker(String lockerName, UserEntity userEntity){
//        this.lockerName = lockerName;
//        this.userEntity = userEntity;
//    }
//
////    public void setUserEntity(UserEntity userEntity){
////        this.userEntity = userEntity;
////        userEntity.getLockers().add(this);
////    }
//
//}