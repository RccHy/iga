package com.qtgl.iga.bean;

import lombok.Data;

@Data
public class OccupyDto {

    private String postCode;

    private String deptCode;

    private String personCardType;

    private String personCardNo;

    private String identityCardType;

    private String identityCardNo;

    private Long startTime;

    private Long endTime;

    private Integer index;

}
