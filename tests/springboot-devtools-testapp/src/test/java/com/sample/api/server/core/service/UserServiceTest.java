package com.sample.api.server.core.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.sample.api.server.core.dto.UserDTO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class UserServiceTest extends AbstractTestCase {
	
	@Autowired
	private UserService userServices;

	@Test
	void getById() {
		UserDTO byId = userServices.getById(1);
		log.info("Result: {}", byId);
	}

}
