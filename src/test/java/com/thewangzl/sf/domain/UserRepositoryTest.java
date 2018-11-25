package com.thewangzl.sf.domain;

import java.util.Optional;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.thewangzl.sf.ApplicationTests;
import com.thewangzl.sf.repository.UserRepository;

import junit.framework.Assert;

public class UserRepositoryTest extends ApplicationTests {

	@Autowired
	private UserRepository userRepository;
	
	@Test
	public void testFindOne() {
		User user = userRepository.findOne(1L);
		Assert.assertEquals("wali", user.getName());
	}
}
