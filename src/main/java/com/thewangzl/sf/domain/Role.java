package com.thewangzl.sf.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name="role")
public class Role {

	@Id
	private Long id;
	
	private String name;
	
	@Column(name="USER_ID")
	private Long userId;
}
