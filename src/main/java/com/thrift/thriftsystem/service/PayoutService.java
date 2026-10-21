package com.thrift.thriftsystem.service;

import com.thrift.thriftsystem.dto.response.PayoutResponse;
import com.thrift.thriftsystem.enums.ContributionStatus;
import com.thrift.thriftsystem.enums.PayoutStatus;
import com.thrift.thriftsystem.enums.Role;
import com.thrift.thriftsystem.exception.BadRequestException;
import com.thrift.thriftsystem.exception.ForbiddenException;
import com.thrift.thriftsystem.exception.ResourceNotFoundException;
import com.thrift.thriftsystem.model.Membership;
import com.thrift.thriftsystem.model.Payout;
import com.thrift.thriftsystem.model.ThriftGroup;
import com.thrift.thriftsystem.model.User;
import com.thrift.thriftsystem.repository.ContributionRepository;
import com.thrift.thriftsystem.repository.MembershipRepository;
import com.thrift.thriftsystem.repository.PayoutRepository;
import com.thrift.thriftsystem.repository.ThriftGroupRepository;
import com.thrift.thriftsystem.repository.UserRepository;
import com.thrift.thriftsystem.util.PayoutMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PayoutService {

    private final PayoutRepository payoutRepository;
    private final MembershipRepository membershipRepository;
    private final ThriftGroupRepository groupRepository;
    private final UserService userService;
    private final PayoutMapper payoutMapper;
    private final EmailService emailService;
    private final ContributionRepository contributionRepository;
    private final UserRepository userRepository;
    private final PaystackService paystackService;

    public PayoutResponse schedulePayout(String groupId, String userId, Integer cycleNumber) {
        ThriftGroup group =
                groupRepository
                        .findById(groupId)
                        .orElseThrow(() -> new ResourceNotFoundException("group not found"));

        Membership membership =
                membershipRepository
                        .findByUserIdAndGroupId(userId, groupId)
                        .orElseThrow(() -> new ResourceNotFoundException("membership not found"));

        if (payoutRepository.existsByUserIdAndGroupIdAndCycleNumber(userId, groupId, cycleNumber)) {
            throw new BadRequestException("Payout already scheduled for this cycle");
        }
        // The recipient collects every member's contribution for the cycle
        long memberCount = membershipRepository.countByGroupId(groupId);
        BigDecimal potAmount =
                group.getContributionAmount().multiply(BigDecimal.valueOf(memberCount));

        Payout payout =
                payoutMapper.toModel(
                        userId,
                        groupId,
                        membership.getId(),
                        potAmount,
                        group.getCurrency(),
                        cycleNumber,
                        LocalDateTime.now().plusDays(1));
        payoutRepository.save(payout);
        log.info("Payout scheduled for user: {} in group: {}", userId, groupId);
        return payoutMapper.toResponse(payout);
    }

    public List<PayoutResponse> getMyPayouts() {
        User currentUser = userService.getCurrentUser();
        return payoutRepository.findByUserId(currentUser.getId()).stream()
                .map(payoutMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<PayoutResponse> getGroupPayouts(String groupId) {
        return payoutRepository.findByGroupId(groupId).stream()
                .map(payoutMapper::toResponse)
                .collect(Collectors.toList());
    }

    public PayoutResponse processPayout(String payoutId) {
        Payout payout =
                payoutRepository
                        .findById(payoutId)
                        .orElseThrow(() -> new ResourceNotFoundException("Payout not found"));

        PayoutStatus previousStatus = payout.getStatus();
        boolean retryingFailedTransfer =
                previousStatus == PayoutStatus.FAILED || previousStatus == PayoutStatus.REVERSED;
        if (previousStatus != PayoutStatus.PENDING && !retryingFailedTransfer) {
            throw new BadRequestException("Payout is not in a payable state");
        }

        ThriftGroup group =
                groupRepository
                        .findById(payout.getGroupId())
                        .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        User currentUser = userService.getCurrentUser();
        if (!currentUser.getId().equals(group.getAdminId())
                && !currentUser.getRoles().contains(Role.ROLE_SUPER_ADMIN)) {
            throw new ForbiddenException("Only the group admin can process payouts");
        }

        if (!"NGN".equalsIgnoreCase(payout.getCurrency())) {
            throw new BadRequestException("Only NGN payouts are supported");
        }

        long memberCount = membershipRepository.countByGroupId(payout.getGroupId());
        long paidCount =
                contributionRepository.countByGroupIdAndCycleNumberAndStatus(
                        payout.getGroupId(),
                        payout.getCycleNumber(),
                        ContributionStatus.SUCCESSFUL);
        if (paidCount < memberCount) {
            throw new BadRequestException(
                    "Not all members have paid for cycle "
                            + payout.getCycleNumber()
                            + " ("
                            + paidCount
                            + "/"
                            + memberCount
                            + ")");
        }

        User recipient =
                userRepository
                        .findById(payout.getUserId())
                        .orElseThrow(() -> new ResourceNotFoundException("Recipient not found"));
        if (recipient.getPaystackRecipientCode() == null) {
            throw new BadRequestException("Recipient has not added a bank account");
        }

        // Mark as processing before calling Paystack so it can't be processed again meanwhile
        payout.setStatus(PayoutStatus.PROCESSING);
        payout.setPayStackRecipientCode(recipient.getPaystackRecipientCode());
        // Keep the same reference when retrying an unconfirmed attempt so Paystack
        // rejects duplicates; a confirmed failure needs a fresh reference
        if (payout.getTransferReference() == null || retryingFailedTransfer) {
            payout.setTransferReference(
                    "payout-"
                            + payout.getId()
                            + (retryingFailedTransfer ? "-" + System.currentTimeMillis() : ""));
        }
        payout.setFailureReason(null);
        payoutRepository.save(payout);

        try {
            Map<String, Object> response =
                    paystackService.initiateTransfer(
                            recipient.getPaystackRecipientCode(),
                            payout.getAmount(),
                            group.getName() + " payout - cycle " + payout.getCycleNumber(),
                            payout.getTransferReference());

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data != null) {
                payout.setPayStackTransferCode((String) data.get("transfer_code"));
                if ("otp".equals(data.get("status"))) {
                    log.warn(
                            "Transfer {} is waiting for OTP; disable transfer OTP in Paystack",
                            payout.getTransferReference());
                }
            }
        } catch (BadRequestException e) {
            // The transfer may or may not have reached Paystack; keep the reference
            // so a retry is deduplicated, and still accept a late webhook for it
            payout.setStatus(PayoutStatus.PENDING);
            payoutRepository.save(payout);
            throw e;
        }

        payoutRepository.save(payout);
        log.info("Payout transfer initiated: {}", payoutId);
        return payoutMapper.toResponse(payout);
    }

    public void handleTransferEvent(String event, String reference, String reason) {
        Payout payout =
                payoutRepository
                        .findByTransferReference(reference)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Payout not found for reference: " + reference));

        // Paystack can deliver the same event more than once
        if (payout.getStatus() != PayoutStatus.PROCESSING
                && payout.getStatus() != PayoutStatus.PENDING) {
            log.info(
                    "Ignoring {} for payout {} in state {}",
                    event,
                    payout.getId(),
                    payout.getStatus());
            return;
        }

        switch (event) {
            case "transfer.success" -> {
                payout.setStatus(PayoutStatus.SUCCESSFUL);
                payout.setProcessedAt(LocalDateTime.now());
                payoutRepository.save(payout);

                membershipRepository
                        .findById(payout.getMembershipId())
                        .ifPresent(
                                membership -> {
                                    membership.setHasCollected(true);
                                    membership.setCollectionDate(LocalDateTime.now());
                                    membershipRepository.save(membership);
                                });

                User user = userRepository.findById(payout.getUserId()).orElse(null);
                ThriftGroup group = groupRepository.findById(payout.getGroupId()).orElse(null);
                if (user != null && group != null) {
                    emailService.sendPayoutNotification(
                            user, group, payout.getAmount() + " " + payout.getCurrency());
                }
                log.info("Payout successful: {}", payout.getId());
            }
            case "transfer.failed", "transfer.reversed" -> {
                payout.setStatus(
                        "transfer.failed".equals(event)
                                ? PayoutStatus.FAILED
                                : PayoutStatus.REVERSED);
                payout.setFailureReason(reason);
                payoutRepository.save(payout);
                log.error("Payout {} {}: {}", payout.getId(), payout.getStatus(), reason);
            }
            default -> log.info("Unhandled transfer event: {}", event);
        }
    }
}
