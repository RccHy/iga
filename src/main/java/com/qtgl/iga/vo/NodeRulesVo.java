package com.qtgl.iga.vo;

import com.qtgl.iga.bo.NodeRules;
import com.qtgl.iga.bo.NodeRulesRange;
import lombok.Data;

import java.util.List;

/**
 * <FileName> NodeRulesVo
 * <Desc>
 **/
@Data
public class NodeRulesVo extends NodeRules {


    //节点规则明细作用域
    private List<NodeRulesRange> nodeRulesRanges;
    //是否来源于本租户
    private Boolean local;
    //0 正常 1[超级租户] 权威源禁用-忽略变动 2[超级租户] 规则禁用-忽略数据并将之前改规则提供的数据置为无效    3 [当前租户]权威源不启用-忽略变动
    //4 [当前租户]规则不启用 -忽略数据并将之前改规则提供的数据置为无效
    private Integer runningStatus;

    public NodeRulesVo(NodeRules nodeRules) {
        this.setId(nodeRules.getId());
        this.setNodeId(nodeRules.getNodeId());
        this.setInheritId(nodeRules.getInheritId());
        this.setType(nodeRules.getType());
        this.setActive(nodeRules.getActive());
        this.setActiveTime(nodeRules.getActiveTime());
        this.setCreateTime(nodeRules.getCreateTime());
        this.setUpdateTime(nodeRules.getUpdateTime());
        this.setServiceKey(nodeRules.getServiceKey());
        this.setUpstreamTypesId(nodeRules.getUpstreamTypesId());
        this.setSort(nodeRules.getSort());
        this.setStatus(nodeRules.getStatus());
    }

    public NodeRulesVo() {
    }
}
