package com.thewangzl.sf.base;

import org.springframework.security.core.context.SecurityContextHolder;

import com.thewangzl.sf.domain.User;

public class LoginUserUtil {

	public static User load() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if(principal != null && principal instanceof User) {
			return (User) principal;
		}
		return null;
	}
	
	public static Long getLoginUserId() {
		User user = load();
		if(user != null) {
			return user.getId();
		}
		return -1L;
	}
}

