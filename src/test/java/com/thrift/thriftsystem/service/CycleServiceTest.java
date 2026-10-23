package com.thrift.thriftsystem.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CycleServiceTest {

    @Mock private ThriftGroupRepository groupRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private ContributionRepository contributionRepository;
    @Mock private PayoutRepository payoutRepository;
    @Mock private HolidayService holidayService;

    // Real mapper so the scheduled payout's fields can be checked
    private final PayoutMapper payoutMapper = new PayoutMapper();

    private CycleService cycleService;

    private ThriftGroup group;
    private Membership ada;
    private Membership bayo;
    private Membership chi;

    @BeforeEach
    void setUp() {
        cycleService =
                new CycleService(
                        groupRepository,
                        membershipRepository,
                        contributionRepository,
                        payoutRepository,
                        payoutMapper,
                        holidayService);

        group =
                ThriftGroup.builder()
                        .id("group1")
                        .contributionAmount(new BigDecimal("10000"))
                        .currency("NGN")
                        .frequency(ContributionFrequency.MONTHLY)
                        .startDate(LocalDateTime.of(2030, 1, 6, 10, 0))
                        .startedAt(LocalDateTime.now())
                        .totalCycles(3)
                        .currentCycle(0)
                        .status(GroupStatus.ACTIVE)
                        .build();

        // Returned out of order to check sorting by payout order
        ada = member("m-ada", "ada", 1);
        bayo = member("m-bayo", "bayo", 2);
        chi = member("m-chi", "chi", 3);
        when(membershipRepository.findByGroupIdAndStatus("group1", MembershipStatus.ACTIVE))
                .thenReturn(List.of(chi, ada, bayo));

        // No holidays unless a test says otherwise
        when(holidayService.adjustForHoliday(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Membership member(String id, String userId, int order) {
        return Membership.builder()
                .id(id)
                .userId(userId)
                .groupId("group1")
                .payoutOrder(order)
                .status(MembershipStatus.ACTIVE)
                .build();
    }

    @Test
    void openCycle_CreatesContributionForEveryMember() {
        cycleService.openCycle(group, 1);

        ArgumentCaptor<Contribution> saved = ArgumentCaptor.forClass(Contribution.class);
        verify(contributionRepository, times(3)).save(saved.capture());
        assertEquals(
                List.of("ada", "bayo", "chi"),
                saved.getAllValues().stream().map(Contribution::getUserId).sorted().toList());
        for (Contribution c : saved.getAllValues()) {
            assertEquals(1, c.getCycleNumber());
            assertEquals(new BigDecimal("10000"), c.getAmount());
            assertEquals(ContributionStatus.PENDING, c.getStatus());
            assertEquals(LocalDateTime.of(2030, 2, 6, 10, 0), c.getDueDate());
        }
        assertEquals(1, group.getCurrentCycle());
    }

    @Test
    void openCycle_SchedulesPotForMemberWithMatchingPayoutOrder() {
        cycleService.openCycle(group, 2);

        ArgumentCaptor<Payout> payout = ArgumentCaptor.forClass(Payout.class);
        verify(payoutRepository).save(payout.capture());
        assertEquals("bayo", payout.getValue().getUserId());
        assertEquals("m-bayo", payout.getValue().getMembershipId());
        assertEquals(new BigDecimal("30000"), payout.getValue().getAmount());
        assertEquals(2, payout.getValue().getCycleNumber());
        assertEquals(LocalDateTime.of(2030, 3, 6, 10, 0), payout.getValue().getScheduledDate());
    }

    @Test
    void openCycle_SkipsExistingContributionsAndPayout() {
        when(contributionRepository.existsByUserIdAndGroupIdAndCycleNumber("ada", "group1", 1))
                .thenReturn(true);
        when(payoutRepository.existsByGroupIdAndCycleNumber("group1", 1)).thenReturn(true);

        cycleService.openCycle(group, 1);

        verify(contributionRepository, times(2)).save(any());
        verify(payoutRepository, never()).save(any());
    }

    @Test
    void openCycle_BeyondMemberCount_Throws() {
        assertThrows(IllegalStateException.class, () -> cycleService.openCycle(group, 4));
        verify(contributionRepository, never()).save(any());
    }

    @Test
    void dueDate_MovesOffHolidaysAndWeekends() {
        // 6 Feb 2030 is adjusted to the 7th by the holiday service
        when(holidayService.adjustForHoliday(LocalDate.of(2030, 2, 6)))
                .thenReturn(LocalDate.of(2030, 2, 7));

        assertEquals(LocalDateTime.of(2030, 2, 7, 10, 0), cycleService.dueDateFor(group, 1));
    }

    @Test
    void dueDate_LateCycleGivesAtLeastADay() {
        group.setStartDate(LocalDateTime.now().minusMonths(6));

        LocalDateTime due = cycleService.dueDateFor(group, 1);

        assertFalse(due.isBefore(LocalDateTime.now().plusHours(23)));
    }

    @Test
    void addPeriods_CoversEveryFrequency() {
        LocalDateTime start = LocalDateTime.of(2030, 1, 31, 9, 0);
        assertEquals(
                start.plusDays(3), CycleService.addPeriods(start, ContributionFrequency.DAILY, 3));
        assertEquals(
                start.plusWeeks(3),
                CycleService.addPeriods(start, ContributionFrequency.WEEKLY, 3));
        assertEquals(
                start.plusWeeks(6),
                CycleService.addPeriods(start, ContributionFrequency.BI_WEEKLY, 3));
        // End-of-month dates clamp rather than overflow (31 Jan + 1 month = 28 Feb)
        assertEquals(
                LocalDateTime.of(2030, 2, 28, 9, 0),
                CycleService.addPeriods(start, ContributionFrequency.MONTHLY, 1));
        assertEquals(
                start.plusMonths(6),
                CycleService.addPeriods(start, ContributionFrequency.BI_MONTHLY, 3));
    }

    @Test
    void onPayoutSuccessful_OpensNextCycle() {
        group.setCurrentCycle(1);
        when(groupRepository.findById("group1")).thenReturn(Optional.of(group));

        cycleService.onPayoutSuccessful(payout(1));

        assertEquals(2, group.getCurrentCycle());
        verify(contributionRepository, times(3)).save(any());
    }

    @Test
    void onPayoutSuccessful_LastCycle_CompletesGroup() {
        group.setCurrentCycle(3);
        when(groupRepository.findById("group1")).thenReturn(Optional.of(group));

        cycleService.onPayoutSuccessful(payout(3));

        assertEquals(GroupStatus.COMPLETED, group.getStatus());
        assertNotNull(group.getEndDate());
        verify(contributionRepository, never()).save(any());
    }

    @Test
    void onPayoutSuccessful_StaleCycle_Ignored() {
        group.setCurrentCycle(2);
        when(groupRepository.findById("group1")).thenReturn(Optional.of(group));

        cycleService.onPayoutSuccessful(payout(1));

        assertEquals(2, group.getCurrentCycle());
        verify(groupRepository, never()).save(any());
    }

    @Test
    void advanceStalledGroups_AdvancesGroupWhosePayoutSucceeded() {
        group.setCurrentCycle(1);
        Payout paid = payout(1);
        paid.setStatus(PayoutStatus.SUCCESSFUL);
        when(groupRepository.findByStatus(GroupStatus.ACTIVE)).thenReturn(List.of(group));
        when(groupRepository.findById("group1")).thenReturn(Optional.of(group));
        when(payoutRepository.findByGroupIdAndCycleNumber("group1", 1))
                .thenReturn(Optional.of(paid));

        cycleService.advanceStalledGroups();

        assertEquals(2, group.getCurrentCycle());
    }

    @Test
    void advanceStalledGroups_LeavesGroupWithPendingPayout() {
        group.setCurrentCycle(1);
        Payout pending = payout(1);
        when(groupRepository.findByStatus(GroupStatus.ACTIVE)).thenReturn(List.of(group));
        when(payoutRepository.findByGroupIdAndCycleNumber("group1", 1))
                .thenReturn(Optional.of(pending));

        cycleService.advanceStalledGroups();

        assertEquals(1, group.getCurrentCycle());
        verify(groupRepository, never()).save(any());
    }

    private static Payout payout(int cycle) {
        return Payout.builder()
                .id("payout-" + cycle)
                .groupId("group1")
                .cycleNumber(cycle)
                .status(PayoutStatus.PENDING)
                .build();
    }
}
