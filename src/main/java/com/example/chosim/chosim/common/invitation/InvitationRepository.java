package com.example.chosim.chosim.common.invitation;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvitationRepository extends CrudRepository<Invitation, String> {
    Optional<Invitation> findByGroupId(Long groupId);
}
