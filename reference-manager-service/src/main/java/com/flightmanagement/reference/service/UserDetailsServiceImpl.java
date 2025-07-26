package com.flightmanagement.reference.service;

import com.flightmanagement.reference.entity.User;
import com.flightmanagement.reference.repository.UserRepository;
import com.flightmanagement.reference.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("=== Loading user: " + username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        System.out.println("=== User found: " + user.getUsername());
        System.out.println("=== User active: " + user.getIsActive());
        System.out.println("=== User approved: " + user.getIsApproved());

        if (!user.getIsActive() || !user.getIsApproved()) {
            throw new UsernameNotFoundException("User not active or approved: " + username);
        }

        return UserPrincipal.create(user);
    }
}