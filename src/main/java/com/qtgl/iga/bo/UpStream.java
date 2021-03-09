package com.qtgl.iga.bo;

import lombok.Data;

import java.util.Date;

/**
 * <FileName> UpStream
 * <Desc> 上游源注册表
 **/
@Data
public class UpStream {
    //主键
    private String id;
    //应用代码，如人事：HR_SYS
    private String appCode;
    //应用名称，如人事
    private String appName;
    //数据前缀代码 HR
    private String dataCode;
    //注册时间
    private Date createTime;
    //修改时间
    private Date updateTime;
    //启用时间
    private Date activeTime;
    //注册人员
    private String createUser;
    //状态  启用/不启用
    private Boolean active;
    //代表色
    private String color;
    //租户信息外建
    private String domain;

}
