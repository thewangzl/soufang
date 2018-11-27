package com.thewangzl.sf.service.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.elasticsearch.action.admin.indices.analyze.AnalyzeAction;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeRequestBuilder;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse.AnalyzeToken;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.index.reindex.DeleteByQueryRequestBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest.Suggestion;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import com.thewangzl.sf.base.HouseSort;
import com.thewangzl.sf.base.RentValueBlock;
import com.thewangzl.sf.domain.House;
import com.thewangzl.sf.domain.HouseDetail;
import com.thewangzl.sf.domain.HouseTag;
import com.thewangzl.sf.repository.HouseDetailRepository;
import com.thewangzl.sf.repository.HouseRepository;
import com.thewangzl.sf.repository.HouseTagRepository;
import com.thewangzl.sf.service.ServiceMultiResult;
import com.thewangzl.sf.service.ServiceResult;
import com.thewangzl.sf.web.controller.form.RentSearch;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SearchServiceImpl implements ISearchService {
	
	
	private static final String INDEX_NAME = "soufang";
	
	private static final String INDEX_TYPE = "house";
	
	private static final String INDEX_TOPIC = "house-build";
	
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
	
	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;
	
	@KafkaListener(topics=INDEX_TOPIC)	
	private void handleContent(String content) {

		try {
			HouseIndexMessage indexMessage = this.objectMapper.readValue(content, HouseIndexMessage.class);
			switch (indexMessage.getOperation()) {
			case HouseIndexMessage.INDEX:
				this.createOrUpdateIndex(indexMessage);
				break;
			case HouseIndexMessage.REMOVE:
				this.removeIndex(indexMessage);
				break;
			default:
				log.warn("Not support Message content: "+ content);
				break;
			}
		} catch (IOException e) {
			log.error("Cannot parse json for " + content, e);
		}
	}
	
	private void createOrUpdateIndex(HouseIndexMessage message) {
		Long houseId = message.getHouseId();
		
		House house = this.houseRepository.findOne(houseId);
		if(house == null) {
			log.error("Index house {} dose not exist!", houseId);
			this.index(houseId, message.getRetry() + 1);
		}
		HouseIndexTemplate indexTemplate = this.modelMapper.map(house, HouseIndexTemplate.class);
		
		HouseDetail detail = this.houseDetailRepository.findByHouseId(houseId);
		if(detail == null) {
			//TODO
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
		log.debug(requestBuilder.toString());
		
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
			log.debug("Index success with house: " + houseId);
		}
	}
	
	private void removeIndex(HouseIndexMessage message) {
		Long houseId = message.getHouseId();
		DeleteByQueryRequestBuilder builder = DeleteByQueryAction.INSTANCE.newRequestBuilder(this.esClient)//
				.filter(QueryBuilders.termQuery(HouseIndexKey.HOUSE_ID, houseId))//
				.source(INDEX_NAME);
			
		log.debug("Delete by query for house: " + builder);
		
		BulkByScrollResponse response = builder.get();
		long deleted = response.getDeleted();
		log.debug("Delete total {}, with house: {} ", deleted, houseId);
		
		if(deleted < 0) {
			this.remove(houseId, message.getRetry() + 1);
		}
	}
	
	@Override
	public void index(Long houseId) {
		this.index(houseId, 0);
	}
	
	private void index(Long houseId, int retry) {
		if(retry > HouseIndexMessage.MAX_RETRY) {
			log.error("Retry index times over {} for house: {}", HouseIndexMessage.MAX_RETRY, houseId);
		}
		
		HouseIndexMessage message = new HouseIndexMessage(houseId, HouseIndexMessage.INDEX, retry);
		try {
			kafkaTemplate.send(INDEX_TOPIC, this.objectMapper.writeValueAsString(message));
		} catch (JsonProcessingException e) {
			log.error("Json encode error for " + message);
		}
	}
	
	private boolean create(HouseIndexTemplate indexTemplate) {
		if(!this.updateSuggest(indexTemplate)) {
			return false;
		}
		try {
			IndexResponse indexResponse = this.esClient.prepareIndex(INDEX_NAME, INDEX_TYPE)//
				.setSource(objectMapper.writeValueAsBytes(indexTemplate), XContentType.JSON).get()
			;
			log.debug("Create index with house: " + indexTemplate.getHouseId());
			return indexResponse.status() == RestStatus.CREATED;
		} catch (JsonProcessingException e) {
			log.error("Error to index house " + indexTemplate.getHouseId(), e);
			return false;
		}
	}
	
	private boolean update(String esId,HouseIndexTemplate indexTemplate) {
		if(!this.updateSuggest(indexTemplate)) {
			return false;
		}
		try {
			UpdateResponse response = this.esClient.prepareUpdate(INDEX_NAME, INDEX_TYPE, esId)//
				.setDoc(objectMapper.writeValueAsBytes(indexTemplate), XContentType.JSON).get()
			;
			log.debug("Update index with house: " + indexTemplate.getHouseId());
			return response.status() == RestStatus.OK;
		} catch (JsonProcessingException e) {
			log.error("Error to update index house " + indexTemplate.getHouseId(), e);
			return false;
		}
	}
	
	private boolean deleteAndCreate(long totalHit,HouseIndexTemplate indexTemplate) {
		DeleteByQueryRequestBuilder builder = DeleteByQueryAction.INSTANCE.newRequestBuilder(this.esClient)//
			.filter(QueryBuilders.termQuery(HouseIndexKey.HOUSE_ID, indexTemplate.getHouseId()))//
			.source(INDEX_NAME);
		
		log.debug("Delete by query for house: " + builder);
		
		BulkByScrollResponse response = builder.get();
		long deleted = response.getDeleted();
		if(deleted != totalHit) {
			log.warn("Need delete {}, but {} was deleted", totalHit, deleted);
			return false;
		}
		return this.create(indexTemplate);
	}
	

	@Override
	public void remove(Long houseId) {
		this.remove(houseId, 0);
	}

	private void remove(Long houseId, int retry) {
		if(retry > HouseIndexMessage.MAX_RETRY) {
			log.error("Retry remove index times over {} for house: {}", HouseIndexMessage.MAX_RETRY, houseId);
		}
		
		HouseIndexMessage message = new HouseIndexMessage(houseId, HouseIndexMessage.REMOVE, retry);
		try {
			kafkaTemplate.send(INDEX_TOPIC, this.objectMapper.writeValueAsString(message));
		} catch (JsonProcessingException e) {
			log.error("Json encode error for " + message);
		}
	}

	@Override
	public ServiceMultiResult<Long> query(RentSearch rentSearch) {
		BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
		boolQueryBuilder.filter(
			QueryBuilders.termQuery(HouseIndexKey.CITY_EN_NAME, rentSearch.getCityEnName())
		);
		if(rentSearch.getRegionEnName() != null && !rentSearch.getRegionEnName().equals("*")) {
			boolQueryBuilder.filter(
					QueryBuilders.termQuery(HouseIndexKey.REGION_EN_NAME, rentSearch.getRegionEnName())
			);
		}
		RentValueBlock area = RentValueBlock.matchArea(rentSearch.getAreaBlock());
		if(!area.equals(RentValueBlock.ALL)) {
			RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(HouseIndexKey.AREA);
			if(area.getMin() > 0) {
				rangeQuery.gte(area.getMin());
			}
			if(area.getMax() > 0) {
				rangeQuery.lte(area.getMax());
			}
			boolQueryBuilder.filter(rangeQuery);
		}
		
		RentValueBlock price = RentValueBlock.matchPrice(rentSearch.getPriceBlock());
		if(!price.equals(RentValueBlock.ALL)) {
			RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(HouseIndexKey.PRICE);
			if(price.getMin() > 0) {
				rangeQuery.gte(price.getMin());
			}
			if(price.getMax() > 0) {
				rangeQuery.lte(price.getMax());
			}
			boolQueryBuilder.filter(rangeQuery);
		}
		
		if(rentSearch.getDirection() > 0) {
			boolQueryBuilder.filter(
					QueryBuilders.termQuery(HouseIndexKey.DIRECTION, rentSearch.getDirection())
					);
		}
		if(rentSearch.getRentWay() > -1) {
			boolQueryBuilder.filter(
					QueryBuilders.termQuery(HouseIndexKey.RENT_WAY, rentSearch.getRentWay())
				);
		}
		
		boolQueryBuilder.should(
				QueryBuilders.multiMatchQuery(rentSearch.getKeywords(), HouseIndexKey.TITLE)
				.boost(2.0f)//权重
				);
		
		boolQueryBuilder.should(QueryBuilders.multiMatchQuery(rentSearch.getKeywords(), 
				HouseIndexKey.TRAFFIC,
				HouseIndexKey.DISTRICT,
				HouseIndexKey.ROUND_SERVICE,
				HouseIndexKey.SUBWAY_LINE_NAME,
				HouseIndexKey.SUBWAY_STATION_NAME
				));
		
		SearchRequestBuilder requestBuilder = this.esClient.prepareSearch(INDEX_NAME)//
				.setTypes(INDEX_TYPE) //
				.setQuery(boolQueryBuilder)//
				.addSort(
						HouseSort.getSortKey(rentSearch.getOrderBy()),
						SortOrder.fromString(rentSearch.getOrderDirection())
				)//
				.setFrom(rentSearch.getStart())//
				.setSize(rentSearch.getSize())//
				.setFetchSource(HouseIndexKey.HOUSE_ID, null)	//只返回houseId
		;
		log.debug(requestBuilder.toString());
		
		List<Long> houseIds = new ArrayList<>();
		SearchResponse response = requestBuilder.get();
		if(response.status() != RestStatus.OK) {
			log.warn("Search status is not ok for " + requestBuilder);
			return new ServiceMultiResult<>(0,houseIds);
		}
		response.getHits().forEach(hit -> {
			houseIds.add(Longs.tryParse(String.valueOf(hit.getSource().get(HouseIndexKey.HOUSE_ID))));
		});
		return new ServiceMultiResult<>(response.getHits().getTotalHits(), houseIds);
	}

	@Override
	public ServiceResult<List<String>> suggest(String prefix) {
		CompletionSuggestionBuilder suggestionBuilder = SuggestBuilders.completionSuggestion("suggest").prefix(prefix).size(5);
		
		SuggestBuilder suggestBuilder = new SuggestBuilder();
		suggestBuilder.addSuggestion("autocomplete", suggestionBuilder);
		
		SearchRequestBuilder requestBuilder = this.esClient.prepareSearch(INDEX_NAME)//
			.setTypes(INDEX_TYPE)//
			.suggest(suggestBuilder)//
			;
		SearchResponse response = requestBuilder.get();
		Suggestion suggestion = response.getSuggest().getSuggestion("autocomplete"); 
		
		int maxSuggest = 0;
		Set<String> suggestSet = new HashSet<>();
		for(Object term : suggestion.getEntries()) {
			if(term instanceof CompletionSuggestion.Entry) {
				CompletionSuggestion.Entry item = (CompletionSuggestion.Entry)term;
				if(item.getOptions().isEmpty()) {
					continue;
				}
				for(CompletionSuggestion.Entry.Option option : item.getOptions()) {
					String tip = option.getText().string();
					if(suggestSet.contains(tip)) {
						continue;
					}
					suggestSet.add(tip);
					maxSuggest++;
				}
			}
			if(maxSuggest > 5) {
				break;
			}
		}
		List<String> result = Lists.newArrayList(suggestSet.toArray(new String[0]));
		
		return ServiceResult.<List<String>> of(result);
	}
	
	private boolean updateSuggest(HouseIndexTemplate template) {
		AnalyzeRequestBuilder requestBuilder  = new AnalyzeRequestBuilder(this.esClient, AnalyzeAction.INSTANCE,
				INDEX_NAME,
				template.getTitle(),
				template.getLayoutDesc(),template.getRoundService(),
				template.getTraffic(), template.getDescription(),
				template.getSubwayLineName(),template.getSubwayStationName()
				);
		requestBuilder.setAnalyzer("ik_smart");
		
		AnalyzeResponse response = requestBuilder.get();
		List<AnalyzeToken> tokens = response.getTokens();
		if(tokens.isEmpty()) {
			log.warn("Can not analyze token for house "+ template.getHouseId());
			return false;
		}
		
		List<HouseSuggest> suggests = new ArrayList<>();
		for(AnalyzeToken token : tokens) {
			//排除数字类型和 长度小于2的分词结果
			if("<NUM>".equals(token.getType()) || token.getTerm().length() < 2) {
				continue;
			}
			
			HouseSuggest suggest = new HouseSuggest();
			suggest.setInput(token.getTerm());
			suggests.add(suggest);
		}
		
		//定制化小区自动补全
		HouseSuggest suggest = new HouseSuggest();
		suggest.setInput(template.getDistrict());
		suggests.add(suggest);
		
		template.setSuggests(suggests);
		return true;
	}

	@Override
	public ServiceResult<Long> aggregateDistrictHouse(String cityName, String regionName, String district) {
		BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()//
				.filter(QueryBuilders.termQuery(HouseIndexKey.CITY_EN_NAME, cityName))//
				.filter(QueryBuilders.termQuery(HouseIndexKey.REGION_EN_NAME, regionName))//
				.filter(QueryBuilders.termQuery(HouseIndexKey.DISTRICT, district))//
				;
		
		SearchRequestBuilder requestBuilder = this.esClient.prepareSearch(INDEX_NAME)//
				.setTypes(INDEX_TYPE)//
				.setQuery(boolQueryBuilder)//
				.addAggregation(
						AggregationBuilders.terms(HouseIndexKey.AGG_DISTRICT)//
						.field(HouseIndexKey.DISTRICT)	//						
				).setSize(0);
		
		log.debug(requestBuilder.toString());
			
				
		SearchResponse response = requestBuilder.get();
		if(response.status() == RestStatus.OK) {
			Terms terms = response.getAggregations().get(HouseIndexKey.AGG_DISTRICT);
			
			if(terms.getBuckets() != null	&& !terms.getBuckets().isEmpty()) {
				return ServiceResult.<Long>of(terms.getBucketByKey(district).getDocCount());
			}
		}else {
			log.warn("Failed to aggregate for " + HouseIndexKey.AGG_DISTRICT);
		}
				
		return ServiceResult.<Long> of(0L);
	}
}
