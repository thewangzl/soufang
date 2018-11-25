package com.thewangzl.sf.web.controller.house;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.thewangzl.sf.base.ApiResponse;
import com.thewangzl.sf.base.RentValueBlock;
import com.thewangzl.sf.domain.SupportAddress;
import com.thewangzl.sf.service.IUserService;
import com.thewangzl.sf.service.ServiceMultiResult;
import com.thewangzl.sf.service.ServiceResult;
import com.thewangzl.sf.service.house.IAddressService;
import com.thewangzl.sf.service.house.IHouseService;
import com.thewangzl.sf.web.controller.dto.HouseDTO;
import com.thewangzl.sf.web.controller.dto.SubwayDTO;
import com.thewangzl.sf.web.controller.dto.SubwayStationDTO;
import com.thewangzl.sf.web.controller.dto.SupportAddressDTO;
import com.thewangzl.sf.web.controller.dto.UserDTO;
import com.thewangzl.sf.web.controller.form.RentSearch;

@Controller
public class HouseController {
	
	@Autowired
	private IAddressService addressService;
	
	@Autowired
	private IHouseService houseService;
	
	@Autowired
	private IUserService userService;

	@GetMapping("/address/support/cities")
	@ResponseBody
	public ApiResponse getSuppprtCities() {
		ServiceMultiResult<SupportAddressDTO> addressDTOs = addressService.findAllCities();
		if(addressDTOs.getResultSize() == 0) {
			return ApiResponse.ofStatus(ApiResponse.Status.NOT_FOUND);
		}
		return ApiResponse.ofSuccess(addressDTOs.getResult());
	}
	
	/**
     * 获取对应城市支持区域列表
     * @param cityEnName
     * @return
     */
    @GetMapping("address/support/regions")
    @ResponseBody
    public ApiResponse getSupportRegions(@RequestParam(name = "city_name") String cityEnName) {
        ServiceMultiResult<SupportAddressDTO> addressResult = addressService.findAllRegionsByCityName(cityEnName);
        if (addressResult.getResult() == null || addressResult.getTotal() < 1) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_FOUND);
        }
        return ApiResponse.ofSuccess(addressResult.getResult());
    }

    /**
     * 获取具体城市所支持的地铁线路
     * @param cityEnName
     * @return
     */
    @GetMapping("address/support/subway/line")
    @ResponseBody
    public ApiResponse getSupportSubwayLine(@RequestParam(name = "city_name") String cityEnName) {
        List<SubwayDTO> subways = addressService.findAllSubwayByCity(cityEnName);
        if (subways.isEmpty()) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_FOUND);
        }

        return ApiResponse.ofSuccess(subways);
    }

    /**
     * 获取对应地铁线路所支持的地铁站点
     * @param subwayId
     * @return
     */
    @GetMapping("address/support/subway/station")
    @ResponseBody
    public ApiResponse getSupportSubwayStation(@RequestParam(name = "subway_id") Long subwayId) {
        List<SubwayStationDTO> stationDTOS = addressService.findAllStationBySubway(subwayId);
        if (stationDTOS.isEmpty()) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_FOUND);
        }

        return ApiResponse.ofSuccess(stationDTOS);
    }
    
    @GetMapping("/rent/house")
    public String rentHousePage(@ModelAttribute RentSearch rentSearch, Model model, HttpSession session,RedirectAttributes redirectAttributes) {
    	if(rentSearch.getCityEnName() == null) {
    		String cityEnNameInSession = (String) session.getAttribute("cityEnName");
    		if(cityEnNameInSession == null) {
    			redirectAttributes.addAttribute("msg", "must_chose_house");
    		}else {
    			rentSearch.setCityEnName(cityEnNameInSession);
    		}
    	}else {
    		session.setAttribute("cityEnName", rentSearch.getCityEnName());
    	}
    	
    	ServiceResult<SupportAddressDTO> city = addressService.findCity(rentSearch.getCityEnName());
    	if(!city.isSuccess()) {
    		redirectAttributes.addAttribute("msg", "must_chose_house");
    		return "redirect:/index";
    	}
    	model.addAttribute("currentCity", city.getResult());
    	ServiceMultiResult<SupportAddressDTO> addressResult = this.addressService.findAllRegionsByCityName(rentSearch.getCityEnName());
    	if(addressResult.getResult() == null || addressResult.getTotal() < 1) {
    		redirectAttributes.addAttribute("msg", "must_chose_house");
    		return "redirect:/index";
    	}
    	ServiceMultiResult<HouseDTO> result = this.houseService.query(rentSearch);
    	model.addAttribute("total", result.getTotal());
    	model.addAttribute("houses", result.getResult());
    	if(rentSearch.getRegionEnName() == null) {
    		rentSearch.setRegionEnName("*");
    	}
    	model.addAttribute("searchBody", rentSearch);
    	model.addAttribute("regions", addressResult.getResult());
    	
    	model.addAttribute("priceBlocks", RentValueBlock.PRICE_BLOCK);
    	model.addAttribute("areaBlocks", RentValueBlock.AREA_BLOCK);
    	
    	model.addAttribute("currentPriceBlock", RentValueBlock.matchPrice(rentSearch.getPriceBlock()));
    	model.addAttribute("currentAreaBlock", RentValueBlock.matchArea(rentSearch.getAreaBlock()));
    	
    	return "rent-list";
    }
    
    @GetMapping("rent/house/show/{id}")
    public String show(@PathVariable("id") Long id, Model model) {
    	if(id < 1) {
    		return "404";
    	}
    	ServiceResult<HouseDTO> serviceResult = this.houseService.findCompleteOne(id);
    	if(!serviceResult.isSuccess()) {
    		return "404";
    	}
    	HouseDTO houseDTO = serviceResult.getResult();
    	Map<SupportAddress.Level, SupportAddressDTO>
        addressMap = addressService.findCityAndRegion(houseDTO.getCityEnName(), houseDTO.getRegionEnName());

		SupportAddressDTO city = addressMap.get(SupportAddress.Level.CITY);
		SupportAddressDTO region = addressMap.get(SupportAddress.Level.REGION);
		
		model.addAttribute("city", city);
		model.addAttribute("region", region);
		
		ServiceResult<UserDTO> userDTOServiceResult = userService.findById(houseDTO.getAdminId());
		model.addAttribute("agent", userDTOServiceResult.getResult());
		model.addAttribute("house", houseDTO);
		
		model.addAttribute("houseCountInDistrict", 0);//TODO es的聚合功能
    	
    	return "house-detail";
    }
}
