package com.qtgl.iga.bo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author 1
 */
@Data
public class Certificate {

    /**
     * 主键
     */
    private String id;

    /**
     * 证件类型
     */
    private String cardType;
    /**
     * 证件号码
     */
    private String cardNo;
    /**
     * 证件所有者
     */
    private String identityId;

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
     * 证件来源
     */
    private String fromIdentityId;

}
