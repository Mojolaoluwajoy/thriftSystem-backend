package com.thrift.thriftsystem.service;

import com.thrift.thriftsystem.dto.request.ContributionRequest;
import com.thrift.thriftsystem.dto.response.ContributionResponse;
import com.thrift.thriftsystem.enums.ContributionStatus;
import com.thrift.thriftsystem.exception.BadRequestException;
import com.thrift.thriftsystem.exception.ResourceNotFoundException;
import com.thrift.thriftsystem.model.Contribution;
import com.thrift.thriftsystem.model.Membership;
import com.thrift.thriftsystem.model.ThriftGroup;
import com.thrift.thriftsystem.model.User;
import com.thrift.thriftsystem.repository.ContributionRepository;
import com.thrift.thriftsystem.repository.MembershipRepository;
import com.thrift.thriftsystem.repository.ThriftGroupRepository;
import com.thrift.thriftsystem.repository.UserRepository;
import com.thrift.thriftsystem.util.ContributionMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContributionService {

    private final ContributionRepository contributionRepository;
    private final MembershipRepository membershipRepository;
    private final ThriftGroupRepository thriftGroupRepository;
    private final UserService userService;
    private final ContributionMapper contributionMapper;
    private final EmailService emailService;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DUE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    public ContributionResponse initiateContribution(ContributionRequest contributionRequest) {
        User currentUser = userService.getCurrentUser();

        ThriftGroup group =
                thriftGroupRepository
                        .findById(contributionRequest.getGroupId())
                        .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        Membership membership =
                membershipRepository
                        .findByUserIdAndGroupId(
                                currentUser.getId(), contributionRequest.getGroupId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "You're not a member of this group"));

        if (contributionRepository.existsByUserIdAndGroupIdAndCycleNumber(
                currentUser.getId(),
                contributionRequest.getGroupId(),
                contributionRequest.getCycleNumber())) {
            throw new BadRequestException("You have already contributed fot this cycle");
        }

        Contribution contribution =
                contributionMapper.toModel(
                        contributionRequest,
                        currentUser.getId(),
                        membership.getId(),
                        group.getContributionAmount(),
                        LocalDateTime.now().plusDays(7));
        contributionRepository.save(contribution);
        log.info("Contribution has been initiated for user {}", currentUser.getId());
        return contributionMapper.toResponse(contribution);
    }

    public List<ContributionResponse> getMyContributions() {
        User currentUser = userService.getCurrentUser();

        return contributionRepository.findByUserId(currentUser.getId()).stream()
                .map(contributionMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<ContributionResponse> getGroupContributions(String groupId) {
        return contributionRepository.findByGroupId(groupId).stream()
                .map(contributionMapper::toResponse)
                .collect(Collectors.toList());
    }

    // Runs once a day, so each unpaid contribution is reminded once in its last 24 hours
    public int sendDueReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<Contribution> dueSoon =
                contributionRepository.findByStatusAndDueDateBetween(
                        ContributionStatus.PENDING, now, now.plusDays(1));

        int sent = 0;
        for (Contribution contribution : dueSoon) {
            User user = userRepository.findById(contribution.getUserId()).orElse(null);
            ThriftGroup group =
                    thriftGroupRepository.findById(contribution.getGroupId()).orElse(null);
            if (user == null || group == null) {
                log.warn(
                        "Skipping reminder for contribution {}: user or group missing",
                        contribution.getId());
                continue;
            }
            emailService.sendContributionReminder(
                    user, group, contribution.getDueDate().format(DUE_DATE_FORMAT));
            sent++;
        }
        return sent;
    }
}
