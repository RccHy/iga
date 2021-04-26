package com.qtgl.iga.bean;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author 1
 */
@Data
public class OccupyDto {

    private String personId;
    private String occupyId;
    private String postCode;
    private String deptCode;
    private String personCardType;
    private String personCardNo;
    /**
     * 身份证件类型
     */
    private String identityCardType;
    /**
     * 身份证件编号
     */
    private String identityCardNo;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer index;
    private String source;
    private String dataSource;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;
    private Integer delMark;
    private String active;
    private LocalDateTime activeTime;


    private String upstreamType;

}
