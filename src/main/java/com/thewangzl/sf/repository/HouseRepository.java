package com.thewangzl.sf.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import com.thewangzl.sf.domain.House;

public interface HouseRepository extends PagingAndSortingRepository<House, Long>, JpaSpecificationExecutor<House> {

	@Modifying
	@Query("update House as house set house.cover = :cover where house.id = :id")
	void updateCover(@Param("id")Long id, @Param("cover") String cover);

	@Modifying
	@Query("update House as house set house.status = :status where house.id = :id")
	void updateStatus(@Param("id") Long id, @Param("status") int status);
	

}
