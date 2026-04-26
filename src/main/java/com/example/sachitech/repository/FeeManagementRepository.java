package com.example.sachitech.repository;

import com.example.sachitech.entity.FeeManagement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FeeManagementRepository extends JpaRepository<FeeManagement, Long> {
    List<FeeManagement> findByStudent_User_Name(String studentName);
    List<FeeManagement> findByStudentId(Long studentId);
}
