package com.thrift.thriftsystem.repository;

import com.thrift.thriftsystem.enums.PayoutStatus;
import com.thrift.thriftsystem.model.Payout;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PayoutRepository extends MongoRepository<Payout, String> {

    List<Payout> findByGroupId(String groupId);

    List<Payout> findByUserId(String userId);

    List<Payout> findByUserIdAndGroupId(String userId, String groupId);

    Optional<Payout> findByUserIdAndGroupIdAndCycleNumber(
            String userId, String groupId, Integer cycleNumber);

    List<Payout> findByGroupIdAndStatus(String groupId, PayoutStatus status);

    List<Payout> findByStatus(PayoutStatus status);

    Optional<Payout> findByTransferReference(String transferReference);

    boolean existsByUserIdAndGroupIdAndCycleNumber(
            String userId, String groupId, Integer cycleNumber);
}
