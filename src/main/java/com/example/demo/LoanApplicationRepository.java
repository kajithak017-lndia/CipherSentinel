package com.example.demo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanApplicationRepository
        extends JpaRepository<LoanApplication,Integer>{

	List<LoanApplication> findByCustomer_IdOrderByCreatedAtDesc(Integer customerId);

}