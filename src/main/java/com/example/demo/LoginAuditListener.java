package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;
@Component
public class LoginAuditListener {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    // ===== LOGIN SUCCESS =====
    @EventListener
    public void onLoginSuccess(
        AuthenticationSuccessEvent event) {

        try {
            Authentication auth =
                event.getAuthentication();
            String username = auth.getName();

            User user = userRepository
                .findByUsername(username);
            
            String ip = "unknown";
            if (auth.getDetails() instanceof
                WebAuthenticationDetails) {
                ip = ((WebAuthenticationDetails)
                    auth.getDetails())
                    .getRemoteAddress();
            }

            AuditLog log = new AuditLog();
            log.setUserId(
                user != null ? user.getId() : 0);
            log.setUsername(username);
            log.setUserRole(
                user != null ? user.getRole() : "USER");
            log.setAction("LOGIN_SUCCESS");
            log.setDetails(
                "User logged in successfully");
            log.setIpAddress(ip);

            auditLogRepository.save(log);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== LOGIN FAILURE =====
    @EventListener
    public void onLoginFailure(
        AbstractAuthenticationFailureEvent event) {

        try {
            String username = event
                .getAuthentication()
                .getName();

            AuditLog log = new AuditLog();
            log.setUserId(0);
            log.setUsername(username);
            log.setUserRole("UNKNOWN");
            log.setAction("LOGIN_FAILED");
            log.setDetails(
                "Failed login attempt for: "
                + username);

            auditLogRepository.save(log);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}