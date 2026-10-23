package com.thrift.thriftsystem.service;

import com.thrift.thriftsystem.dto.request.CreateGroupRequest;
import com.thrift.thriftsystem.dto.response.GroupResponse;
import com.thrift.thriftsystem.enums.GroupStatus;
import com.thrift.thriftsystem.enums.MembershipStatus;
import com.thrift.thriftsystem.exception.BadRequestException;
import com.thrift.thriftsystem.exception.DuplicateResourceException;
import com.thrift.thriftsystem.exception.ResourceNotFoundException;
import com.thrift.thriftsystem.model.Membership;
import com.thrift.thriftsystem.model.ThriftGroup;
import com.thrift.thriftsystem.model.User;
import com.thrift.thriftsystem.repository.MembershipRepository;
import com.thrift.thriftsystem.repository.ThriftGroupRepository;
import com.thrift.thriftsystem.repository.UserRepository;
import com.thrift.thriftsystem.util.GroupMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ThriftGroupService {

    private final ThriftGroupRepository thriftGroupRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final EmailService emailService;
    private final GroupMapper groupMapper;
    private final CycleService cycleService;

    public GroupResponse createGroup(CreateGroupRequest createGroupRequest) {
        User currentUser = userService.getCurrentUser();

        if (thriftGroupRepository.existsByNameAndAdminId(
                createGroupRequest.getName(), currentUser.getId())) {
            throw new DuplicateResourceException("You already have a group with this name");
        }
        ThriftGroup group = groupMapper.toModel(createGroupRequest, currentUser.getId());
        group.getMemberIds().add(currentUser.getId());
        // Save first so the group has an id for the admin's membership
        thriftGroupRepository.save(group);

        Membership adminMembership =
                Membership.builder()
                        .userId(currentUser.getId())
                        .groupId(group.getId())
                        .payoutOrder(1)
                        .status(MembershipStatus.ACTIVE)
                        .joinedAt(LocalDateTime.now())
                        .build();

        membershipRepository.save(adminMembership);

        log.info("Group created: {} by {}", group.getName(), currentUser.getEmail());

        return groupMapper.toResponse(group);
    }

    public List<GroupResponse> getMyGroups() {
        User currentUser = userService.getCurrentUser();

        return thriftGroupRepository.findByMemberIdsContains(currentUser.getId()).stream()
                .map(groupMapper::toResponse)
                .collect(Collectors.toList());
    }

    public GroupResponse getGroupById(String groupId) {
        ThriftGroup group =
                thriftGroupRepository
                        .findById(groupId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Group not found with id:" + groupId));
        return groupMapper.toResponse(group);
    }

    public GroupResponse addMember(String groupId, String userId) {
        User currentUser = userService.getCurrentUser();
        ThriftGroup group =
                thriftGroupRepository
                        .findByIdAndAdminId(groupId, currentUser.getId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Group not found or you're not the admin"));

        if (group.getStartedAt() != null) {
            throw new BadRequestException("Members can't be added after the group has started");
        }

        if (membershipRepository.existsByUserIdAndGroupId(userId, groupId)) {
            throw new DuplicateResourceException("You already have a member with this membership");
        }

        long currentMembers = membershipRepository.countByGroupId(groupId);

        if (currentMembers >= group.getTotalMember()) {
            throw new BadRequestException("Group is already full");
        }
        Membership membership =
                Membership.builder()
                        .userId(userId)
                        .groupId(groupId)
                        .payoutOrder((int) currentMembers + 1)
                        .status(MembershipStatus.ACTIVE)
                        .joinedAt(LocalDateTime.now())
                        .build();

        membershipRepository.save(membership);
        group.getMemberIds().add(userId);
        thriftGroupRepository.save(group);

        User newMember =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        emailService.sendGroupJointedNotification(newMember, group);
        log.info("Member :{}, added to Group:{}", userId, groupId);
        return groupMapper.toResponse(group);
    }

    public GroupResponse startGroup(String groupId) {
        User currentUser = userService.getCurrentUser();
        ThriftGroup group =
                thriftGroupRepository
                        .findByIdAndAdminId(groupId, currentUser.getId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Group not found or you're not the admin"));

        if (group.getStartedAt() != null) {
            throw new BadRequestException("Group has already started");
        }

        long memberCount =
                membershipRepository
                        .findByGroupIdAndStatus(groupId, MembershipStatus.ACTIVE)
                        .size();
        if (memberCount < 2) {
            throw new BadRequestException("A group needs at least 2 members to start");
        }

        // Every member collects once, so there is one cycle per member. Cycles count from
        // the planned start date, or from now if that date has passed.
        LocalDateTime now = LocalDateTime.now();
        if (group.getStartDate() == null || group.getStartDate().isBefore(now)) {
            group.setStartDate(now);
        }
        group.setStartedAt(now);
        group.setTotalCycles((int) memberCount);
        group.setStatus(GroupStatus.ACTIVE);
        thriftGroupRepository.save(group);

        cycleService.openCycle(group, 1);
        log.info("Group {} started with {} members", groupId, memberCount);
        return groupMapper.toResponse(group);
    }

    public List<GroupResponse> getAllGroups() {
        return thriftGroupRepository.findAll().stream()
                .map(groupMapper::toResponse)
                .collect(Collectors.toList());
    }
}
