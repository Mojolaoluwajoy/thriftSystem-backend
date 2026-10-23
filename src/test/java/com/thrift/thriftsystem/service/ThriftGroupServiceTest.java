package com.thrift.thriftsystem.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.thrift.thriftsystem.dto.request.CreateGroupRequest;
import com.thrift.thriftsystem.dto.response.GroupResponse;
import com.thrift.thriftsystem.enums.ContributionFrequency;
import com.thrift.thriftsystem.enums.GroupStatus;
import com.thrift.thriftsystem.enums.MembershipStatus;
import com.thrift.thriftsystem.exception.BadRequestException;
import com.thrift.thriftsystem.exception.DuplicateResourceException;
import com.thrift.thriftsystem.model.Membership;
import com.thrift.thriftsystem.model.ThriftGroup;
import com.thrift.thriftsystem.model.User;
import com.thrift.thriftsystem.repository.MembershipRepository;
import com.thrift.thriftsystem.repository.ThriftGroupRepository;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ThriftGroupServiceTest {
    @Mock private ThriftGroupRepository thriftGroupRepository;

    @Mock private MembershipRepository membershipRepository;

    @Mock private UserService userService;

    @Mock private EmailService emailService;

    @Mock private com.thrift.thriftsystem.util.GroupMapper groupMapper;

    @Mock private CycleService cycleService;

    @InjectMocks private ThriftGroupService thriftGroupService;

    private User currentUser;
    private ThriftGroup group;
    private GroupResponse groupResponse;
    private CreateGroupRequest createGroupRequest;

    @BeforeEach
    void setUp() {
        currentUser =
                User.builder()
                        .id("user123")
                        .firstName("John")
                        .lastName("Doe")
                        .email("john@example.com")
                        .build();

        createGroupRequest = new CreateGroupRequest();
        createGroupRequest.setName("Test Group");
        createGroupRequest.setDescription("Test Description");
        createGroupRequest.setContributionAmount(new BigDecimal("10000"));
        createGroupRequest.setCurrency("NGN");
        createGroupRequest.setFrequency(ContributionFrequency.MONTHLY);
        createGroupRequest.setStartDate(LocalDateTime.now().plusDays(1));
        createGroupRequest.setTotalMembers(5);

        group =
                ThriftGroup.builder()
                        .id("group123")
                        .name("Test Group")
                        .adminId("user123")
                        .contributionAmount(new BigDecimal("10000"))
                        .currency("NGN")
                        .status(GroupStatus.ACTIVE)
                        .totalMember(5)
                        .memberIds(new java.util.ArrayList<>())
                        .build();

        groupResponse =
                GroupResponse.builder()
                        .id("group123")
                        .name("Test Group")
                        .adminId("user123")
                        .build();
    }

    @Test
    void createGroup_Success() {
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(thriftGroupRepository.existsByNameAndAdminId(any(), any())).thenReturn(false);
        when(groupMapper.toModel(any(), any())).thenReturn(group);
        when(thriftGroupRepository.save(any())).thenReturn(group);
        when(groupMapper.toResponse(any())).thenReturn(groupResponse);

        GroupResponse result = thriftGroupService.createGroup(createGroupRequest);

        assertNotNull(result);
        assertEquals("group123", result.getId());
        // The group must be saved first so the admin's membership gets its id
        org.mockito.InOrder order = inOrder(thriftGroupRepository, membershipRepository);
        order.verify(thriftGroupRepository).save(group);
        order.verify(membershipRepository)
                .save(
                        argThat(
                                (Membership m) ->
                                        "group123".equals(m.getGroupId())
                                                && "user123".equals(m.getUserId())
                                                && m.getPayoutOrder() == 1));
    }

    @Test
    void createGroup_DuplicateName_ThrowsException() {
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(thriftGroupRepository.existsByNameAndAdminId(any(), any())).thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> thriftGroupService.createGroup(createGroupRequest));

        verify(thriftGroupRepository, never()).save(any());
    }

    @Test
    void getMyGroups_Success() {
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(thriftGroupRepository.findByMemberIdsContains(any())).thenReturn(List.of(group));
        when(groupMapper.toResponse(any())).thenReturn(groupResponse);

        List<GroupResponse> result = thriftGroupService.getMyGroups();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getGroupById_Success() {
        when(thriftGroupRepository.findById("group123")).thenReturn(Optional.of(group));
        when(groupMapper.toResponse(any())).thenReturn(groupResponse);

        GroupResponse result = thriftGroupService.getGroupById("group123");

        assertNotNull(result);
        assertEquals("group123", result.getId());
    }

    @Test
    void addMember_GroupFull_ThrowsException() {
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(thriftGroupRepository.findByIdAndAdminId(any(), any())).thenReturn(Optional.of(group));
        when(membershipRepository.existsByUserIdAndGroupId(any(), any())).thenReturn(false);
        when(membershipRepository.countByGroupId(any())).thenReturn(5L);

        assertThrows(Exception.class, () -> thriftGroupService.addMember("group123", "user456"));
    }

    private void givenAdminWithMembers(int count) {
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(thriftGroupRepository.findByIdAndAdminId("group123", "user123"))
                .thenReturn(Optional.of(group));
        List<Membership> members =
                java.util.stream.IntStream.rangeClosed(1, count)
                        .mapToObj(
                                n ->
                                        Membership.builder()
                                                .id("m" + n)
                                                .userId("u" + n)
                                                .payoutOrder(n)
                                                .build())
                        .toList();
        when(membershipRepository.findByGroupIdAndStatus("group123", MembershipStatus.ACTIVE))
                .thenReturn(members);
    }

    @Test
    void startGroup_Success_OpensFirstCycle() {
        group.setStatus(GroupStatus.FORMING);
        group.setStartDate(LocalDateTime.now().plusDays(3));
        givenAdminWithMembers(3);
        when(groupMapper.toResponse(any())).thenReturn(groupResponse);

        thriftGroupService.startGroup("group123");

        assertEquals(GroupStatus.ACTIVE, group.getStatus());
        assertEquals(3, group.getTotalCycles());
        assertNotNull(group.getStartedAt());
        verify(cycleService).openCycle(group, 1);
    }

    @Test
    void startGroup_PastStartDate_CountsCyclesFromNow() {
        group.setStartDate(LocalDateTime.now().minusMonths(2));
        givenAdminWithMembers(2);

        thriftGroupService.startGroup("group123");

        assertFalse(group.getStartDate().isBefore(LocalDateTime.now().minusMinutes(1)));
    }

    @Test
    void startGroup_TooFewMembers_ThrowsException() {
        givenAdminWithMembers(1);

        assertThrows(BadRequestException.class, () -> thriftGroupService.startGroup("group123"));
        verify(cycleService, never()).openCycle(any(), anyInt());
    }

    @Test
    void startGroup_AlreadyStarted_ThrowsException() {
        group.setStartedAt(LocalDateTime.now().minusDays(1));
        givenAdminWithMembers(3);

        assertThrows(BadRequestException.class, () -> thriftGroupService.startGroup("group123"));
        verify(cycleService, never()).openCycle(any(), anyInt());
    }

    @Test
    void startGroup_NotAdmin_ThrowsNotFound() {
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(thriftGroupRepository.findByIdAndAdminId(any(), any())).thenReturn(Optional.empty());

        assertThrows(
                com.thrift.thriftsystem.exception.ResourceNotFoundException.class,
                () -> thriftGroupService.startGroup("group123"));
    }

    @Test
    void addMember_AfterStart_ThrowsException() {
        group.setStartedAt(LocalDateTime.now());
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(thriftGroupRepository.findByIdAndAdminId(any(), any())).thenReturn(Optional.of(group));

        assertThrows(
                BadRequestException.class,
                () -> thriftGroupService.addMember("group123", "user456"));
        verify(membershipRepository, never()).save(any());
    }
}
