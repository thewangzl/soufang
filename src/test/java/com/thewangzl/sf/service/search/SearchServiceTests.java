
package com.thewangzl.sf.service.search;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.thewangzl.sf.ApplicationTests;

import junit.framework.Assert;

public class SearchServiceTests extends ApplicationTests {

	@Autowired
	private ISearchService searchService;
	
	@Test
	public void testIndex() {
		Long targetId = 15L;
		boolean success = this.searchService.index(targetId);
		Assert.assertEquals(true, success);
	}
	
	@Test 
	public void testRemove() {
		Long targetId = 15L;
		this.searchService.remove(targetId);
	}
}
