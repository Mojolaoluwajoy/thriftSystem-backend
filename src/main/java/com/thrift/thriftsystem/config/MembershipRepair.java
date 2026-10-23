package com.thrift.thriftsystem.config;

import com.thrift.thriftsystem.enums.MembershipStatus;
import com.thrift.thriftsystem.model.Membership;
import com.thrift.thriftsystem.model.ThriftGroup;
import com.thrift.thriftsystem.repository.MembershipRepository;
import com.thrift.thriftsystem.repository.ThriftGroupRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// Groups used to save the admin's membership before the group had an id, leaving it with
// groupId null, and members' payout order was counted without the admin. For groups that
// haven't started, reattach the admin and renumber payout order: admin first, then by join date.
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class MembershipRepair implements ApplicationRunner {

    private final ThriftGroupRepository groupRepository;
    private final MembershipRepository membershipRepository;

    @Override
    public void run(ApplicationArguments args) {
        for (ThriftGroup group : groupRepository.findAll()) {
            if (group.getStartedAt() == null) {
                repair(group);
            }
        }
    }

    void repair(ThriftGroup group) {
        List<Membership> members =
                new ArrayList<>(membershipRepository.findByGroupId(group.getId()));
        List<Membership> changed = new ArrayList<>();

        boolean adminMissing =
                members.stream().noneMatch(m -> group.getAdminId().equals(m.getUserId()));
        if (adminMissing) {
            Membership admin =
                    membershipRepository
                            .findFirstByUserIdAndGroupIdIsNull(group.getAdminId())
                            .orElseGet(
                                    () ->
                                            Membership.builder()
                                                    .userId(group.getAdminId())
                                                    .status(MembershipStatus.ACTIVE)
                                                    .joinedAt(
                                                            group.getCreatedDate() != null
                                                                    ? group.getCreatedDate()
                                                                    : LocalDateTime.now())
                                                    .build());
            admin.setGroupId(group.getId());
            members.add(admin);
            changed.add(admin);
            log.info("Reattached admin membership for group {}", group.getId());
        }

        members.sort(
                Comparator.comparing((Membership m) -> !group.getAdminId().equals(m.getUserId()))
                        .thenComparing(
                                Membership::getJoinedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())));
        for (int i = 0; i < members.size(); i++) {
            Membership m = members.get(i);
            if (m.getPayoutOrder() == null || m.getPayoutOrder() != i + 1) {
                m.setPayoutOrder(i + 1);
                if (!changed.contains(m)) {
                    changed.add(m);
                }
            }
        }

        if (!changed.isEmpty()) {
            membershipRepository.saveAll(changed);
        }
    }
}
