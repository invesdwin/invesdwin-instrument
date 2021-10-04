package com.sample.api.server.core.service;

import com.sample.api.server.core.dto.UserDTO;

public interface UserService {

	void save(UserDTO user);
	
	UserDTO getById(Integer userId);
}
