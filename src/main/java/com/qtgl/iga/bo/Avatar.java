package com.qtgl.iga.bo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Avatar implements Serializable,Cloneable{
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
}
