package com.thewangzl.sf.service;

import com.thewangzl.sf.domain.User;
import com.thewangzl.sf.web.controller.dto.UserDTO;

public interface IUserService {

	User findByName(String userName);
	
	ServiceResult<UserDTO> findById(Long id);
}
