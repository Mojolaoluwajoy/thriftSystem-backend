package com.thrift.thriftsystem.repository;

import com.thrift.thriftsystem.enums.ContributionStatus;
import com.thrift.thriftsystem.model.Contribution;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContributionRepository extends MongoRepository<Contribution, String> {

    List<Contribution> findByUserIdAndGroupId(String userId, String groupId);

    List<Contribution> findByUserId(String userId);

    List<Contribution> findByGroupId(String groupId);

    List<Contribution> findByGroupIdAndCycleNumber(String groupId, Integer cycleNumber);

    long countByGroupIdAndCycleNumberAndStatus(
            String groupId, Integer cycleNumber, ContributionStatus status);

    List<Contribution> findByStatusAndDueDateBetween(
            ContributionStatus status, LocalDateTime from, LocalDateTime to);

    Optional<Contribution> findByPaystackReference(String paystackReference);

    List<Contribution> findByStatusAndDueDateBefore(
            ContributionStatus status, LocalDateTime dueDate);

    boolean existsByUserIdAndGroupIdAndCycleNumber(String userId, String groupId, int cycleNumber);
}
