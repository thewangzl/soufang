package com.thewangzl.sf.service;

import java.util.List;

import lombok.Data;

@Data
public class ServiceMultiResult<T> {

	private long total;
	
	private List<T> result;
	
	
	public ServiceMultiResult(long total, List<T> result) {
		super();
		this.total = total;
		this.result = result;
	}



	public ServiceMultiResult() {
		super();
	}
	
	public int getResultSize() {
		return this.result.size();
	}
}
