package com.thewangzl.sf.service.house;

import com.thewangzl.sf.service.ServiceMultiResult;
import com.thewangzl.sf.service.ServiceResult;
import com.thewangzl.sf.web.controller.dto.HouseDTO;
import com.thewangzl.sf.web.controller.form.DatatableSearch;
import com.thewangzl.sf.web.controller.form.HouseForm;
import com.thewangzl.sf.web.controller.form.RentSearch;

public interface IHouseService {

	/**
	 * 新增
	 * @param houseForm
	 * @return
	 */
	ServiceResult<HouseDTO> save(HouseForm houseForm);
	
	ServiceResult update(HouseForm houseForm );
	
	ServiceMultiResult<HouseDTO> adminQuery(DatatableSearch searchBody);
	
	/**
	 * 查询完整信息
	 * @param id
	 * @return
	 */
	ServiceResult<HouseDTO> findCompleteOne(Long id);

	ServiceResult removePhoto(Long id);

	ServiceResult updateCover(Long coverId, Long targetId);

	ServiceResult addTag(Long houseId, String tag);

	ServiceResult removeTag(Long houseId, String tag);
	
	/**
	 * 更新房源状态
	 * @param id
	 * @param status
	 * @return
	 */
	ServiceResult updateStatus(Long id, int status);

	ServiceMultiResult<HouseDTO> query(RentSearch rentSearch);
}
