package com.thewangzl.sf.web.controller.form;

import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

@Data
public class DatatableSearch {

	/**
	 * Datatables要求回显字段
	 */
	private int draw;
	
	/**
	 * Datatables规定的分页字段
	 */
	private int start;
	
	private int length;
	
	private Integer status;
	
	@DateTimeFormat(pattern="yyyy-MM-dd")
	private Date createTimeMin;
	
	@DateTimeFormat(pattern="yyyy-MM-dd")
	private Date createTimeMax;
	
	private String city;
	private String title;
	
	private String direction;
	
	private String orderBy;
}
