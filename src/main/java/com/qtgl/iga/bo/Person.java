package com.qtgl.iga.bo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author 1
 */
@Data
public class Person {

    /**
     * 主键
     */
    private String id;
    /**
     * 名称
     */
    private String name;
    /**
     * 用户名
     */
    private String accountNo;
    /**
     * 证件类型
     */
    private String cardType;
    /**
     * 证件号码
     */
    private String cardNo;
    /**
     * 标签
     */
    private String tags;
    /**
     * 租户
     */
    private String tenantId;
    /**
     * 电话
     */
    private String cellphone;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 来源
     */
    private String source;
    /**
     * 来源(机读)
     */
    private String dataSource;
    /**
     * 启用
     */
    private Integer active;
    /**
     * 启用时间
     */
    private LocalDateTime activeTime;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 修改时间
     */
    private LocalDateTime updateTime;
    /**
     * 删除标记
     */
    private Integer delMark;
    /**
     * 权威源类型
     */
    private String upstreamType;

    private String openId;

    /**
     * 人员最终有效时间
     */
    private LocalDateTime validStartTime;

    private LocalDateTime validEndTime;

    /**
     * 冻结时间
     */
    private LocalDateTime freezeTime;

    /**
     * 密码
     */
    private String password;

}
