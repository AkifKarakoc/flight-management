package com.flightmanagement.reference.service;

import com.flightmanagement.reference.dto.response.PagedResponse;
import com.flightmanagement.reference.dto.response.UserResponseDto;
import com.flightmanagement.reference.entity.User;
import com.flightmanagement.reference.exception.ReferenceNotFoundException;
import com.flightmanagement.reference.mapper.UserMapper;
import com.flightmanagement.reference.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public PagedResponse<UserResponseDto> getPendingUsers(Pageable pageable) {
        Page<User> users = userRepository.findByApprovalStatus(false, pageable);
        return createPagedResponse(users);
    }

    public UserResponseDto approveUser(Long userId) {
        User user = getUserById(userId);
        user.setIsApproved(true);
        user.setIsActive(true);
        userRepository.save(user);
        return userMapper.toResponseDto(user);
    }

    public void rejectUser(Long userId) {
        User user = getUserById(userId);
        userRepository.delete(user);
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ReferenceNotFoundException("User not found with id: " + userId));
    }

    private PagedResponse<UserResponseDto> createPagedResponse(Page<User> page) {
        return PagedResponse.<UserResponseDto>builder()
                .content(page.getContent().stream().map(userMapper::toResponseDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}