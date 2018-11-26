package com.thewangzl.sf.service.search;

import com.thewangzl.sf.service.ServiceMultiResult;
import com.thewangzl.sf.web.controller.form.RentSearch;

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
	void index(Long houseId);
	
	/**
	 * 移除房源索引
	 * @param houseId
	 */
	void remove(Long houseId);
	
	ServiceMultiResult<Long> query(RentSearch rentSearch);
}
