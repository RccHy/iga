package com.qtgl.iga.bo;


import lombok.Data;


/**
 * 节点规则明细作用域
 *
 * @author 1
 */
@Data
public class NodeRulesRange {

    private String id;

    /**
     * nodeRules外键
     */
    private String nodeRulesId;

    /**
     * 规则类型 0 挂载 1 排除 2 重命名
     */
    private Integer type;

    /**
     * 重命名规则
     */
    private String rename;

    /**
     * 作用节点
     */
    private String node;

    /**
     * 作用域0/1；挂载（是否包含节点本身0还是仅其子树1） 排除（排除无用节点以及0/仅1其子树）
     */
    private Integer range;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 创建时间
     */
    private Long updateTime;
    /**
     * 当前状态 0 发布 1 编辑中 2 历史 3失效
     */
    private Integer status;

}
