package com.thrift.thriftsystem.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.thrift.thriftsystem.dto.request.CreateGroupRequest;
import com.thrift.thriftsystem.enums.ContributionFrequency;
import com.thrift.thriftsystem.enums.ContributionStatus;
import com.thrift.thriftsystem.enums.GroupStatus;
import com.thrift.thriftsystem.enums.PayoutStatus;
import com.thrift.thriftsystem.model.Contribution;
import com.thrift.thriftsystem.model.Payout;
import com.thrift.thriftsystem.model.ThriftGroup;
import com.thrift.thriftsystem.model.User;
import com.thrift.thriftsystem.repository.ContributionRepository;
import com.thrift.thriftsystem.repository.MembershipRepository;
import com.thrift.thriftsystem.repository.PayoutRepository;
import com.thrift.thriftsystem.repository.ThriftGroupRepository;
import com.thrift.thriftsystem.repository.UserRepository;
import com.thrift.thriftsystem.service.EmailService;
import com.thrift.thriftsystem.service.HolidayService;
import com.thrift.thriftsystem.service.PayoutService;
import com.thrift.thriftsystem.service.ThriftGroupService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

// Runs a two-member group from forming to completed against the real MongoDB from MONGO_URI
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = ContributionCycleFlowTest.ADMIN_EMAIL)
class ContributionCycleFlowTest {

    static final String ADMIN_EMAIL = "cycle-flow-admin@test.local";
    static final String MEMBER_EMAIL = "cycle-flow-member@test.local";

    @Autowired private MockMvc mockMvc;
    @Autowired private ThriftGroupService thriftGroupService;
    @Autowired private PayoutService payoutService;
    @Autowired private UserRepository userRepository;
    @Autowired private ThriftGroupRepository groupRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private ContributionRepository contributionRepository;
    @Autowired private PayoutRepository payoutRepository;

    // No outside calls: no holiday lookups or emails
    @MockBean private HolidayService holidayService;
    @MockBean private EmailService emailService;

    private User admin;
    private User member;
    private String groupId;

    @BeforeEach
    void setUp() {
        cleanUp();
        when(holidayService.adjustForHoliday(any())).thenAnswer(inv -> inv.getArgument(0));
        admin = userRepository.save(User.builder().email(ADMIN_EMAIL).firstName("Ada").build());
        member = userRepository.save(User.builder().email(MEMBER_EMAIL).firstName("Bayo").build());
    }

    @AfterEach
    void cleanUp() {
        if (groupId != null) {
            contributionRepository.deleteAll(contributionRepository.findByGroupId(groupId));
            payoutRepository.deleteAll(payoutRepository.findByGroupId(groupId));
            membershipRepository.deleteAll(membershipRepository.findByGroupId(groupId));
            groupRepository.deleteById(groupId);
        }
        userRepository.findByEmail(ADMIN_EMAIL).ifPresent(userRepository::delete);
        userRepository.findByEmail(MEMBER_EMAIL).ifPresent(userRepository::delete);
    }

    @Test
    void groupRunsEveryCycleAndCompletes() throws Exception {
        CreateGroupRequest request = new CreateGroupRequest();
        request.setName("Cycle Flow Test Group");
        request.setContributionAmount(new BigDecimal("5000"));
        request.setCurrency("NGN");
        request.setFrequency(ContributionFrequency.WEEKLY);
        request.setStartDate(LocalDateTime.now().plusDays(2));
        request.setTotalMembers(2);
        groupId = thriftGroupService.createGroup(request).getId();

        assertEquals(GroupStatus.FORMING, group().getStatus());

        thriftGroupService.addMember(groupId, member.getId());
        thriftGroupService.startGroup(groupId);

        // Cycle 1: both members owe, the admin (first in payout order) collects
        assertEquals(1, group().getCurrentCycle());
        assertEquals(2, group().getTotalCycles());
        assertCycleOpened(1, admin);

        mockMvc.perform(get("/api/v1/group/" + groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentCycle").value(1));

        completeCycle(1);

        // Cycle 2: the member collects
        assertEquals(2, group().getCurrentCycle());
        assertCycleOpened(2, member);

        completeCycle(2);

        ThriftGroup finished = group();
        assertEquals(GroupStatus.COMPLETED, finished.getStatus());
        assertNotNull(finished.getEndDate());
        assertEquals(4, contributionRepository.findByGroupId(groupId).size());
    }

    private void assertCycleOpened(int cycle, User expectedRecipient) {
        List<Contribution> contributions =
                contributionRepository.findByGroupIdAndCycleNumber(groupId, cycle);
        assertEquals(2, contributions.size());
        contributions.forEach(c -> assertEquals(ContributionStatus.PENDING, c.getStatus()));

        Payout payout = payoutRepository.findByGroupIdAndCycleNumber(groupId, cycle).orElseThrow();
        assertEquals(expectedRecipient.getId(), payout.getUserId());
        assertEquals(0, new BigDecimal("10000").compareTo(payout.getAmount()));
    }

    // Stands in for Paystack: members pay, then the transfer webhook reports success
    private void completeCycle(int cycle) {
        for (Contribution c : contributionRepository.findByGroupIdAndCycleNumber(groupId, cycle)) {
            c.setStatus(ContributionStatus.SUCCESSFUL);
            contributionRepository.save(c);
        }

        Payout payout = payoutRepository.findByGroupIdAndCycleNumber(groupId, cycle).orElseThrow();
        payout.setStatus(PayoutStatus.PROCESSING);
        payout.setTransferReference("payout-flow-" + payout.getId());
        payoutRepository.save(payout);

        payoutService.handleTransferEvent("transfer.success", payout.getTransferReference(), null);

        assertEquals(
                PayoutStatus.SUCCESSFUL,
                payoutRepository.findById(payout.getId()).orElseThrow().getStatus());
    }

    private ThriftGroup group() {
        return groupRepository.findById(groupId).orElseThrow();
    }
}
