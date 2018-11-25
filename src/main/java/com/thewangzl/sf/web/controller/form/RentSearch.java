package com.thewangzl.sf.web.controller.form;

import lombok.Data;

@Data
public class RentSearch {

	private String cityEnName;
    private String regionEnName;
    private String priceBlock;
    private String areaBlock;
    private int room;
    private int direction;
    private String keywords;
    private int rentWay = -1;
    private String orderBy = "lastUpdateTime";
    private String orderDirection = "desc";
    
    private int start = 0;

    private int size = 5;
}
