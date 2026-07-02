package com.example.demo;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LoanApplicationService {

    @Autowired
    private LoanApplicationRepository loanRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BankServiceRepository bankServiceRepository;

    /**
     * Create a new banking application
     */
    public LoanApplication createApplication(Integer customerId,
                                             Integer serviceId) {

        User customer = userRepository.findById(customerId)
                .orElseThrow(() ->
                new RuntimeException("Customer not found"));

        BankService service = bankServiceRepository.findById(serviceId)
                .orElseThrow(() ->
                new RuntimeException("Bank Service not found"));

        LoanApplication application = new LoanApplication();

        application.setCustomer(customer);

        application.setBankService(service);

        application.setStatus("Pending Verification");

        application.setTrustScore(0);

        application.setCreatedAt(LocalDateTime.now());

        application.setUpdatedAt(LocalDateTime.now());

        application.setApplicationNumber(generateApplicationNumber());

        return loanRepository.save(application);
    }

    /**
     * Generate Application Number
     * Example:
     * HL202600001
     */
    private String generateApplicationNumber() {

        String year =
                String.valueOf(LocalDateTime.now().getYear());

        long count = loanRepository.count() + 1;

        return "APP" +
                year +
                String.format("%05d", count);
    }

    /**
     * Get Application by ID
     */
    public LoanApplication getApplication(Integer id) {

        return loanRepository.findById(id)
                .orElseThrow(() ->
                new RuntimeException("Application not found"));
    }
    public List<LoanApplication> getCustomerApplications(Integer customerId) {

        return loanRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId);

    }

}