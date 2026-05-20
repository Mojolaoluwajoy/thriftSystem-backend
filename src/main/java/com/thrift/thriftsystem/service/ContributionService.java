package com.thrift.thriftsystem.service;

import com.thrift.thriftsystem.dto.request.ContributionRequest;
import com.thrift.thriftsystem.dto.response.ContributionResponse;
import com.thrift.thriftsystem.exception.BadRequestException;
import com.thrift.thriftsystem.exception.ResourceNotFoundException;
import com.thrift.thriftsystem.model.Contribution;
import com.thrift.thriftsystem.model.Membership;
import com.thrift.thriftsystem.model.ThriftGroup;
import com.thrift.thriftsystem.model.User;
import com.thrift.thriftsystem.repository.ContributionRepository;
import com.thrift.thriftsystem.repository.MembershipRepository;
import com.thrift.thriftsystem.repository.ThriftGroupRepository;
import com.thrift.thriftsystem.util.ContributionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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

    public ContributionResponse initiateContribution(ContributionRequest contributionRequest) {
        User currentUser = userService.getCurrentUser();

        ThriftGroup group = thriftGroupRepository.findById(contributionRequest.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        Membership membership = membershipRepository.findByUserIdAndGroupId(currentUser.getId(), contributionRequest.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("You're not a member of this group"));

        if (contributionRepository.existsByUserIdAndGroupIdAndCycleNumber(currentUser.getId(), contributionRequest.getGroupId(), contributionRequest.getCycleNumber())) {
            throw new BadRequestException("You have already contributed fot this cycle");
        }

        Contribution contribution = contributionMapper.toModel(contributionRequest,currentUser.getId(),membership.getId(),group.getContributionAmount(), LocalDateTime.now().plusDays(7));
            contributionRepository.save(contribution);
            log.info("Contribution has been initiated for user {}", currentUser.getId());
            return contributionMapper.toResponse(contribution);
    }

    public List<ContributionResponse>  getMyContributions(){
        User currentUser = userService.getCurrentUser();

        return contributionRepository.findByUserId(currentUser.getId())
                .stream()
                .map(contributionMapper::toResponse)
                .collect(Collectors.toList());
    }
    public List<ContributionResponse>  getGroupContributions(String groupId){
        return contributionRepository.findByGroupId(groupId)
                .stream()
                .map(contributionMapper::toResponse)
                .collect(Collectors.toList());

    }
}
