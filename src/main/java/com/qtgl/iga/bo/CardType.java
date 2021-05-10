package com.qtgl.iga.bo;


import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author 1
 */
@Data
public class CardType {

    /**
     * 主键
     */
    private String id;
    /**
     * 类型名称
     */
    private String cardTypeName;
    /**
     * 类型编号
     */
    private String cardTypeCode;
    /**
     * 类型规则
     */
    private String cardTypeReg;
    /**
     * 租户id
     */
    private String tenantId;
    /**
     * 修改时间
     */
    private LocalDateTime updateTime;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
