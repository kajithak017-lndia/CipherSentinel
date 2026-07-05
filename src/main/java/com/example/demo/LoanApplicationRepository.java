package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Integer> {
    List<LoanApplication> findByCustomerId(int customerId);
    List<LoanApplication> findByOfficerId(int officerId);
    List<LoanApplication> findByManagerId(int managerId);

    List<LoanApplication> findByStatusAndOfficerIsNull(String status);
    List<LoanApplication> findByOfficerIdAndStatus(int officerId, String status);

    List<LoanApplication> findByStatusAndManagerIsNull(String status);
    List<LoanApplication> findByManagerIdAndStatus(int managerId, String status);
}