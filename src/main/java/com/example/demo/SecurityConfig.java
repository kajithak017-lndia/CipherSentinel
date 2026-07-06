package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
public class SecurityConfig {

	@Autowired
	private AuditLogService auditLogService;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(auth -> auth
				.requestMatchers("/login", "/register", "/forgot-password", "/css/**", "/js/**", "/profile-images/**")
				.permitAll()

				// Admin dashboard — Admin only
				.requestMatchers("/admin/**").hasAuthority("ADMIN")

				// Officer's claim/review dashboard — Officer only
				.requestMatchers("/officer/**").hasAuthority("OFFICER")

				// Manager's claim/review dashboard — Manager only
				.requestMatchers("/manager/**").hasAuthority("MANAGER")

				// Audit trail — visible to Admin and Manager
				.requestMatchers("/audit", "/export-audit").hasAnyAuthority("ADMIN", "MANAGER")

				.requestMatchers("/download-report/**").hasAnyAuthority("ADMIN", "MANAGER", "OFFICER")

				// ✅ Regular customer-facing pages — open to EVERYONE who's logged in
				// (USER, OFFICER, MANAGER, ADMIN can all apply, upload, view documents, etc.)
				.requestMatchers("/dashboard", "/apply", "/upload", "/documents", "/anomalies")
				.authenticated()

				.anyRequest().authenticated()
		)
		.formLogin(form -> form
				.loginPage("/login")
				.successHandler(customSuccessHandler())
				.permitAll()
		)
		.exceptionHandling(ex -> ex
				.accessDeniedHandler((request, response, accessDeniedException) -> {
					try {
						String username = request.getUserPrincipal() != null
								? request.getUserPrincipal().getName()
								: "anonymous";
						auditLogService.log(username, "ACCESS_DENIED",
								"Access denied to " + request.getRequestURI() + " | "
										+ accessDeniedException.getMessage(),
								request);
					} catch (Exception e) {
						e.printStackTrace();
					}

					// Any authenticated user hitting a page they don't have the
					// role/permission for — /download-report, /admin, /officer,
					// /manager, /audit, etc. — is sent to the Access Denied page.
					response.sendRedirect(request.getContextPath() + "/access-denied");
				})
		)
		.logout(logout -> logout
				.logoutUrl("/logout")
				.logoutSuccessUrl("/login?logout")
				.addLogoutHandler((request, response, auth) -> {
					if (auth != null) {
						try {
							auditLogService.log(auth.getName(), "LOGOUT", "User logged out", request);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				})
				.invalidateHttpSession(true)
				.clearAuthentication(true)
				.permitAll()
		)
		.sessionManagement(session -> session
				.maximumSessions(1)
				.expiredUrl("/login?expired")
		)
		.csrf(csrf -> csrf.ignoringRequestMatchers(
				"/upload", "/register", "/delete/**", "/rescan/**",
				"/profile/upload-image", "/profile/remove-photo", "/forgot-password",
				"/admin/change-role", "/admin/delete-user"
		));

		return http.build();
	}

	@Bean
	public AuthenticationSuccessHandler customSuccessHandler() {
		return (request, response, authentication) -> {
			boolean isOfficer = authentication.getAuthorities().stream()
					.anyMatch(a -> a.getAuthority().equals("OFFICER"));
			boolean isManager = authentication.getAuthorities().stream()
					.anyMatch(a -> a.getAuthority().equals("MANAGER"));

			if (isOfficer) {
				response.sendRedirect(request.getContextPath() + "/officer");
			} else if (isManager) {
				response.sendRedirect(request.getContextPath() + "/manager");
			} else {
				response.sendRedirect(request.getContextPath() + "/dashboard");
			}
		};
	}
}