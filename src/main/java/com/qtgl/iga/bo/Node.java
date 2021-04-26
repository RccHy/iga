package com.qtgl.iga.bo;


import lombok.Data;


/**
 * 节点规则
 *
 * @author 1
 */
@Data
public class Node {

    private String id;

    /**
     * 是否允许手工
     */
    private Boolean manual;

    /**
     * 节点代码
     */
    private String nodeCode;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 修改时间
     */
    private Long updateTime;

    /**
     * 租户
     */
    private String domain;

    /**
     * 部门树类型
     */
    private String deptTreeType;
    /**
     * 当前状态 0 发布 1 编辑中 2 历史
     */
    private Integer status;
    /**
     * 类型
     */
    private String type;

}
