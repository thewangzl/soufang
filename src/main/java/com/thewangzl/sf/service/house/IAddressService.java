package com.thewangzl.sf.service.house;


import java.util.List;
import java.util.Map;

import com.thewangzl.sf.domain.SupportAddress.Level;
import com.thewangzl.sf.service.ServiceMultiResult;
import com.thewangzl.sf.service.ServiceResult;
import com.thewangzl.sf.web.controller.dto.SubwayDTO;
import com.thewangzl.sf.web.controller.dto.SubwayStationDTO;
import com.thewangzl.sf.web.controller.dto.SupportAddressDTO;

public interface IAddressService {


	ServiceMultiResult<SupportAddressDTO> findAllCities();

	ServiceMultiResult<SupportAddressDTO> findAllRegionsByCityName(String cityName);

	List<SubwayDTO> findAllSubwayByCity(String cityEnName);

	List<SubwayStationDTO> findAllStationBySubway(Long subwayId);

	ServiceResult<SubwayDTO> findSubway(Long subwayId);

	ServiceResult<SubwayStationDTO> findSubwayStation(Long stationId);

	ServiceResult<SupportAddressDTO> findCity(String cityEnName);

	Map<Level, SupportAddressDTO> findCityAndRegion(String cityEnName, String regionEnName);
}

