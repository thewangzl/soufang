package com.thewangzl.sf.service.house;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.criteria.Predicate;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thewangzl.sf.base.HouseStatus;
import com.thewangzl.sf.base.LoginUserUtil;
import com.thewangzl.sf.domain.House;
import com.thewangzl.sf.domain.HouseDetail;
import com.thewangzl.sf.domain.HousePicture;
import com.thewangzl.sf.domain.HouseTag;
import com.thewangzl.sf.domain.Subway;
import com.thewangzl.sf.domain.SubwayStation;
import com.thewangzl.sf.repository.HouseDetailRepository;
import com.thewangzl.sf.repository.HousePictureRepository;
import com.thewangzl.sf.repository.HouseRepository;
import com.thewangzl.sf.repository.HouseTagRepository;
import com.thewangzl.sf.repository.SubwayRepository;
import com.thewangzl.sf.repository.SubwayStationRepository;
import com.thewangzl.sf.service.ServiceMultiResult;
import com.thewangzl.sf.service.ServiceResult;
import com.thewangzl.sf.web.controller.dto.HouseDTO;
import com.thewangzl.sf.web.controller.dto.HouseDetailDTO;
import com.thewangzl.sf.web.controller.dto.HousePictureDTO;
import com.thewangzl.sf.web.controller.form.DatatableSearch;
import com.thewangzl.sf.web.controller.form.HouseForm;
import com.thewangzl.sf.web.controller.form.PhotoForm;
import com.thewangzl.sf.web.controller.form.RentSearch;

@Service
public class HouseServiceImpl implements IHouseService {

	@Autowired
	private ModelMapper modelMapper;
	
	@Autowired
	private HouseRepository houseRepository;
	
	@Autowired
	private SubwayRepository subwayRepository;
	
	@Autowired
	private SubwayStationRepository subwayStationRepository;
	
	@Autowired
	private HouseDetailRepository houseDetailRepository;
	
	@Autowired
	private HousePictureRepository housePictureRepository;
	
	@Autowired
	private HouseTagRepository houseTagRepository;
	
	@Value("${qiniu.cdn.prefix}")
	private String cdnPrefix;
	
	@Override
	@Transactional
	public ServiceResult<HouseDTO> save(HouseForm houseForm) {
		
		HouseDetail detail = new HouseDetail();
		ServiceResult<HouseDTO> validationResult = this.wrapperDetailInfo(detail, houseForm);
		if(validationResult != null) {
			return validationResult;
		}
		House house = new House();
		modelMapper.map(houseForm, house);
		
		Date date = new Date();
		house.setCreateTime(date);
		house.setLastUpdateTime(date);
		house.setAdminId(LoginUserUtil.getLoginUserId());
		
		house = houseRepository.save(house);
		
		detail.setHouseId(house.getId());
		detail = houseDetailRepository.save(detail);
		
		List<HousePicture> pictures = this.generatePictures(houseForm, house.getId());
		Iterable<HousePicture> housePictures = this.housePictureRepository.save(pictures);
		
		HouseDTO houseDTO = modelMapper.map(house, HouseDTO.class);
		HouseDetailDTO houseDetailDTO = modelMapper.map(detail, HouseDetailDTO.class);
		houseDTO.setHouseDetail(houseDetailDTO);
		
		List<HousePictureDTO> housePictureDTOs = new ArrayList<>();
		housePictures.forEach(picture -> housePictureDTOs.add(modelMapper.map(picture, HousePictureDTO.class)));
		houseDTO.setPictures(housePictureDTOs);
		houseDTO.setCover(this.cdnPrefix + houseDTO.getCover());
		
		List<String> tags = houseForm.getTags();
		
		if(tags != null && !tags.isEmpty()) {
			List<HouseTag> houseTags = new ArrayList<>();
			for(String tag : tags) {
				houseTags.add(new HouseTag(house.getId(), tag));
			}
			houseTagRepository.save(houseTags);
			houseDTO.setTags(tags);
		}
		
		return ServiceResult.<HouseDTO> of(houseDTO);
	}
	
	private List<HousePicture> generatePictures(HouseForm houseForm, Long houseId){
		List<HousePicture> pictures = new ArrayList<>();
		if(houseForm.getPhotos() == null  || houseForm.getPhotos().isEmpty()) {
			return pictures;
		}
		for(PhotoForm photoForm : houseForm.getPhotos()) {
			HousePicture picture = new HousePicture();
			picture.setHouseId(houseId);
			picture.setCdnPrefix(cdnPrefix);
			picture.setPath(photoForm.getPath());
			picture.setWidth(photoForm.getWidth());
			picture.setHeight(photoForm.getHeight());
			
			pictures.add(picture);
		}
		return pictures;
	}
	
	private ServiceResult<HouseDTO> wrapperDetailInfo(HouseDetail detail, HouseForm houseForm) {
		if(houseForm.getSubwayLineId()	!= null) {
			Subway subway = this.subwayRepository.findOne(houseForm.getSubwayLineId());
			if(houseForm.getSubwayStationId() != null) {
				SubwayStation subwayStation = this.subwayStationRepository.findOne(houseForm.getSubwayStationId());
				if(subwayStation == null || subwayStation.getSubwayId() != subway.getId()) {
					return new ServiceResult<>(false, "Not valid subway station");
				}
				detail.setSubwayLineId(subway.getId());
				detail.setSubwayLineName(subway.getName());
				
				detail.setSubwayStationId(subwayStation.getId());
				detail.setSubwayStationName(subwayStation.getName());
			}
		}
		
		detail.setDescription(houseForm.getDescription());
		detail.setDetailAddress(houseForm.getDetailAddress());
		detail.setLayoutDesc(houseForm.getLayoutDesc());
		detail.setRentWay(houseForm.getRentWay());
		detail.setRoundService(houseForm.getRoundService());
		detail.setTraffic(houseForm.getTraffic());
		
		return null;
	}

	@Override
	public ServiceMultiResult<HouseDTO> adminQuery(DatatableSearch searchBody) {
		List<HouseDTO> houseDTOs = new ArrayList<>();
		
		Sort sort = new Sort(Sort.Direction.fromString(searchBody.getDirection()), searchBody.getOrderBy());
		
		int page = searchBody.getStart() / searchBody.getLength();
		
		Pageable pageable = new PageRequest(page, searchBody.getLength(), sort);
		
		Specification<House> specification = (root, query, cb) ->{
			Predicate predicate = cb.equal(root.get("adminId"), LoginUserUtil.getLoginUserId());
			
			predicate = cb.and(predicate, cb.notEqual(root.get("status"), HouseStatus.DELETED.getValue()));

			if(searchBody.getCity() != null) {
				predicate = cb.and(predicate, cb.equal(root.get("cityEnName"), searchBody.getCity()));
			}
			if(searchBody.getStatus() != null) {
				predicate = cb.and(predicate, cb.equal(root.get("status"), searchBody.getStatus()));
			}
			if(searchBody.getCreateTimeMin() != null) {
				predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("createTime"), searchBody.getCreateTimeMin()));
			}
			if(searchBody.getCreateTimeMax() != null) {
				predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("createTime"), searchBody.getCreateTimeMax()));
			}
			if(searchBody.getTitle() != null) {
				predicate = cb.and(predicate, cb.like(root.get("title"), "%" + searchBody.getTitle() + "%"));
			}

			return predicate;
		};
		
		Page<House> houses = this.houseRepository.findAll(specification, pageable);
		houses.forEach(house -> {
			HouseDTO houseDTO = modelMapper.map(house, HouseDTO.class);
			
			houseDTO.setCover(this.cdnPrefix + house.getCover());
			houseDTOs.add(houseDTO);
		});
		
		return new ServiceMultiResult<>(houses.getTotalElements(), houseDTOs);
	}

	@Override
	public ServiceResult<HouseDTO> findCompleteOne(Long id) {
		House house = this.houseRepository.findOne(id);
		if(house == null) {
			return ServiceResult.notFound();
		}
		
		HouseDetail houseDetail = this.houseDetailRepository.findByHouseId(id);
		HouseDetailDTO houseDetailDTO = modelMapper.map(houseDetail, HouseDetailDTO.class);
		
		List<HousePicture> housePictures = this.housePictureRepository.findAllByHouseId(id);
		List<HousePictureDTO> housePictureDTOs = new ArrayList<>();
		housePictures.forEach(picture -> {
			housePictureDTOs.add(modelMapper.map(picture, HousePictureDTO.class));
		});
		
		List<HouseTag> tags = this.houseTagRepository.findAllByHouseId(id);
		List<String>  tagList = new ArrayList<>();
		tags.forEach(tag -> {
			tagList.add(tag.getName());
		});
		
		HouseDTO houseDTO = modelMapper.map(house, HouseDTO.class);
		houseDTO.setHouseDetail(houseDetailDTO);
		houseDTO.setPictures(housePictureDTOs);
		houseDTO.setTags(tagList);
		
		return ServiceResult.<HouseDTO> of(houseDTO);
	}

	
	@Override
	@Transactional
	public ServiceResult update(HouseForm houseForm) {
		House house = this.houseRepository.findOne(houseForm.getId());
		if(house == null) {
			return ServiceResult.notFound();
		}
		HouseDetail houseDetail = this.houseDetailRepository.findByHouseId(houseForm.getId());
		if(houseDetail == null) {
			return ServiceResult.notFound();
		}
		
		ServiceResult wrapperResult = this.wrapperDetailInfo(houseDetail, houseForm);
		if(wrapperResult != null) {
			return wrapperResult;
		}
		this.houseDetailRepository.save(houseDetail);
		
		List<HousePicture> pictures = this.generatePictures(houseForm, houseForm.getId());
		this.housePictureRepository.save(pictures);
		
		if(houseForm.getCover() == null) {
			houseForm.setCover(house.getCover());
		}
		
		modelMapper.map(houseForm, house);
		house.setLastUpdateTime(new Date());
		houseRepository.save(house);
		
		return ServiceResult.success();
	}

	@Override
	@Transactional
	public ServiceResult removePhoto(Long id) {
		HousePicture picture = this.housePictureRepository.findOne(id);
		if(picture == null) {
			return ServiceResult.notFound();
		}
		
		//TODO  七牛
		
		housePictureRepository.delete(id);
		return ServiceResult.success();
	}

	@Override
	@Transactional
	public ServiceResult updateCover(Long coverId, Long targetId) {
		HousePicture cover = housePictureRepository.findOne(coverId);
        if (cover == null) {
            return ServiceResult.notFound();
        }

        houseRepository.updateCover(targetId, cover.getPath());
        return ServiceResult.success();
	}

	@Override
	@Transactional
	public ServiceResult addTag(Long houseId, String tag) {
		House house = houseRepository.findOne(houseId);
        if (house == null) {
            return ServiceResult.notFound();
        }

        HouseTag houseTag = houseTagRepository.findByNameAndHouseId(tag, houseId);
        if (houseTag != null) {
            return new ServiceResult(false, "标签已存在");
        }

        houseTagRepository.save(new HouseTag(houseId, tag));
        return ServiceResult.success();
	}

	@Override
	public ServiceResult removeTag(Long houseId, String tag) {
		House house = houseRepository.findOne(houseId);
        if (house == null) {
            return ServiceResult.notFound();
        }

        HouseTag houseTag = houseTagRepository.findByNameAndHouseId(tag, houseId);
        if (houseTag == null) {
            return new ServiceResult(false, "标签不存在");
        }

        houseTagRepository.delete(houseTag.getId());
        return ServiceResult.success();
	}

	@Override
	@Transactional
	public ServiceResult updateStatus(Long id, int status) {
		House house = this.houseRepository.findOne(id);
		if (house == null) {
            return ServiceResult.notFound();
        }
		if (house.getStatus() == status) {
            return new ServiceResult(false, "状态没有发生变化");
        }

        if (house.getStatus() == HouseStatus.RENTED.getValue()) {
            return new ServiceResult(false, "已出租的房源不允许修改状态");
        }

        if (house.getStatus() == HouseStatus.DELETED.getValue()) {
            return new ServiceResult(false, "已删除的资源不允许操作");
        }
        houseRepository.updateStatus(id, status);
		
		return ServiceResult.success();
	}

	@Override
	public ServiceMultiResult<HouseDTO> query(RentSearch rentSearch) {
		Sort sort = new Sort(Sort.Direction.DESC, "lastUpdateTime");
		int page = rentSearch.getStart() / rentSearch.getSize();
		
		Pageable pageable = new PageRequest(page, rentSearch.getSize(), sort);
		
		Specification<House> specification = (root, query, cb) -> {
			Predicate predicate = cb.equal(root.get("status"), HouseStatus.PASSED.getValue());
			
			predicate = cb.and(predicate, cb.equal(root.get("cityEnName"), rentSearch.getCityEnName()));
			
			return predicate;
		};
		Page<House> houses = this.houseRepository.findAll(specification, pageable);
		List<HouseDTO> houseDTOs = new ArrayList<>();
		houses.forEach(house -> {
			HouseDTO houseDTO = modelMapper.map(house, HouseDTO.class);
			houseDTO.setCover(this.cdnPrefix + house.getCover());
			houseDTOs.add(houseDTO);
		});
		
		return new ServiceMultiResult<>(houses.getTotalElements(), houseDTOs);
	}

}
