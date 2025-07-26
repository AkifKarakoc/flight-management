package com.flightmanagement.reference.controller;

import com.flightmanagement.reference.dto.response.PagedResponse;
import com.flightmanagement.reference.dto.response.UserResponseDto;
import com.flightmanagement.reference.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<UserResponseDto>> getPendingUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<UserResponseDto> response = userService.getPendingUsers(pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDto> approveUser(@PathVariable Long id) {
        UserResponseDto response = userService.approveUser(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> rejectUser(@PathVariable Long id) {
        userService.rejectUser(id);
        return ResponseEntity.ok("User rejected successfully");
    }
}