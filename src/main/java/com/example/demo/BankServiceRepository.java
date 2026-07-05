package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BankServiceRepository extends JpaRepository<BankService, Integer> {
}