package com.thewangzl.sf.service.search;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.index.reindex.DeleteByQueryRequestBuilder;
import org.elasticsearch.rest.RestStatus;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thewangzl.sf.domain.House;
import com.thewangzl.sf.domain.HouseDetail;
import com.thewangzl.sf.domain.HouseTag;
import com.thewangzl.sf.repository.HouseDetailRepository;
import com.thewangzl.sf.repository.HouseRepository;
import com.thewangzl.sf.repository.HouseTagRepository;

@Service
public class SearchServiceImpl implements ISearchService {
	
	private static final Logger logger = LoggerFactory.getLogger(ISearchService.class);
	
	private static final String INDEX_NAME = "soufang";
	
	private static final String INDEX_TYPE = "house";
	
	@Autowired
	private HouseRepository houseRepository;
	
	@Autowired
	private HouseDetailRepository houseDetailRepository;
	
	@Autowired
	private HouseTagRepository houseTagRepository;
	
	@Autowired
	private ModelMapper modelMapper;
	
	@Autowired
	private TransportClient esClient;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	
	@Override
	public boolean index(Long houseId) {
		House house = this.houseRepository.findOne(houseId);
		if(house == null) {
			logger.error("Index house {} dose not exist!", houseId);
			return false;
		}
		HouseIndexTemplate indexTemplate = this.modelMapper.map(house, HouseIndexTemplate.class);
		
		HouseDetail detail = this.houseDetailRepository.findByHouseId(houseId);
		if(detail == null) {
			//TODO
			return false;
		}
		modelMapper.map(detail, indexTemplate);
		
		List<HouseTag> houseTags = this.houseTagRepository.findAllByHouseId(houseId);
		if(houseTags != null && !houseTags.isEmpty()) {
			List<String> tags = new ArrayList<>();
			houseTags.forEach(tag -> {
				tags.add(tag.getName());
			});
			indexTemplate.setTags(tags);
		}
		
		SearchRequestBuilder requestBuilder = this.esClient.prepareSearch(INDEX_NAME).setTypes(INDEX_TYPE)	//
			.setQuery(QueryBuilders.termQuery(HouseIndexKey.HOUSE_ID, houseId));
		logger.debug(requestBuilder.toString());
		
		SearchResponse searchResponse = requestBuilder.get();
		long totalHit = searchResponse.getHits().getTotalHits();
		
		boolean success;
		if(totalHit == 0) {	//
			success = this.create(indexTemplate);
		}else if(totalHit == 1) {	//
			String esId = searchResponse.getHits().getAt(0).getId();
			success = this.update(esId, indexTemplate);
		}else {	//	出现多个说明出现问题，先删除，再增加
			success = this.deleteAndCreate(totalHit, indexTemplate);
		}
		
		if(success) {
			logger.debug("Index success with house: " + houseId);
		}
		return success;
	}
	
	private boolean create(HouseIndexTemplate indexTemplate) {
		try {
			IndexResponse indexResponse = this.esClient.prepareIndex(INDEX_NAME, INDEX_TYPE)//
				.setSource(objectMapper.writeValueAsBytes(indexTemplate), XContentType.JSON).get()
			;
			logger.debug("Create index with house: " + indexTemplate.getHouseId());
			return indexResponse.status() == RestStatus.CREATED;
		} catch (JsonProcessingException e) {
			logger.error("Error to index house " + indexTemplate.getHouseId(), e);
			return false;
		}
	}
	
	private boolean update(String esId,HouseIndexTemplate indexTemplate) {
		try {
			UpdateResponse response = this.esClient.prepareUpdate(INDEX_NAME, INDEX_TYPE, esId)//
				.setDoc(objectMapper.writeValueAsBytes(indexTemplate), XContentType.JSON).get()
			;
			logger.debug("Update index with house: " + indexTemplate.getHouseId());
			return response.status() == RestStatus.OK;
		} catch (JsonProcessingException e) {
			logger.error("Error to update index house " + indexTemplate.getHouseId(), e);
			return false;
		}
	}
	
	private boolean deleteAndCreate(long totalHit,HouseIndexTemplate indexTemplate) {
		DeleteByQueryRequestBuilder builder = DeleteByQueryAction.INSTANCE.newRequestBuilder(this.esClient)//
			.filter(QueryBuilders.termQuery(HouseIndexKey.HOUSE_ID, indexTemplate.getHouseId()))//
			.source(INDEX_NAME);
		
		logger.debug("Delete by query for house: " + builder);
		
		BulkByScrollResponse response = builder.get();
		long deleted = response.getDeleted();
		if(deleted != totalHit) {
			logger.warn("Need delete {}, but {} was deleted", totalHit, deleted);
			return false;
		}
		return this.create(indexTemplate);
	}
	

	@Override
	public void remove(Long houseId) {
		DeleteByQueryRequestBuilder builder = DeleteByQueryAction.INSTANCE.newRequestBuilder(this.esClient)//
				.filter(QueryBuilders.termQuery(HouseIndexKey.HOUSE_ID, houseId))//
				.source(INDEX_NAME);
			
		logger.debug("Delete by query for house: " + builder);
		
		BulkByScrollResponse response = builder.get();
		long deleted = response.getDeleted();
		logger.debug("Delete total {}, with house: {} ", deleted, houseId);
	}

}
