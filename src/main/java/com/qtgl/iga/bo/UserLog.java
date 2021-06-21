package com.qtgl.iga.bo;


import lombok.Data;

import java.io.Serializable;
import java.util.Date;


/**
 * 人员身份有效期变更日志
 */

@Data
public class UserLog implements Serializable {


    private static final long serialVersionUID = 8372029665959131980L;

    /**
     * 主键
     */
    private String id;
    /**
     * 人员身份表外键
     */
    private String userId;
    /**
     * 身份开始时间
     */
    private Date startTime;
    /**
     * 身份到期时间
     */
    private Date endTime;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 来源
     */
    private String source;
    /**
     * 数据来源
     */
    private String dataSource;


    public UserLog() {
    }



}
