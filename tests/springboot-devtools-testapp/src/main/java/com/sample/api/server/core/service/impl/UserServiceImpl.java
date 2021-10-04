package com.sample.api.server.core.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.sample.api.server.core.dto.UserDTO;
import com.sample.api.server.core.service.UserService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
	private final List<UserDTO> list = new ArrayList<>();

	static {
		System.out.println("UserServiceImpl loaded");
	}

	@Async
	@Override
	public void save(final UserDTO user) {
		user.setUserId(list.size() + 1);
		list.add(user);

		log.info("Added successfully.");
	}

	@Override
	@Cacheable("userCache")
	public UserDTO getById(final Integer userId) {
		return list.stream().filter(user -> user.getUserId() == userId).findFirst().get();
	}

}
