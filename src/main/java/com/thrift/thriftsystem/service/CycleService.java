package com.thrift.thriftsystem.service;

import com.thrift.thriftsystem.enums.ContributionFrequency;
import com.thrift.thriftsystem.enums.ContributionStatus;
import com.thrift.thriftsystem.enums.GroupStatus;
import com.thrift.thriftsystem.enums.MembershipStatus;
import com.thrift.thriftsystem.enums.PayoutStatus;
import com.thrift.thriftsystem.model.Contribution;
import com.thrift.thriftsystem.model.Membership;
import com.thrift.thriftsystem.model.Payout;
import com.thrift.thriftsystem.model.ThriftGroup;
import com.thrift.thriftsystem.repository.ContributionRepository;
import com.thrift.thriftsystem.repository.MembershipRepository;
import com.thrift.thriftsystem.repository.PayoutRepository;
import com.thrift.thriftsystem.repository.ThriftGroupRepository;
import com.thrift.thriftsystem.util.PayoutMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CycleService {

    private final ThriftGroupRepository groupRepository;
    private final MembershipRepository membershipRepository;
    private final ContributionRepository contributionRepository;
    private final PayoutRepository payoutRepository;
    private final PayoutMapper payoutMapper;
    private final HolidayService holidayService;

    // Creates every member's contribution for the cycle and schedules its payout. Safe to
    // call again for the same cycle: existing contributions and payouts are left alone.
    public void openCycle(ThriftGroup group, int cycleNumber) {
        List<Membership> members = activeMembersInPayoutOrder(group.getId());
        if (cycleNumber > members.size()) {
            throw new IllegalStateException(
                    "Group " + group.getId() + " has no member for cycle " + cycleNumber);
        }

        LocalDateTime dueDate = dueDateFor(group, cycleNumber);

        for (Membership member : members) {
            if (!contributionRepository.existsByUserIdAndGroupIdAndCycleNumber(
                    member.getUserId(), group.getId(), cycleNumber)) {
                contributionRepository.save(
                        Contribution.builder()
                                .userId(member.getUserId())
                                .groupId(group.getId())
                                .membershipId(member.getId())
                                .amount(group.getContributionAmount())
                                .currency(group.getCurrency())
                                .cycleNumber(cycleNumber)
                                .dueDate(dueDate)
                                .status(ContributionStatus.PENDING)
                                .build());
            }
        }

        if (!payoutRepository.existsByGroupIdAndCycleNumber(group.getId(), cycleNumber)) {
            Membership recipient = members.get(cycleNumber - 1);
            BigDecimal pot =
                    group.getContributionAmount().multiply(BigDecimal.valueOf(members.size()));
            Payout payout =
                    payoutMapper.toModel(
                            recipient.getUserId(),
                            group.getId(),
                            recipient.getId(),
                            pot,
                            group.getCurrency(),
                            cycleNumber,
                            dueDate);
            payoutRepository.save(payout);
        }

        group.setCurrentCycle(cycleNumber);
        groupRepository.save(group);
        log.info("Opened cycle {} for group {}, due {}", cycleNumber, group.getId(), dueDate);
    }

    // Called once a cycle's payout has been paid out: opens the next cycle, or completes
    // the group after the last one
    public void onPayoutSuccessful(Payout payout) {
        ThriftGroup group = groupRepository.findById(payout.getGroupId()).orElse(null);
        if (group == null || !payout.getCycleNumber().equals(group.getCurrentCycle())) {
            return;
        }

        int completedCycle = payout.getCycleNumber();
        if (completedCycle >= group.getTotalCycles()) {
            group.setStatus(GroupStatus.COMPLETED);
            group.setEndDate(LocalDateTime.now());
            groupRepository.save(group);
            log.info("Group {} completed after {} cycles", group.getId(), completedCycle);
            return;
        }

        openCycle(group, completedCycle + 1);
    }

    // Recovers groups whose cycle didn't advance when the payout webhook arrived
    public void advanceStalledGroups() {
        for (ThriftGroup group : groupRepository.findByStatus(GroupStatus.ACTIVE)) {
            if (group.getStartedAt() == null) {
                continue;
            }
            payoutRepository
                    .findByGroupIdAndCycleNumber(group.getId(), group.getCurrentCycle())
                    .filter(payout -> payout.getStatus() == PayoutStatus.SUCCESSFUL)
                    .ifPresent(
                            payout -> {
                                try {
                                    onPayoutSuccessful(payout);
                                } catch (Exception e) {
                                    log.error(
                                            "Failed to advance group {}: {}",
                                            group.getId(),
                                            e.getMessage());
                                }
                            });
        }
    }

    // Each cycle's contributions are due one period after the cycle opens, counted from the
    // group's start. A late cycle still gives members at least a day, and due dates that fall
    // on a weekend or public holiday move to the next working day.
    LocalDateTime dueDateFor(ThriftGroup group, int cycleNumber) {
        LocalDateTime due = addPeriods(group.getStartDate(), group.getFrequency(), cycleNumber);
        LocalDateTime earliest = LocalDateTime.now().plusDays(1);
        if (due.isBefore(earliest)) {
            due = earliest;
        }
        return holidayService.adjustForHoliday(due.toLocalDate()).atTime(due.toLocalTime());
    }

    static LocalDateTime addPeriods(
            LocalDateTime start, ContributionFrequency frequency, int periods) {
        return switch (frequency) {
            case DAILY -> start.plusDays(periods);
            case WEEKLY -> start.plusWeeks(periods);
            case BI_WEEKLY -> start.plusWeeks(2L * periods);
            case MONTHLY -> start.plusMonths(periods);
            case BI_MONTHLY -> start.plusMonths(2L * periods);
        };
    }

    private List<Membership> activeMembersInPayoutOrder(String groupId) {
        return membershipRepository
                .findByGroupIdAndStatus(groupId, MembershipStatus.ACTIVE)
                .stream()
                .sorted(Comparator.comparing(Membership::getPayoutOrder))
                .toList();
    }
}
