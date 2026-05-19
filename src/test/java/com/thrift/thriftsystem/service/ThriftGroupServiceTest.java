package com.thrift.thriftsystem.service;

import com.thrift.thriftsystem.dto.request.CreateGroupRequest;
import com.thrift.thriftsystem.dto.response.GroupResponse;
import com.thrift.thriftsystem.enums.ContributionFrequency;
import com.thrift.thriftsystem.enums.GroupStatus;
import com.thrift.thriftsystem.exception.DuplicateResourceException;
import com.thrift.thriftsystem.model.ThriftGroup;
import com.thrift.thriftsystem.model.User;
import com.thrift.thriftsystem.repository.MembershipRepository;
import com.thrift.thriftsystem.repository.ThriftGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThriftGroupServiceTest {

    @Mock
    private ThriftGroupRepository thriftGroupRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private UserService userService;

    @Mock
    private EmailService emailService;

    @Mock
    private com.thrift.thriftsystem.util.GroupMapper groupMapper;

    @InjectMocks
    private ThriftGroupService thriftGroupService;

    private User currentUser;
    private ThriftGroup group;
    private GroupResponse groupResponse;
    private CreateGroupRequest createGroupRequest;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
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

        group = ThriftGroup.builder()
                .id("group123")
                .name("Test Group")
                .adminId("user123")
                .contributionAmount(new BigDecimal("10000"))
                .currency("NGN")
                .status(GroupStatus.ACTIVE)
                .totalMember(5)
                .build();

        groupResponse = GroupResponse.builder()
                .id("group123")
                .name("Test Group")
                .adminId("user123")
                .build();
    }

    @Test
    void createGroup_Success() {
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(thriftGroupRepository.existsByNameAndAdminId(any(), any()))
                .thenReturn(false);
        when(groupMapper.toModel(any(), any())).thenReturn(group);
        when(thriftGroupRepository.save(any())).thenReturn(group);
        when(groupMapper.toResponse(any())).thenReturn(groupResponse);

        GroupResponse result = thriftGroupService
                .createGroup(createGroupRequest);

        assertNotNull(result);
        assertEquals("group123", result.getId());
        verify(thriftGroupRepository, times(2)).save(any());
        verify(membershipRepository, times(1)).save(any());
    }

    @Test
    void createGroup_DuplicateName_ThrowsException() {
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(thriftGroupRepository.existsByNameAndAdminId(any(), any()))
                .thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> thriftGroupService.createGroup(createGroupRequest));

        verify(thriftGroupRepository, never()).save(any());
    }

    @Test
    void getMyGroups_Success() {
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(thriftGroupRepository.findByMemberIdsContains(any()))
                .thenReturn(List.of(group));
        when(groupMapper.toResponse(any())).thenReturn(groupResponse);

        List<GroupResponse> result = thriftGroupService.getMyGroups();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getGroupById_Success() {
        when(thriftGroupRepository.findById("group123"))
                .thenReturn(Optional.of(group));
        when(groupMapper.toResponse(any())).thenReturn(groupResponse);

        GroupResponse result = thriftGroupService.getGroupById("group123");

        assertNotNull(result);
        assertEquals("group123", result.getId());
    }

    @Test
    void addMember_GroupFull_ThrowsException() {
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(thriftGroupRepository.findByIdAndAdminId(any(), any()))
                .thenReturn(Optional.of(group));
        when(membershipRepository.existsByUserIdAndGroupId(any(), any()))
                .thenReturn(false);
        when(membershipRepository.countByGroupId(any())).thenReturn(5L);

        assertThrows(Exception.class,
                () -> thriftGroupService.addMember("group123", "user456"));
    }
}