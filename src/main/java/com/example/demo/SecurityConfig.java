package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
	@Autowired
	private AuditLogRepository auditLogRepository;

	@Autowired
	private UserRepository userRepository;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(auth -> auth
				.requestMatchers("/login", "/register", "/forgot-password", "/css/**", "/js/**", "/profile-images/**")
				.permitAll().requestMatchers("/admin/**").hasAuthority("ADMIN")
				.requestMatchers("/audit", "/export-audit").hasAnyAuthority("ADMIN", "MANAGER", "AUDITOR")
				.requestMatchers("/download-report/**").hasAnyAuthority("ADMIN", "MANAGER", "OFFICER").anyRequest()
				.authenticated())
				.formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/dashboard", true).permitAll())
				.exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"))
				.logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout")
						.addLogoutHandler((request, response, auth) -> {
							if (auth != null) {
								try {
									User user = userRepository.findByUsername(auth.getName());
									AuditLog log = new AuditLog();
									log.setUserId(user != null ? user.getId() : 0);
									log.setUsername(auth.getName());
									log.setUserRole(user != null ? user.getRole() : "USER");
									log.setAction("USER_LOGOUT");
									log.setDetails("User logged out");
									log.setIpAddress(request.getRemoteAddr());
									auditLogRepository.save(log);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}).invalidateHttpSession(true).clearAuthentication(true).permitAll())

				// ✅ Session management
				.sessionManagement(session -> session.maximumSessions(1).expiredUrl("/login?expired"))

				.csrf(csrf -> csrf.ignoringRequestMatchers("/upload", "/register", "/delete/**", "/rescan/**",
						"/profile/upload-image", "/profile/remove-photo", "/forgot-password", "/admin/change-role",
						"/admin/delete-user"));

		return http.build();
	}
}