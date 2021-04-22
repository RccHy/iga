package com.qtgl.iga.bean;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OccupyDto {

    private String postCode;

    private String deptCode;

    private String personCardType;

    private String personCardNo;

    // 身份证件类型
    private String identityCardType;

    // 身份证件编号
    private String identityCardNo;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer index;

    private String source;


    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private String userId;
    private String occupyId;

}
