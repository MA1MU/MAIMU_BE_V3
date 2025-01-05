package com.example.chosim.chosim.domain.locker.service;

//import com.example.chosim.chosim.domain.Locker;
//import com.example.chosim.chosim.domain.auth.entity.UserEntity;
//import com.example.chosim.chosim.domain.auth.repository.UserRepository;
//import com.example.chosim.chosim.dto.request.locker.LockerCreate;
//import com.example.chosim.chosim.exception.LockerNotFound;
//import com.example.chosim.chosim.exception.UserEntityNotFound;
//import com.example.chosim.chosim.repository.LockerRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class LockerService {
//
//    private final LockerRepository lockerRepository;
//    private final UserRepository userRepository;
//
//    //User가 만들어질 때 호출해서 Locker만들기
//    //Locker field가 더 추가될 가능성 있어서 LockerCreate DTO 사용
////    public void save(String username, LockerCreate lockerCreate){
////        UserEntity userEntity = userRepository.findByUsername(username)
////                .orElseThrow(UserEntityNotFound::new);
////
////        Locker locker = Locker.builder()
////                .lockerName(lockerCreate.getLockerName())
////                .build();
////        locker.setUserEntity(userEntity);
////
////        lockerRepository.save(locker);
////    }
//
//    //사용할 일은 없을듯 하지만 일단 만듦, Locker삭제 기능
//    public void delete(Long id){
//        Locker locker = lockerRepository.findById(id)
//                .orElseThrow(LockerNotFound::new);
//        lockerRepository.delete(locker);
//    }
//
//}