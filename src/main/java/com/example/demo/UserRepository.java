package com.example.demo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository 
    extends JpaRepository<User, Integer> {
	List<User> findByRole(String role);
    User findByUsername(String username);
    User findByEmail(String email);
}