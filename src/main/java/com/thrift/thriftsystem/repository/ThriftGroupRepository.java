package com.thrift.thriftsystem.repository;

import com.thrift.thriftsystem.enums.GroupStatus;
import com.thrift.thriftsystem.model.ThriftGroup;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ThriftGroupRepository extends MongoRepository<ThriftGroup, String> {

    List<ThriftGroup> findByAdminId(String adminId);

    List<ThriftGroup> findByStatus(GroupStatus status);

    List<ThriftGroup> findByMemberIdsContains(String memberId);

    Optional<ThriftGroup> findByIdAndAdminId(String id, String adminId);

    boolean existsByNameAndAdminId(String nameA, String adminId);
}
