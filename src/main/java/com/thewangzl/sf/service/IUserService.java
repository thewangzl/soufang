package com.thewangzl.sf.service;

import com.thewangzl.sf.domain.User;

public interface IUserService {

	User findByName(String userName);
	
}
