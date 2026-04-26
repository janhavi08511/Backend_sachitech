package com.example.sachitech.repository;

import com.example.sachitech.entity.TrainerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrainerProfileRepository extends JpaRepository<TrainerProfile, Long> {
    Optional<TrainerProfile> findByUserId(Long userId);
}
