package com.qtgl.iga.bo;


import lombok.Data;


/**
 * 节点规则明细
 *
 * @author 1
 */
@Data
public class NodeRules implements java.io.Serializable {

    /**
     * 主键
     */
    private String id;

    /**
     * node 外键
     */
    private String nodeId;
    /**
     * 是否继承
     */
    private String inheritId;

    /**
     * 规则类型 0推送 1拉取 2手动
     */
    private Integer type;

    /**
     * 是否启用
     */
    private Boolean active;

    /**
     * 生效/失效 操作时间
     */
    private Long activeTime;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 修改时间
     */
    private Long updateTime;

    /**
     * 【推送】的服务标识
     */
    private String serviceKey;

    /**
     * UpStreamType外键
     */
    private String upstreamTypesId;

    /**
     * 排序
     */
    private Integer sort;
    /**
     * 当前状态 0 发布 1 编辑中 2 历史 3失效
     */
    private Integer status;


}
