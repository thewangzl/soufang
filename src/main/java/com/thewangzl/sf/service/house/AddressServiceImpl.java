package com.thewangzl.sf.service.house;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.thewangzl.sf.domain.Subway;
import com.thewangzl.sf.domain.SubwayStation;
import com.thewangzl.sf.domain.SupportAddress;
import com.thewangzl.sf.repository.SubwayRepository;
import com.thewangzl.sf.repository.SubwayStationRepository;
import com.thewangzl.sf.repository.SupportAddressRepository;
import com.thewangzl.sf.service.ServiceMultiResult;
import com.thewangzl.sf.service.ServiceResult;
import com.thewangzl.sf.web.controller.dto.SubwayDTO;
import com.thewangzl.sf.web.controller.dto.SubwayStationDTO;
import com.thewangzl.sf.web.controller.dto.SupportAddressDTO;

@Service
public class AddressServiceImpl implements IAddressService {

	@Autowired
	private SupportAddressRepository supportAddressRepository;
	
	@Autowired
    private SubwayRepository subwayRepository;

    @Autowired
    private SubwayStationRepository subwayStationRepository;
	
	@Autowired
	private ModelMapper modelMapper;
	
	@Override
	public ServiceMultiResult<SupportAddressDTO> findAllCities() {
		List<SupportAddress> supportAddresses = this.supportAddressRepository.findAllByLevel(SupportAddress.Level.CITY.getValue());
		
		List<SupportAddressDTO> supportAddressDTOs = new ArrayList<>();
		if(supportAddresses != null && supportAddresses.size() > 0) {
			supportAddresses.forEach(
					supportAddress -> supportAddressDTOs.add(modelMapper.map(supportAddress, SupportAddressDTO.class))
					);
		}
		return new ServiceMultiResult<>(supportAddressDTOs.size(), supportAddressDTOs);
	}
	
	@Override
    public Map<SupportAddress.Level, SupportAddressDTO> findCityAndRegion(String cityEnName, String regionEnName) {
        Map<SupportAddress.Level, SupportAddressDTO> result = new HashMap<>();

        SupportAddress city = supportAddressRepository.findByEnNameAndLevel(cityEnName, SupportAddress.Level.CITY
                .getValue());
        SupportAddress region = supportAddressRepository.findByEnNameAndBelongTo(regionEnName, city.getEnName());

        result.put(SupportAddress.Level.CITY, modelMapper.map(city, SupportAddressDTO.class));
        result.put(SupportAddress.Level.REGION, modelMapper.map(region, SupportAddressDTO.class));
        return result;
    }
	
	@Override
	public ServiceMultiResult<SupportAddressDTO> findAllRegionsByCityName(String cityName){
		if(cityName == null) {
			return new ServiceMultiResult<>(0, null);
		}
		List<SupportAddressDTO> addressDTOs = new ArrayList<>();
		List<SupportAddress> supportAddresses = this.supportAddressRepository.findAllByLevelAndBelongTo(SupportAddress.Level.REGION.REGION.getValue(), cityName);
		if(supportAddresses != null && supportAddresses.size() > 0) {
			supportAddresses.forEach(
					supportAddress -> addressDTOs.add(modelMapper.map(supportAddress, SupportAddressDTO.class))
					);
		}
		
		return new ServiceMultiResult<>(addressDTOs.size(), addressDTOs);
	}
	
	@Override
    public List<SubwayDTO> findAllSubwayByCity(String cityEnName) {
        List<SubwayDTO> result = new ArrayList<>();
        List<Subway> subways = subwayRepository.findAllByCityEnName(cityEnName);
        if (subways.isEmpty()) {
            return result;
        }

        subways.forEach(subway -> result.add(modelMapper.map(subway, SubwayDTO.class)));
        return result;
    }

    @Override
    public List<SubwayStationDTO> findAllStationBySubway(Long subwayId) {
        List<SubwayStationDTO> result = new ArrayList<>();
        List<SubwayStation> stations = subwayStationRepository.findAllBySubwayId(subwayId);
        if (stations.isEmpty()) {
            return result;
        }

        stations.forEach(station -> result.add(modelMapper.map(station, SubwayStationDTO.class)));
        return result;
    }

    @Override
    public ServiceResult<SubwayDTO> findSubway(Long subwayId) {
        if (subwayId == null) {
            return ServiceResult.notFound();
        }
        Subway subway = subwayRepository.findOne(subwayId);
        if (subway == null) {
            return ServiceResult.notFound();
        }
        return ServiceResult.of(modelMapper.map(subway, SubwayDTO.class));
    }

    @Override
    public ServiceResult<SubwayStationDTO> findSubwayStation(Long stationId) {
        if (stationId == null) {
            return ServiceResult.notFound();
        }
        SubwayStation station = subwayStationRepository.findOne(stationId);
        if (station == null) {
            return ServiceResult.notFound();
        }
        return ServiceResult.of(modelMapper.map(station, SubwayStationDTO.class));
    }

    @Override
    public ServiceResult<SupportAddressDTO> findCity(String cityEnName) {
        if (cityEnName == null) {
            return ServiceResult.notFound();
        }
        SupportAddress supportAddress = supportAddressRepository.findByEnNameAndLevel(cityEnName, SupportAddress.Level.CITY.getValue());
        if (supportAddress == null) {
            return ServiceResult.notFound();
        }

        SupportAddressDTO addressDTO = modelMapper.map(supportAddress, SupportAddressDTO.class);
        return ServiceResult.of(addressDTO);
    }

}
