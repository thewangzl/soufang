package com.thewangzl.sf.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.thewangzl.sf.domain.SupportAddress;

@Repository
public interface SupportAddressRepository extends CrudRepository<SupportAddress, Long> {

	List<SupportAddress> findAllByLevel(String level);
	
    SupportAddress findByEnNameAndLevel(String enName, String level);

    SupportAddress findByEnNameAndBelongTo(String enName, String belongTo);
	
	List<SupportAddress> findAllByLevelAndBelongTo(String level, String cityName);
}
