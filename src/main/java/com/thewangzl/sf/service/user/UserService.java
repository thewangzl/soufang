package com.thewangzl.sf.service.user;

import java.util.ArrayList;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import com.thewangzl.sf.domain.Role;
import com.thewangzl.sf.domain.User;
import com.thewangzl.sf.repository.RoleRepository;
import com.thewangzl.sf.repository.UserRepository;
import com.thewangzl.sf.service.IUserService;
import com.thewangzl.sf.service.ServiceResult;
import com.thewangzl.sf.web.controller.dto.UserDTO;

@Service
public class UserService implements IUserService {

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private RoleRepository roleRepository;
	
	@Autowired
	private ModelMapper modelMapper;
	
	@Override
	public User findByName(String userName) {

		User user = userRepository.findByName(userName);
		if(user != null) {
			List<Role> roles = roleRepository.findRolesByUserId(user.getId());
			if(roles == null) {
				throw new DisabledException("没有权限");
			}
			
			List<GrantedAuthority> authorities = new ArrayList<>();
			roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_"+ role.getName())));
			
			user.setAuthorityList(authorities);
		}
		
		return user;
	}

	@Override
	public ServiceResult<UserDTO> findById(Long id) {
		User user = this.userRepository.findOne(id);
		if (user == null) {
			return ServiceResult.<UserDTO> notFound();
		}
		UserDTO userDTO = modelMapper.map(user, UserDTO.class);
		return ServiceResult.of(userDTO);
	}

}
