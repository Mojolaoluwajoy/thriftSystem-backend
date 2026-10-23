package com.thrift.thriftsystem.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.thrift.thriftsystem.dto.response.PayoutResponse;
import com.thrift.thriftsystem.enums.ContributionStatus;
import com.thrift.thriftsystem.enums.PayoutStatus;
import com.thrift.thriftsystem.enums.Role;
import com.thrift.thriftsystem.exception.BadRequestException;
import com.thrift.thriftsystem.exception.ForbiddenException;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PayoutServiceTest {

    @Mock private PayoutRepository payoutRepository;

    @Mock private MembershipRepository membershipRepository;

    @Mock private ThriftGroupRepository thriftGroupRepository;

    @Mock private UserService userService;

    @Mock private PayoutMapper payoutMapper;

    @Mock private EmailService emailService;

    @Mock private ContributionRepository contributionRepository;

    @Mock private UserRepository userRepository;

    @Mock private PaystackService paystackService;

    @Mock private CycleService cycleService;

    @InjectMocks private PayoutService payoutService;

    private User currentUser;
    private ThriftGroup group;
    private Membership membership;
    private Payout payout;
    private PayoutResponse payoutResponse;

    @BeforeEach
    void setUp() {
        currentUser =
                User.builder()
                        .id("user123")
                        .email("john@example.com")
                        .paystackRecipientCode("RCP_123")
                        .build();

        group =
                ThriftGroup.builder()
                        .id("group123")
                        .name("Office Ajo")
                        .adminId("user123")
                        .contributionAmount(new BigDecimal("10000"))
                        .currency("NGN")
                        .build();

        membership =
                Membership.builder()
                        .id("membership123")
                        .userId("user123")
                        .groupId("group123")
                        .build();

        payout =
                Payout.builder()
                        .id("payout123")
                        .userId("user123")
                        .groupId("group123")
                        .membershipId("membership123")
                        .amount(new BigDecimal("30000"))
                        .currency("NGN")
                        .status(PayoutStatus.PENDING)
                        .cycleNumber(1)
                        .build();

        payoutResponse =
                PayoutResponse.builder()
                        .id("payout123")
                        .userId("user123")
                        .groupId("group123")
                        .payoutStatus(PayoutStatus.PENDING)
                        .build();
    }

    @Test
    void schedulePayout_Success() {
        when(thriftGroupRepository.findById("group123")).thenReturn(Optional.of(group));
        when(membershipRepository.findByUserIdAndGroupId(any(), any()))
                .thenReturn(Optional.of(membership));
        when(payoutRepository.findByUserIdAndGroupId("user123", "group123")).thenReturn(List.of());
        when(payoutRepository.existsByGroupIdAndCycleNumber("group123", 1)).thenReturn(false);
        when(membershipRepository.countByGroupId("group123")).thenReturn(3L);
        when(payoutMapper.toModel(any(), any(), any(), any(), any(), anyInt(), any()))
                .thenReturn(payout);
        when(payoutRepository.save(any())).thenReturn(payout);
        when(payoutMapper.toResponse(any())).thenReturn(payoutResponse);

        PayoutResponse result = payoutService.schedulePayout("group123", "user123", 1);

        assertNotNull(result);
        assertEquals("payout123", result.getId());
        verify(payoutMapper)
                .toModel(
                        eq("user123"),
                        eq("group123"),
                        eq("membership123"),
                        eq(new BigDecimal("30000")),
                        eq("NGN"),
                        eq(1),
                        any());
        verify(payoutRepository, times(1)).save(any());
    }

    @Test
    void schedulePayout_CycleAlreadyHasPayout_ThrowsException() {
        when(thriftGroupRepository.findById(any())).thenReturn(Optional.of(group));
        when(membershipRepository.findByUserIdAndGroupId(any(), any()))
                .thenReturn(Optional.of(membership));
        when(payoutRepository.findByUserIdAndGroupId(any(), any())).thenReturn(List.of());
        when(payoutRepository.existsByGroupIdAndCycleNumber("group123", 1)).thenReturn(true);

        assertThrows(
                BadRequestException.class,
                () -> payoutService.schedulePayout("group123", "user123", 1));

        verify(payoutRepository, never()).save(any());
    }

    @Test
    void schedulePayout_MemberAlreadyHasPayout_ThrowsException() {
        when(thriftGroupRepository.findById(any())).thenReturn(Optional.of(group));
        when(membershipRepository.findByUserIdAndGroupId(any(), any()))
                .thenReturn(Optional.of(membership));
        when(payoutRepository.findByUserIdAndGroupId("user123", "group123"))
                .thenReturn(List.of(payout));

        assertThrows(
                BadRequestException.class,
                () -> payoutService.schedulePayout("group123", "user123", 2));

        verify(payoutRepository, never()).save(any());
    }

    @Test
    void schedulePayout_MemberAlreadyCollected_ThrowsException() {
        membership.setHasCollected(true);
        when(thriftGroupRepository.findById(any())).thenReturn(Optional.of(group));
        when(membershipRepository.findByUserIdAndGroupId(any(), any()))
                .thenReturn(Optional.of(membership));

        assertThrows(
                BadRequestException.class,
                () -> payoutService.schedulePayout("group123", "user123", 2));

        verify(payoutRepository, never()).save(any());
    }

    private void stubPayableCycle() {
        when(payoutRepository.findById("payout123")).thenReturn(Optional.of(payout));
        when(thriftGroupRepository.findById("group123")).thenReturn(Optional.of(group));
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(membershipRepository.countByGroupId("group123")).thenReturn(3L);
        when(contributionRepository.countByGroupIdAndCycleNumberAndStatus(
                        "group123", 1, ContributionStatus.SUCCESSFUL))
                .thenReturn(3L);
    }

    @Test
    void processPayout_Success() {
        stubPayableCycle();
        when(membershipRepository.findById("membership123")).thenReturn(Optional.of(membership));
        when(userRepository.findById("user123")).thenReturn(Optional.of(currentUser));
        when(paystackService.initiateTransfer(any(), any(), any(), any()))
                .thenReturn(
                        Map.of(
                                "status",
                                true,
                                "data",
                                Map.of("transfer_code", "TRF_123", "status", "pending")));
        when(payoutMapper.toResponse(any())).thenReturn(payoutResponse);

        PayoutResponse result = payoutService.processPayout("payout123");

        assertNotNull(result);
        verify(paystackService)
                .initiateTransfer(
                        "RCP_123",
                        new BigDecimal("30000"),
                        "Office Ajo payout - cycle 1",
                        "payout-payout123");
        assertEquals(PayoutStatus.PROCESSING, payout.getStatus());
        assertEquals("TRF_123", payout.getPayStackTransferCode());
    }

    @Test
    void processPayout_NotAllMembersPaid_ThrowsException() {
        stubPayableCycle();
        when(contributionRepository.countByGroupIdAndCycleNumberAndStatus(
                        "group123", 1, ContributionStatus.SUCCESSFUL))
                .thenReturn(2L);

        assertThrows(BadRequestException.class, () -> payoutService.processPayout("payout123"));
        verify(paystackService, never()).initiateTransfer(any(), any(), any(), any());
    }

    @Test
    void processPayout_NoBankAccount_ThrowsException() {
        stubPayableCycle();
        when(membershipRepository.findById("membership123")).thenReturn(Optional.of(membership));
        User noBank = User.builder().id("user123").build();
        when(userRepository.findById("user123")).thenReturn(Optional.of(noBank));

        assertThrows(BadRequestException.class, () -> payoutService.processPayout("payout123"));
        verify(paystackService, never()).initiateTransfer(any(), any(), any(), any());
    }

    @Test
    void processPayout_MemberAlreadyCollected_ThrowsException() {
        stubPayableCycle();
        membership.setHasCollected(true);
        when(membershipRepository.findById("membership123")).thenReturn(Optional.of(membership));

        assertThrows(BadRequestException.class, () -> payoutService.processPayout("payout123"));
        verify(paystackService, never()).initiateTransfer(any(), any(), any(), any());
    }

    @Test
    void processPayout_NotGroupAdmin_ThrowsForbidden() {
        User otherUser =
                User.builder().id("other").roles(java.util.Set.of(Role.ROLE_ADMIN)).build();
        when(payoutRepository.findById("payout123")).thenReturn(Optional.of(payout));
        when(thriftGroupRepository.findById("group123")).thenReturn(Optional.of(group));
        when(userService.getCurrentUser()).thenReturn(otherUser);

        assertThrows(ForbiddenException.class, () -> payoutService.processPayout("payout123"));
        verify(paystackService, never()).initiateTransfer(any(), any(), any(), any());
    }

    @Test
    void processPayout_TransferRejected_RevertsToPending() {
        stubPayableCycle();
        when(membershipRepository.findById("membership123")).thenReturn(Optional.of(membership));
        when(userRepository.findById("user123")).thenReturn(Optional.of(currentUser));
        when(paystackService.initiateTransfer(any(), any(), any(), any()))
                .thenThrow(new BadRequestException("Transfer initiation failed"));

        assertThrows(BadRequestException.class, () -> payoutService.processPayout("payout123"));
        assertEquals(PayoutStatus.PENDING, payout.getStatus());
        assertEquals("payout-payout123", payout.getTransferReference());
    }

    @Test
    void handleTransferEvent_Success_MarksPaidAndNotifies() {
        payout.setStatus(PayoutStatus.PROCESSING);
        when(payoutRepository.findByTransferReference("payout-payout123"))
                .thenReturn(Optional.of(payout));
        when(membershipRepository.findById("membership123")).thenReturn(Optional.of(membership));
        when(userRepository.findById("user123")).thenReturn(Optional.of(currentUser));
        when(thriftGroupRepository.findById("group123")).thenReturn(Optional.of(group));

        payoutService.handleTransferEvent("transfer.success", "payout-payout123", null);

        assertEquals(PayoutStatus.SUCCESSFUL, payout.getStatus());
        assertNotNull(payout.getProcessedAt());
        assertTrue(membership.isHasCollected());
        verify(emailService).sendPayoutNotification(currentUser, group, "30000 NGN");
        verify(cycleService).onPayoutSuccessful(payout);
    }

    @Test
    void handleTransferEvent_Failed_RecordsReason() {
        payout.setStatus(PayoutStatus.PROCESSING);
        when(payoutRepository.findByTransferReference("payout-payout123"))
                .thenReturn(Optional.of(payout));

        payoutService.handleTransferEvent("transfer.failed", "payout-payout123", "Account closed");

        assertEquals(PayoutStatus.FAILED, payout.getStatus());
        assertEquals("Account closed", payout.getFailureReason());
        verify(emailService, never()).sendPayoutNotification(any(), any(), any());
    }

    @Test
    void handleTransferEvent_AlreadySuccessful_Ignored() {
        payout.setStatus(PayoutStatus.SUCCESSFUL);
        when(payoutRepository.findByTransferReference("payout-payout123"))
                .thenReturn(Optional.of(payout));

        payoutService.handleTransferEvent("transfer.success", "payout-payout123", null);

        verify(payoutRepository, never()).save(any());
        verify(emailService, never()).sendPayoutNotification(any(), any(), any());
    }

    @Test
    void processPayout_NotPending_ThrowsException() {
        payout.setStatus(PayoutStatus.SUCCESSFUL);
        when(payoutRepository.findById(any())).thenReturn(Optional.of(payout));

        assertThrows(BadRequestException.class, () -> payoutService.processPayout("payout123"));
    }

    @Test
    void getMyPayouts_Success() {
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(payoutRepository.findByUserId(any())).thenReturn(List.of(payout));
        when(payoutMapper.toResponse(any())).thenReturn(payoutResponse);

        List<PayoutResponse> result = payoutService.getMyPayouts();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getGroupPayouts_Success() {
        when(payoutRepository.findByGroupId("group123")).thenReturn(List.of(payout));
        when(payoutMapper.toResponse(any())).thenReturn(payoutResponse);

        List<PayoutResponse> result = payoutService.getGroupPayouts("group123");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void handleTransferEvent_CycleAdvanceFails_PayoutStillRecorded() {
        payout.setStatus(PayoutStatus.PROCESSING);
        when(payoutRepository.findByTransferReference("payout-payout123"))
                .thenReturn(Optional.of(payout));
        doThrow(new RuntimeException("db down")).when(cycleService).onPayoutSuccessful(any());

        assertDoesNotThrow(
                () ->
                        payoutService.handleTransferEvent(
                                "transfer.success", "payout-payout123", null));
        assertEquals(PayoutStatus.SUCCESSFUL, payout.getStatus());
    }

    @Test
    void handleTransferEvent_Failed_DoesNotAdvanceCycle() {
        payout.setStatus(PayoutStatus.PROCESSING);
        when(payoutRepository.findByTransferReference("payout-payout123"))
                .thenReturn(Optional.of(payout));

        payoutService.handleTransferEvent("transfer.failed", "payout-payout123", "Account closed");

        verify(cycleService, never()).onPayoutSuccessful(any());
    }
}
