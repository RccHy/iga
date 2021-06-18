package com.qtgl.iga.bo;


import com.qtgl.iga.bean.OccupyDto;
import lombok.Data;

import java.io.Serializable;
import java.time.ZoneId;
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

    public UserLog(OccupyDto occupyDto) {

        this.userId = occupyDto.getOccupyId();
        this.startTime = null == occupyDto.getStartTime() ? null : Date.from(occupyDto.getStartTime().atZone(ZoneId.systemDefault()).toInstant());
        this.endTime = null == occupyDto.getStartTime() ? null : Date.from(occupyDto.getEndTime().atZone(ZoneId.systemDefault()).toInstant());
//        this.createTime = null == occupyDto.getStartTime() ? null : Date.from(occupyDto.getCreateTime().atZone(ZoneId.systemDefault()).toInstant());
        this.source = occupyDto.getSource();
        this.dataSource = occupyDto.getDataSource();

    }


}
