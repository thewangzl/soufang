package com.thewangzl.sf.base;

public enum HouseStatus {

	NOT_AUDITED(0), 	// 未审核
	PASSED(1),	//审核通过
	RENTED(2), 		//已出租
	DELETED(3),		//已删除
	;
	
	private int value;
	
	private HouseStatus(int value) {
		this.value = value;
	}



	public int getValue() {
		return value;
	}
}
