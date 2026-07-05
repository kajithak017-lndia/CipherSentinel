package com.example.demo;

import java.util.List;

public class ApplicationWithDocuments {

    private final LoanApplication application;
    private final List<Document> documents;

    public ApplicationWithDocuments(LoanApplication application, List<Document> documents) {
        this.application = application;
        this.documents = documents;
    }

    public LoanApplication getApplication() { return application; }
    public List<Document> getDocuments() { return documents; }
}