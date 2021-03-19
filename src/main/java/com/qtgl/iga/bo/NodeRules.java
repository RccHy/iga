package com.qtgl.iga.bo;


import lombok.Data;

import java.sql.Timestamp;


/**
 * 节点规则明细
 */
@Data
public class NodeRules implements java.io.Serializable {


    private String id;

    //node 外键
    private String nodeId;

    private String inheritId;

    // 规则类型 0推送 1拉取 3手动
    private Integer type;

    //
    private Boolean active;

    // 生效/失效 操作时间
    private Long activeTime;

    //创建时间
    private Long createTime;

    //修改时间
    private Long updateTime;

    //【推送】的服务标识
    private String serviceKey;

    //UpStreamType外键
    private String upstreamTypesId;

    //排序
    private Integer sort;


}
