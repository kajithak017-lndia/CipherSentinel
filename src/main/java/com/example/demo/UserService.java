package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

		User user = userRepository.findByEmail(email);

		if (user == null) {
			throw new UsernameNotFoundException("User not found: " + email);
		}

		// ✅ Block deactivated accounts
		if ("DEACTIVATED".equals(user.getRole())) {
			throw new UsernameNotFoundException("Account is deactivated!");
		}

		return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
				.password(user.getPassword()).authorities(user.getRole()).build();
	}

	public void saveUser(User user) {
		user.setPassword(passwordEncoder.encode(user.getPassword()));
		user.setRole("USER");
		user.setAnomalyAlerts(true);
		user.setAuditNotifications(true);
		user.setUploadNotifications(true);
		user.setTwoFactorAuth(false);

		userRepository.save(user);

	}
}