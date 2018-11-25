package com.thewangzl.sf.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.thewangzl.sf.domain.Role;

@Repository
public interface RoleRepository extends CrudRepository<Role, Long> {

	List<Role> findRolesByUserId(Long userId);
	
}
