package com.bhishi.repository;

import com.bhishi.model.Group;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends MongoRepository<Group, String> {
    Optional<Group> findByGroupCode(String groupCode);
    List<Group> findByAdminId(String adminId);
    List<Group> findByStatus(Group.GroupStatus status);
    boolean existsByGroupCode(String groupCode);
}
