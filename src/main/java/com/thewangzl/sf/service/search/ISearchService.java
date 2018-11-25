package com.thewangzl.sf.service.search;

/**
 * 检索接口
 * @author thewangzl
 *
 */
public interface ISearchService {

	/**
	 * 索引目标房源
	 * @param houseId
	 * @return 
	 */
	boolean index(Long houseId);
	
	/**
	 * 移除房源索引
	 * @param houseId
	 */
	void remove(Long houseId);
}
