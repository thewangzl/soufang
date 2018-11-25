package com.thewangzl.sf.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.thewangzl.sf.domain.SubwayStation;


/**
 * Created by 瓦力.
 */
@Repository
public interface SubwayStationRepository extends CrudRepository<SubwayStation, Long> {
    List<SubwayStation> findAllBySubwayId(Long subwayId);
}
