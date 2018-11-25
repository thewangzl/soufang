package com.thewangzl.sf.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.thewangzl.sf.domain.User;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {

	User findByName(String userName);
}
