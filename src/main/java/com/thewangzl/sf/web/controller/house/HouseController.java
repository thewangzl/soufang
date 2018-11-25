package com.thewangzl.sf.web.controller.house;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.thewangzl.sf.base.ApiResponse;
import com.thewangzl.sf.base.RentValueBlock;
import com.thewangzl.sf.service.ServiceMultiResult;
import com.thewangzl.sf.service.ServiceResult;
import com.thewangzl.sf.service.house.IAddressService;
import com.thewangzl.sf.service.house.IHouseService;
import com.thewangzl.sf.web.controller.dto.HouseDTO;
import com.thewangzl.sf.web.controller.dto.SubwayDTO;
import com.thewangzl.sf.web.controller.dto.SubwayStationDTO;
import com.thewangzl.sf.web.controller.dto.SupportAddressDTO;
import com.thewangzl.sf.web.controller.form.RentSearch;

@Controller
public class HouseController {
	
	@Autowired
	private IAddressService addressService;
	
	@Autowired
	private IHouseService houseService;

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
    	model.addAttribute("currentAreaBlock", RentValueBlock.matchPrice(rentSearch.getAreaBlock()));
    	
    	return "rent-list";
    }
}
