package com.thrift.thriftsystem.util;

import com.thrift.thriftsystem.dto.request.CreateGroupRequest;
import com.thrift.thriftsystem.dto.response.GroupResponse;
import com.thrift.thriftsystem.enums.GroupStatus;
import com.thrift.thriftsystem.model.ThriftGroup;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class GroupMapper {



    public ThriftGroup toModel(CreateGroupRequest request,String adminId){

        return ThriftGroup.builder()
                .name(request.getName())
                .description(request.getDescription())
                .adminId(adminId)
                .contributionAmount(request.getContributionAmount())
                .currency(request.getCurrency())
                .frequency(request.getFrequency())
                .startDate(request.getStartDate() !=null? request.getStartDate():LocalDateTime.now())
                .totalMember(request.getTotalMembers())
                .totalCycles(request.getTotalMembers())
                .currentCycle(1)
                .status(GroupStatus.ACTIVE)
                .build();

    }

    public GroupResponse toResponse(ThriftGroup group){
        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .adminId(group.getAdminId())
                .contributionAmount(group.getContributionAmount())
                .currency(group.getCurrency())
                .status(group.getStatus())
                .totalMembers(group.getTotalMember())
                .totalCycles(group.getTotalCycles())
                .startDate(group.getStartDate())
                .endDate(group.getEndDate())
                .frequency(group.getFrequency())
                .memberIds(group.getMemberIds())
                .createdAt(group.getCreatedDate())
                .build();
    }
}
