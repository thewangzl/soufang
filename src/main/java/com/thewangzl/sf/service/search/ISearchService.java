package com.thewangzl.sf.service.search;

import java.util.List;

import com.thewangzl.sf.service.ServiceMultiResult;
import com.thewangzl.sf.service.ServiceResult;
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
	
	/**
	 * 获取自动补全关键词
	 * @param key
	 * @return
	 */
	ServiceResult<List<String>> suggest(String prefix);
}
