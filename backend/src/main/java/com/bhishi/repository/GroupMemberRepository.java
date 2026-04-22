package com.bhishi.repository;

import com.bhishi.model.GroupMember;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends MongoRepository<GroupMember, String> {
    List<GroupMember> findByGroupId(String groupId);
    List<GroupMember> findByGroupIdAndStatus(String groupId, GroupMember.MemberStatus status);
    Optional<GroupMember> findByGroupIdAndUserId(String groupId, String userId);
    List<GroupMember> findByUserId(String userId);
    boolean existsByGroupIdAndUserId(String groupId, String userId);
    long countByGroupIdAndStatus(String groupId, GroupMember.MemberStatus status);

    @Query("{ 'groupId': ?0, 'hasReceivedPayout': false, 'status': 'ACTIVE' }")
    List<GroupMember> findEligibleForPayout(String groupId);
}
