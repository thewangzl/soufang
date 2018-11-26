
package com.thewangzl.sf.service.search;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.thewangzl.sf.ApplicationTests;
import com.thewangzl.sf.service.ServiceMultiResult;
import com.thewangzl.sf.web.controller.form.RentSearch;

public class SearchServiceTests extends ApplicationTests {

	@Autowired
	private ISearchService searchService;
	
	@Test
	public void testIndex() {
		Long targetId = 15L;
		this.searchService.index(targetId);
	}
	
	@Test 
	public void testRemove() {
		Long targetId = 15L;
		this.searchService.remove(targetId);
	}
	
	@Test
	public void testQuery() {
		RentSearch rentSearch = new RentSearch();
		rentSearch.setCityEnName("bj");
		rentSearch.setStart(0);
		rentSearch.setSize(10);
		ServiceMultiResult<Long> result = this.searchService.query(rentSearch);
		System.out.println(result.getResultSize());		
		Assert.assertEquals(10, result.getResultSize());
	}
}
