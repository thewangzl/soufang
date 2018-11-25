package com.thewangzl.sf.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name="support_address")
public class SupportAddress {

	@Id
	private Long id;
	
	@Column(name="belong_to")
	private String belongTo;
	
	@Column(name="en_name")
	private String enName;
	
	@Column(name="cn_name")
	private String cnName;
	
	private String level;
	
	/**
	 * 行政级别定义
	 */
	
	public enum Level{
		CITY("city"),
		REGION("region");
		
		private String value;
		
		Level(String value){
			this.value = value;
		}
		
		public String getValue() {
			return value;
		}
		
		public static Level of(String value) {
			for(Level level : Level.values()) {
				if(level.equals(level.getValue())) {
					return  level;
				}
			}
			throw new IllegalArgumentException();
		}
	}
	
	
}
