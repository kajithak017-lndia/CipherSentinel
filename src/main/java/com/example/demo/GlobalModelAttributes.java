package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

	@Autowired
	private UserRepository userRepository;

	@ModelAttribute("profilePic")
	public String profilePic(Authentication auth) {

		if (auth == null) {
			return null;
		}
		User user = userRepository.findByUsername(auth.getName());

		return user != null ? user.getProfileImage() : null;
	}
}