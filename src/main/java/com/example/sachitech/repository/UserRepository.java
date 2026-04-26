package com.example.sachitech.repository;

import com.example.sachitech.entity.Role;
import com.example.sachitech.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    /** Count users by role (used for totalStudents, totalTrainers, etc.) */
    long countByRole(Role role);
}
