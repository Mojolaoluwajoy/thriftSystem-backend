package com.thrift.thriftsystem.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.thrift.thriftsystem.model.Membership;
import com.thrift.thriftsystem.model.ThriftGroup;
import com.thrift.thriftsystem.repository.MembershipRepository;
import com.thrift.thriftsystem.repository.ThriftGroupRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MembershipRepairTest {

    @Mock private ThriftGroupRepository groupRepository;
    @Mock private MembershipRepository membershipRepository;

    @InjectMocks private MembershipRepair membershipRepair;

    private ThriftGroup group;
    private Membership firstMember;
    private Membership secondMember;

    @BeforeEach
    void setUp() {
        group =
                ThriftGroup.builder()
                        .id("group1")
                        .adminId("admin")
                        .createdDate(LocalDateTime.of(2026, 1, 1, 9, 0))
                        .build();
        // Both numbered 1 and 2 without counting the admin, as the old code did
        firstMember = member("m1", "ada", 1, LocalDateTime.of(2026, 1, 2, 9, 0));
        secondMember = member("m2", "bayo", 2, LocalDateTime.of(2026, 1, 3, 9, 0));
    }

    private static Membership member(String id, String userId, int order, LocalDateTime joined) {
        return Membership.builder()
                .id(id)
                .userId(userId)
                .groupId("group1")
                .payoutOrder(order)
                .joinedAt(joined)
                .build();
    }

    @Test
    void reattachesOrphanedAdminMembershipAndPutsAdminFirst() {
        Membership orphan = Membership.builder().id("m0").userId("admin").payoutOrder(1).build();
        when(membershipRepository.findByGroupId("group1"))
                .thenReturn(List.of(secondMember, firstMember));
        when(membershipRepository.findFirstByUserIdAndGroupIdIsNull("admin"))
                .thenReturn(Optional.of(orphan));

        membershipRepair.repair(group);

        assertEquals("group1", orphan.getGroupId());
        assertEquals(1, orphan.getPayoutOrder());
        assertEquals(2, firstMember.getPayoutOrder());
        assertEquals(3, secondMember.getPayoutOrder());
        verify(membershipRepository).saveAll(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void createsAdminMembershipWhenNoOrphanExists() {
        when(membershipRepository.findByGroupId("group1")).thenReturn(List.of(firstMember));
        when(membershipRepository.findFirstByUserIdAndGroupIdIsNull("admin"))
                .thenReturn(Optional.empty());

        membershipRepair.repair(group);

        ArgumentCaptor<List<Membership>> saved = ArgumentCaptor.forClass(List.class);
        verify(membershipRepository).saveAll(saved.capture());
        Membership admin =
                saved.getValue().stream()
                        .filter(m -> "admin".equals(m.getUserId()))
                        .findFirst()
                        .orElseThrow();
        assertEquals("group1", admin.getGroupId());
        assertEquals(1, admin.getPayoutOrder());
        assertEquals(2, firstMember.getPayoutOrder());
    }

    @Test
    void leavesCorrectGroupUntouched() {
        Membership admin = member("m0", "admin", 1, LocalDateTime.of(2026, 1, 1, 9, 0));
        firstMember.setPayoutOrder(2);
        secondMember.setPayoutOrder(3);
        when(membershipRepository.findByGroupId("group1"))
                .thenReturn(List.of(admin, firstMember, secondMember));

        membershipRepair.repair(group);

        verify(membershipRepository, never()).saveAll(any());
    }

    @Test
    void skipsGroupsThatHaveStarted() {
        group.setStartedAt(LocalDateTime.now());
        when(groupRepository.findAll()).thenReturn(List.of(group));

        membershipRepair.run(null);

        verify(membershipRepository, never()).findByGroupId(any());
    }
}
