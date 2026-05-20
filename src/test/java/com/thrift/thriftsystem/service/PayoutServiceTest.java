package com.thrift.thriftsystem.service;

import com.thrift.thriftsystem.dto.response.PayoutResponse;
import com.thrift.thriftsystem.enums.PayoutStatus;
import com.thrift.thriftsystem.exception.BadRequestException;
import com.thrift.thriftsystem.model.Membership;
import com.thrift.thriftsystem.model.Payout;
import com.thrift.thriftsystem.model.ThriftGroup;
import com.thrift.thriftsystem.model.User;
import com.thrift.thriftsystem.repository.MembershipRepository;
import com.thrift.thriftsystem.repository.PayoutRepository;
import com.thrift.thriftsystem.repository.ThriftGroupRepository;
import com.thrift.thriftsystem.util.PayoutMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayoutServiceTest {

    @Mock
    private PayoutRepository payoutRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private ThriftGroupRepository thriftGroupRepository;

    @Mock
    private UserService userService;

    @Mock
    private PayoutMapper payoutMapper;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PayoutService payoutService;

    private User currentUser;
    private ThriftGroup group;
    private Membership membership;
    private Payout payout;
    private PayoutResponse payoutResponse;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id("user123")
                .email("john@example.com")
                .build();

        group = ThriftGroup.builder()
                .id("group123")
                .contributionAmount(new BigDecimal("10000"))
                .currency("NGN")
                .build();

        membership = Membership.builder()
                .id("membership123")
                .userId("user123")
                .groupId("group123")
                .build();

        payout = Payout.builder()
                .id("payout123")
                .userId("user123")
                .groupId("group123")
                .amount(new BigDecimal("10000"))
                .status(PayoutStatus.PENDING)
                .cycleNumber(1)
                .build();

        payoutResponse = PayoutResponse.builder()
                .id("payout123")
                .userId("user123")
                .groupId("group123")
                .payoutStatus(PayoutStatus.PENDING)
                .build();
    }

    @Test
    void schedulePayout_Success() {
        when(thriftGroupRepository.findById("group123"))
                .thenReturn(Optional.of(group));
        when(membershipRepository.findByUserIdAndGroupId(any(), any()))
                .thenReturn(Optional.of(membership));
        when(payoutRepository
                .existsByUserIdAndGroupIdAndCycleNumber(any(), any(), anyInt()))
                .thenReturn(false);
        when(payoutMapper.toModel(any(), any(), any(), any(), any(), anyInt(), any()))
                .thenReturn(payout);
        when(payoutRepository.save(any())).thenReturn(payout);
        when(payoutMapper.toResponse(any())).thenReturn(payoutResponse);

        PayoutResponse result = payoutService
                .schedulePayout("group123", "user123", 1);

        assertNotNull(result);
        assertEquals("payout123", result.getId());
        verify(payoutRepository, times(1)).save(any());
    }

    @Test
    void schedulePayout_AlreadyScheduled_ThrowsException() {
        when(thriftGroupRepository.findById(any()))
                .thenReturn(Optional.of(group));
        when(membershipRepository.findByUserIdAndGroupId(any(), any()))
                .thenReturn(Optional.of(membership));
        when(payoutRepository
                .existsByUserIdAndGroupIdAndCycleNumber(any(), any(), anyInt()))
                .thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> payoutService.schedulePayout("group123", "user123", 1));

        verify(payoutRepository, never()).save(any());
    }

    @Test
    void processPayout_Success() {
        when(payoutRepository.findById("payout123"))
                .thenReturn(Optional.of(payout));
        when(payoutRepository.save(any())).thenReturn(payout);
        when(payoutMapper.toResponse(any())).thenReturn(payoutResponse);

        PayoutResponse result = payoutService.processPayout("payout123");

        assertNotNull(result);
        verify(payoutRepository, times(1)).save(any());
    }

    @Test
    void processPayout_NotPending_ThrowsException() {
        payout.setStatus(PayoutStatus.SUCCESSFUL);
        when(payoutRepository.findById(any()))
                .thenReturn(Optional.of(payout));

        assertThrows(BadRequestException.class,
                () -> payoutService.processPayout("payout123"));
    }

    @Test
    void getMyPayouts_Success() {
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(payoutRepository.findByUserId(any()))
                .thenReturn(List.of(payout));
        when(payoutMapper.toResponse(any())).thenReturn(payoutResponse);

        List<PayoutResponse> result = payoutService.getMyPayouts();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getGroupPayouts_Success() {
        when(payoutRepository.findByGroupId("group123"))
                .thenReturn(List.of(payout));
        when(payoutMapper.toResponse(any())).thenReturn(payoutResponse);

        List<PayoutResponse> result = payoutService
                .getGroupPayouts("group123");

        assertNotNull(result);
        assertEquals(1, result.size());
    }
}