package com.thewangzl.sf.web.controller.admin;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.validation.Valid;

import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.thewangzl.sf.base.ApiDataTableResponse;
import com.thewangzl.sf.base.ApiResponse;
import com.thewangzl.sf.base.HouseOperation;
import com.thewangzl.sf.base.HouseStatus;
import com.thewangzl.sf.domain.SupportAddress;
import com.thewangzl.sf.domain.SupportAddress.Level;
import com.thewangzl.sf.service.ServiceMultiResult;
import com.thewangzl.sf.service.ServiceResult;
import com.thewangzl.sf.service.house.IAddressService;
import com.thewangzl.sf.service.house.IHouseService;
import com.thewangzl.sf.web.controller.dto.HouseDTO;
import com.thewangzl.sf.web.controller.dto.HouseDetailDTO;
import com.thewangzl.sf.web.controller.dto.SubwayDTO;
import com.thewangzl.sf.web.controller.dto.SubwayStationDTO;
import com.thewangzl.sf.web.controller.dto.SupportAddressDTO;
import com.thewangzl.sf.web.controller.form.DatatableSearch;
import com.thewangzl.sf.web.controller.form.HouseForm;

@Controller
@RequestMapping("/admin")
public class AdminController {
	
	@Autowired
	private IAddressService addressService;
	
	@Autowired
	private IHouseService houseService;
	
	@GetMapping("/center")
	public String adminCenter() {
		return "admin/center";
	}
	
	@GetMapping("/welcome")
	public String welcomePage() {
		return "admin/welcome";
	}
	
	@GetMapping("/login")
	public String login() {
		return "admin/login";
	}
	
	@GetMapping("/add/house")
	public String addHouse() {
		
		return "admin/house-add";
	}
	
	@GetMapping("/house/list")
	public String houseListPage() {
		return "/admin/house-list";
	}
	
	@PostMapping("/houses")
	@ResponseBody
	public ApiDataTableResponse houses(@ModelAttribute DatatableSearch searchBody) {
		ServiceMultiResult<HouseDTO> houseDTOs = this.houseService.adminQuery(searchBody);
		
		ApiDataTableResponse response = new ApiDataTableResponse(ApiResponse.Status.SUCCESS);
		
		response.setData(houseDTOs.getResult());
		response.setRecordsFiltered(houseDTOs.getTotal());
		response.setRecordsTotal(houseDTOs.getTotal());
		response.setDraw(searchBody.getDraw());
		
		return response;
	}
	
	@PostMapping(value="upload/photo",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseBody
	public ApiResponse uploadPhoto(@RequestParam("file") MultipartFile file)
	{
		if(file.isEmpty()) {
			return ApiResponse.ofStatus(ApiResponse.Status.NOT_VALID_PARAM);
		}
		Map<String,Object> map = new HashMap<>();
		String filename = file.getOriginalFilename();
		File f = new File("D:/soufang/" + filename);
		try {
			file.transferTo(f);
			BufferedImage image = ImageIO.read(f);
			map.put("key", "D:\\soufang\\"+filename);
			map.put("width", image.getWidth());
			map.put("height", image.getHeight());
		} catch (IllegalStateException | IOException e) {
			return ApiResponse.ofStatus(ApiResponse.Status.INTERNAL_SERVER_ERROR);
		}
		return ApiResponse.ofSuccess(map);
	}
	
	@PostMapping("/add/house")
	@ResponseBody
	public ApiResponse addHouse(@Valid @ModelAttribute("form-house-add") HouseForm houseForm, BindingResult bindingResult) {
		if(bindingResult.hasErrors()){
			return new ApiResponse(HttpStatus.BAD_REQUEST.value(), bindingResult.getAllErrors().get(0).getDefaultMessage(), null);
		}
		if(houseForm.getPhotos() == null && houseForm.getCover() == null) {
			return ApiResponse.ofMessage(HttpStatus.BAD_REQUEST.value(), "必须上传图片");
		}
		Map<SupportAddress.Level, SupportAddressDTO> addressMap = this.addressService.findCityAndRegion(houseForm.getCityEnName(), houseForm.getRegionEnName());
		if(addressMap.keySet().size() != 2) {
			return ApiResponse.ofStatus(ApiResponse.Status.NOT_VALID_PARAM);
		}
		ServiceResult<HouseDTO> result = houseService.save(houseForm);
		if(result.isSuccess()) {
			return ApiResponse.ofSuccess(result.getResult());
		}
		return ApiResponse.ofStatus(ApiResponse.Status.NOT_VALID_PARAM);
	}
	
	@GetMapping("/house/edit")
	public String houseEditPage(@RequestParam("id") Long id, Model model) {
		if(id == null || id < 1) {
			 return "404";	
		}
		ServiceResult<HouseDTO> serviceResult = this.houseService.findCompleteOne(id);
		if(!serviceResult.isSuccess()) {
			return "404";
		}
		HouseDTO result = serviceResult.getResult();
		model.addAttribute("house", result);
		
		Map<Level, SupportAddressDTO> addressMap = addressService.findCityAndRegion(result.getCityEnName(), result.getRegionEnName());
		
		model.addAttribute("city", addressMap.get(SupportAddress.Level.CITY));
		model.addAttribute("region", addressMap.get(SupportAddress.Level.REGION));
		
		HouseDetailDTO detailDTO = result.getHouseDetail();
        ServiceResult<SubwayDTO> subwayServiceResult = addressService.findSubway(detailDTO.getSubwayLineId());
        if (subwayServiceResult.isSuccess()) {
            model.addAttribute("subway", subwayServiceResult.getResult());
        }

        ServiceResult<SubwayStationDTO> subwayStationServiceResult = addressService.findSubwayStation(detailDTO.getSubwayStationId());
        if (subwayStationServiceResult.isSuccess()) {
            model.addAttribute("station", subwayStationServiceResult.getResult());
        }
		
        return "admin/house-edit";
	}
	
	@PostMapping("house/edit")
	@ResponseBody
	public ApiResponse saveHouse(@Valid @ModelAttribute("form-house-edit") HouseForm houseForm, BindingResult bindingResult) {
		if(bindingResult.hasErrors()) {
			return new ApiResponse(ApiResponse.Status.BAD_REQUEST.getCode(), bindingResult.getAllErrors().get(0).getDefaultMessage(), null);
		}
		
		Map<SupportAddress.Level, SupportAddressDTO> addressMap = addressService.findCityAndRegion(houseForm.getCityEnName(), houseForm.getRegionEnName());
        if (addressMap.keySet().size() != 2) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_VALID_PARAM);
        }
		ServiceResult result = houseService.update(houseForm);
		if(result.isSuccess()) {
			return ApiResponse.ofSuccess(null);
		}
		ApiResponse response = ApiResponse.ofStatus(ApiResponse.Status.BAD_REQUEST);
		response.setMessage(result.getMessage());
        return response;
	}
	
	/**
	 * 移除图片
	 * @param id
	 * @return
	 */
	@DeleteMapping("/house/photo")
	@ResponseBody
	public ApiResponse removeHousePhoto(@RequestParam(value="id") Long id) {
		ServiceResult result = this.houseService.removePhoto(id);
		
		if(result.isSuccess()) {
			return ApiResponse.ofSuccess(null);
		}else {
			return ApiResponse.ofMessage(ApiResponse.Status.BAD_REQUEST.getCode(), result.getMessage());
		}
	}
	
	/**
	 * 修改封面
	 * @param coverId
	 * @param targetId
	 * @return
	 */
	@PostMapping("/house/cover")
	@ResponseBody
	public ApiResponse updateCover(@RequestParam("cover_id") Long coverId, @RequestParam("target_id") Long targetId) {
		ServiceResult result = this.houseService.updateCover(coverId, targetId);
		
		if(result.isSuccess()) {
			return ApiResponse.ofSuccess(null);
		}else {
			return ApiResponse.ofMessage(ApiResponse.Status.BAD_REQUEST.getCode(), result.getMessage());
		}
	}
	
	/**
	 * 增加标签
	 * @param houseId
	 * @param tag
	 * @return
	 */
	@PostMapping("/house/tag")
	@ResponseBody
	public ApiResponse addHouseTag(@RequestParam("house_id") Long houseId, @RequestParam("tag")String tag) {
		if(houseId == null || houseId < 0 || Strings.isNullOrEmpty(tag)) {
			return ApiResponse.ofStatus(ApiResponse.Status.BAD_REQUEST);
		}
		ServiceResult result = this.houseService.addTag(houseId, tag);
		
		if(result.isSuccess()) {
			return ApiResponse.ofSuccess(null);
		}else {
			return ApiResponse.ofMessage(ApiResponse.Status.BAD_REQUEST.getCode(), result.getMessage());
		}
	}
	
	/**
	 * 移除标签
	 * @param houseId
	 * @param tag
	 * @return
	 */
	@DeleteMapping("/house/tag")
	@ResponseBody
	public ApiResponse removeHouseTag(@RequestParam("house_id") Long houseId, @RequestParam("tag")String tag) {
		if(houseId == null || houseId < 0 || Strings.isNullOrEmpty(tag)) {
			return ApiResponse.ofStatus(ApiResponse.Status.BAD_REQUEST);
		}
		ServiceResult result = this.houseService.removeTag(houseId, tag);
		
		if(result.isSuccess()) {
			return ApiResponse.ofSuccess(null);
		}else {
			return ApiResponse.ofMessage(ApiResponse.Status.BAD_REQUEST.getCode(), result.getMessage());
		}
	}
	
	@PutMapping("/house/operate/{id}/{operation}")
	@ResponseBody
	public ApiResponse operateHouse(@PathVariable("id") Long id, @PathVariable("operation") int operation) {
		if(id == null || id < 1) {
			return ApiResponse.ofStatus(ApiResponse.Status.BAD_REQUEST);
		}
		if(operation == HouseOperation.PASS) {
			ServiceResult result = this.houseService.updateStatus(id, HouseStatus.PASSED.getValue());
			return ApiResponse.ofSuccess(null);
		}
		
		ServiceResult result;
		switch (operation) {
		case HouseOperation.PASS:
			result = this.houseService.updateStatus(id, HouseStatus.PASSED.getValue());
			break;
		case HouseOperation.PULL_OUT:
			result = this.houseService.updateStatus(id, HouseStatus.NOT_AUDITED.getValue());
			break;
		case HouseOperation.DELETE:
			result = this.houseService.updateStatus(id, HouseStatus.DELETED.getValue());
			break;
		case HouseOperation.RENT:
			result = this.houseService.updateStatus(id, HouseStatus.RENTED.getValue());
			break;
		default:
			return ApiResponse.ofStatus(ApiResponse.Status.BAD_REQUEST);
		}
		if(result.isSuccess()) {
			return ApiResponse.ofSuccess(null);
		}
		return ApiResponse.ofMessage(ApiResponse.Status.BAD_REQUEST.getCode(), result.getMessage());
	}
}
