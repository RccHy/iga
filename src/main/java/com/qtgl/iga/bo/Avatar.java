package com.qtgl.iga.bo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Avatar implements Serializable, Cloneable {
    /**
     * 主键id
     */
    private String id;

    /**
     * 头像url
     */
    private String avatarUrl;
    /**
     * 头像文件
     */
    private byte[] avatar;
    /**
     * 头像hashcode
     */
    private Integer avatarHashCode;
    /**
     * 头像更新时间
     */
    private LocalDateTime avatarUpdateTime;
    /**
     * 人员id
     */
    private String identityId;
    /**
     * 租户id
     */
    private String tenantId;


    /**
     * 证件类型
     */
    private String cardType;
    /**
     * 证件号码
     */
    private String cardNo;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 用户名
     */
    private String accountNo;
    /**
     * 电话
     */
    private String cellphone;
    /**
     * 人员特征
     */
    private String personCharacteristic;
}
