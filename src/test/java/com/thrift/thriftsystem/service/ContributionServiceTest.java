package com.thrift.thriftsystem.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.thrift.thriftsystem.dto.request.ContributionRequest;
import com.thrift.thriftsystem.dto.response.ContributionResponse;
import com.thrift.thriftsystem.enums.ContributionStatus;
import com.thrift.thriftsystem.exception.BadRequestException;
import com.thrift.thriftsystem.model.Contribution;
import com.thrift.thriftsystem.model.Membership;
import com.thrift.thriftsystem.model.ThriftGroup;
import com.thrift.thriftsystem.model.User;
import com.thrift.thriftsystem.repository.ContributionRepository;
import com.thrift.thriftsystem.repository.MembershipRepository;
import com.thrift.thriftsystem.repository.ThriftGroupRepository;
import com.thrift.thriftsystem.repository.UserRepository;
import com.thrift.thriftsystem.util.ContributionMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContributionServiceTest {

    @Mock private ContributionRepository contributionRepository;

    @Mock private MembershipRepository membershipRepository;

    @Mock private ThriftGroupRepository thriftGroupRepository;

    @Mock private UserService userService;

    @Mock private ContributionMapper contributionMapper;

    @Mock private EmailService emailService;

    @Mock private UserRepository userRepository;

    @InjectMocks private ContributionService contributionService;

    private User currentUser;
    private ThriftGroup group;
    private Membership membership;
    private Contribution contribution;
    private ContributionRequest contributionRequest;
    private ContributionResponse contributionResponse;

    @BeforeEach
    void setUp() {
        currentUser = User.builder().id("user123").email("john@example.com").build();

        group =
                ThriftGroup.builder()
                        .id("group123")
                        .contributionAmount(new BigDecimal("10000"))
                        .currency("NGN")
                        .build();

        membership =
                Membership.builder()
                        .id("membership123")
                        .userId("user123")
                        .groupId("group123")
                        .build();

        contribution =
                Contribution.builder()
                        .id("contribution123")
                        .userId("user123")
                        .groupId("group123")
                        .amount(new BigDecimal("10000"))
                        .status(ContributionStatus.PENDING)
                        .cycleNumber(1)
                        .build();

        contributionRequest = new ContributionRequest();
        contributionRequest.setGroupId("group123");
        contributionRequest.setCycleNumber(1);
        contributionRequest.setCurrency("NGN");

        contributionResponse =
                ContributionResponse.builder()
                        .id("contribution123")
                        .userId("user123")
                        .groupId("group123")
                        .status(ContributionStatus.PENDING)
                        .build();
    }

    @Test
    void initiateContribution_Success() {
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(thriftGroupRepository.findById("group123")).thenReturn(Optional.of(group));
        when(membershipRepository.findByUserIdAndGroupId(any(), any()))
                .thenReturn(Optional.of(membership));
        when(contributionRepository.existsByUserIdAndGroupIdAndCycleNumber(any(), any(), anyInt()))
                .thenReturn(false);
        when(contributionMapper.toModel(any(), any(), any(), any(), any()))
                .thenReturn(contribution);
        when(contributionRepository.save(any())).thenReturn(contribution);
        when(contributionMapper.toResponse(any())).thenReturn(contributionResponse);

        ContributionResponse result = contributionService.initiateContribution(contributionRequest);

        assertNotNull(result);
        assertEquals("contribution123", result.getId());
        verify(contributionRepository, times(1)).save(any());
    }

    @Test
    void initiateContribution_AlreadyContributed_ThrowsException() {
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(thriftGroupRepository.findById(any())).thenReturn(Optional.of(group));
        when(membershipRepository.findByUserIdAndGroupId(any(), any()))
                .thenReturn(Optional.of(membership));
        when(contributionRepository.existsByUserIdAndGroupIdAndCycleNumber(any(), any(), anyInt()))
                .thenReturn(true);

        assertThrows(
                BadRequestException.class,
                () -> contributionService.initiateContribution(contributionRequest));

        verify(contributionRepository, never()).save(any());
    }

    @Test
    void initiateContribution_NotMember_ThrowsException() {
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(thriftGroupRepository.findById(any())).thenReturn(Optional.of(group));
        when(membershipRepository.findByUserIdAndGroupId(any(), any()))
                .thenReturn(Optional.empty());

        assertThrows(
                Exception.class,
                () -> contributionService.initiateContribution(contributionRequest));
    }

    @Test
    void getMyContributions_Success() {
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(contributionRepository.findByUserId(any())).thenReturn(List.of(contribution));
        when(contributionMapper.toResponse(any())).thenReturn(contributionResponse);

        List<ContributionResponse> result = contributionService.getMyContributions();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getGroupContributions_Success() {
        when(contributionRepository.findByGroupId("group123")).thenReturn(List.of(contribution));
        when(contributionMapper.toResponse(any())).thenReturn(contributionResponse);

        List<ContributionResponse> result = contributionService.getGroupContributions("group123");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void sendDueReminders_EmailsMembersWithContributionsDueSoon() {
        contribution.setDueDate(LocalDateTime.of(2026, 10, 23, 9, 0));
        when(contributionRepository.findByStatusAndDueDateBetween(
                        eq(ContributionStatus.PENDING), any(), any()))
                .thenReturn(List.of(contribution));
        when(userRepository.findById("user123")).thenReturn(Optional.of(currentUser));
        when(thriftGroupRepository.findById("group123")).thenReturn(Optional.of(group));

        int sent = contributionService.sendDueReminders();

        assertEquals(1, sent);
        verify(emailService).sendContributionReminder(currentUser, group, "23 Oct 2026");
    }

    @Test
    void sendDueReminders_SkipsWhenUserMissing() {
        contribution.setDueDate(LocalDateTime.now().plusHours(5));
        when(contributionRepository.findByStatusAndDueDateBetween(any(), any(), any()))
                .thenReturn(List.of(contribution));
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        when(thriftGroupRepository.findById(any())).thenReturn(Optional.of(group));

        assertEquals(0, contributionService.sendDueReminders());
        verify(emailService, never()).sendContributionReminder(any(), any(), any());
    }
}
