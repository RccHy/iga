package com.qtgl.iga.bo;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class IncrementalTask {
    /**
     * 主键
     */
    private String id;
    /**
     * 类型 组织机构、岗位、人、身份
     */
    private String type;
    /**
     * 下次同步查询时间戳。（max9:_then:10。 返回数据 最小时间小于max 则取max。最大时间大于then 则取10）
     */
    private Timestamp time;
    /**
     * 数据获取完成时间
     */
    private Timestamp createTime;
    /**
     * 权威源类型ID
     */
    private String upstreamTypeId;
    /**
     * 所属租户
     */
    private String domain;
    /**
     * 本次操作数量
     */
    private String operationNo;
    /**
     * 主任务id
     */
    private String mainTaskId;
}
