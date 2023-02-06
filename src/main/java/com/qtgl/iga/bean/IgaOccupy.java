package com.qtgl.iga.bean;


import lombok.Data;

import java.util.List;

@Data
public class IgaOccupy {
    /**
     * 主键
     */
    private String id;
    /**
     * 姓名
     */
    private String name;

    /**
     * 用户名
     */
    private String accountNo;
    /**
     * 人员证件类型
     */
    private String cardType;
    /**
     * 人员证件号码
     */
    private String cardNo;

    /**
     * openid
     */
    private String openId;
    /**
     * 身份
     */
    private List<OccupyDto> positions;
}
