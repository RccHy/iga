package com.qtgl.iga.bo;

import lombok.Data;

import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

/**
 * 注册用户表
 *
 * @author 1
 */
@Data
public class DomainInfo {
    /**
     * 主键
     */
    private String id;
    /**
     * 租户id
     */
    private String domainId;
    /**
     * 租户名称（域名）
     */
    private String domainName;
    /**
     * 授权id
     */
    private String clientId;
    /**
     * 授权密钥
     */
    private String clientSecret;
    /**
     * 状态
     */
    private Integer status;
    /**
     * 注册时间
     */
    private Timestamp createTime;
    /**
     * 修改时间
     */
    private Timestamp updateTime;
    /**
     * 注册人员
     */
    private String createUser;


     public DomainInfo(){};

    public DomainInfo(String domainName, String domainId, Timestamp createTime, Integer status, String clientId, String clientSecret){
        this.id= UUID.randomUUID().toString();
        this.domainName=domainName;
        this.createTime=createTime;
        this.domainId=domainId;
        this.status=status;
        this.clientId=clientId;
        this.clientSecret=clientSecret;
    }


}
