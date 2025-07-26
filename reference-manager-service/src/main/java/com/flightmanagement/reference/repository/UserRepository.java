package com.flightmanagement.reference.repository;

import com.flightmanagement.reference.entity.User;
import com.flightmanagement.reference.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.isApproved = :approved ORDER BY u.createdAt DESC")
    Page<User> findByApprovalStatus(Boolean approved, Pageable pageable);

    Page<User> findByRole(UserRole role, Pageable pageable);

    Page<User> findByAirlineId(Long airlineId, Pageable pageable);
}