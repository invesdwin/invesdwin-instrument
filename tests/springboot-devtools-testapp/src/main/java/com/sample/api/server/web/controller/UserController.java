package com.sample.api.server.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sample.api.server.core.dto.UserDTO;
import com.sample.api.server.core.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {
	
	@Autowired
	private UserService userService;
	
	@PostMapping
	public UserDTO addUser(@RequestBody UserDTO user) {
		userService.save(user);
		return user;
	}
	
	@GetMapping("/{userId}")
	public UserDTO addUser(@PathVariable Integer userId) {
		return userService.getById(userId);
	}
}
