package com.qtgl.iga.bean;

import com.qtgl.iga.bo.DynamicValue;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    // 上游删除
    private Integer active;
    private LocalDateTime activeTime;


    private String upstreamType;
    private String openId;
    //用户名
    private String accountNo;
    // 手机号
    private String cellPhone;
    //邮箱
    private String email;


    /**
     * 身份最终有效时间
     */
    private LocalDateTime validStartTime;
    private LocalDateTime validEndTime;
    /**
     * 上游岗位、部门导致身份失效
     */
    private  Integer orphan;
    /**
     * 逻辑处理字段 规则是否启用
     */
    private Boolean ruleStatus;
    private Map<String, String> dynamic;

    //扩展字段值
    private List<DynamicValue> attrsValues;



}
