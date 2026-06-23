package com.example.demo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, Integer> {

    List<AuditLog> findByUserId(int userId);

    List<AuditLog> findByUsername(String username);

    List<AuditLog> findByUsernameOrderByTimestampDesc(
            String username);

    List<AuditLog> findByDocumentId(
            Integer documentId);

    List<AuditLog> findByDocumentName(
            String documentName);

    List<AuditLog> findTop10ByOrderByTimestampDesc();
}