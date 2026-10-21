package com.thrift.thriftsystem.controller;

import com.thrift.thriftsystem.dto.request.CreateGroupRequest;
import com.thrift.thriftsystem.dto.response.ApiResponse;
import com.thrift.thriftsystem.dto.response.GroupResponse;
import com.thrift.thriftsystem.service.ThriftGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/group")
@RequiredArgsConstructor
public class ThriftGroupController {

    private final ThriftGroupService thriftGroupService;

    @PostMapping("/create-group")
    public ResponseEntity<ApiResponse> createGroup(
            @RequestBody @Valid CreateGroupRequest createGroupRequest) {
        GroupResponse response = thriftGroupService.createGroup(createGroupRequest);
        return new ResponseEntity<>(
                ApiResponse.success("Group successfully created", response), HttpStatus.OK);
    }

    @GetMapping("/my-groups")
    public ResponseEntity<ApiResponse> getMyGroups() {
        return new ResponseEntity<>(
                ApiResponse.success("Your groups has been found", thriftGroupService.getMyGroups()),
                HttpStatus.OK);
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse> getGroupById(@PathVariable String id) {
        GroupResponse response = thriftGroupService.getGroupById(id);
        return new ResponseEntity<>(
                ApiResponse.success("Group with id found", response), HttpStatus.OK);
    }

    @PostMapping("/{groupId}/members/{userId}")
    public ResponseEntity<ApiResponse> addMember(
            @PathVariable String groupId, @PathVariable String userId) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        "Member added successfully", thriftGroupService.addMember(groupId, userId)),
                HttpStatus.OK);
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse> getAllGroups() {
        return new ResponseEntity<>(
                ApiResponse.success("All groups has been found", thriftGroupService.getAllGroups()),
                HttpStatus.OK);
    }
}
