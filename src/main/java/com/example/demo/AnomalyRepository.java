package com.example.demo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnomalyRepository
        extends JpaRepository<Anomaly, Integer> {

    List<Anomaly> findByDocumentId(int documentId);

    void deleteByDocumentId(int documentId);
}