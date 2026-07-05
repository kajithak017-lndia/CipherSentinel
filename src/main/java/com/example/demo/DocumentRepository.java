package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Integer> {

    List<Document> findByUploadedBy(int uploadedBy);

    List<Document> findByUploadedByAndServiceType(int uploadedBy, String serviceType);

    List<Document> findByUploadedByOrderByUploadTimeDesc(int uploadedBy);
   
    List<Document> findByApplicationId(int applicationId);
    long countByApplicationId(Integer applicationId);
    List<Document> findByApplicationId(Integer applicationId);
    boolean existsByApplicationIdAndDocumentTypeIgnoreCase(Integer applicationId, String documentType);
}