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
	private List<UserDTO> list = new ArrayList<>();

	@Async
	@Override
	public void save(UserDTO user) {
		user.setUserId(list.size() + 1);
		list.add(user);
		
		log.info("Added successfully.");
	}

	@Override
	@Cacheable("userCache")
	public UserDTO getById(Integer userId) {
		return list.stream().filter(user -> user.getUserId() == userId).findFirst().get();
	}
	
}
