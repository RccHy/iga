package com.qtgl.iga.bean;

import com.qtgl.iga.bo.Node;
import com.qtgl.iga.vo.NodeRulesVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * <FileName> NodeDto
 * <Desc> Node入参
 *
 * @author 1
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class NodeDto extends Node {

    /**
     * 节点规则明细
     */
    private List<NodeRulesVo> nodeRules;

    private Boolean inherit;
    //是否来自本租户
    private Boolean local;
    public NodeDto() {
    }

    public NodeDto(Node node) {
        this.setId(node.getId());
        this.setCreateTime(node.getCreateTime());
        this.setDomain(node.getDomain());
        this.setManual(node.getManual());
        this.setNodeCode(node.getNodeCode());
        this.setUpdateTime(node.getUpdateTime());
        this.setDeptTreeType(node.getDeptTreeType());
        this.setStatus(node.getStatus());
        this.setType(node.getType());
        this.setNodeRules(new ArrayList<NodeRulesVo>());
    }

}
