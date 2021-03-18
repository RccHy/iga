package com.qtgl.iga.bean;

import com.qtgl.iga.bo.Node;
import com.qtgl.iga.bo.NodeRules;
import lombok.Data;

import java.util.List;

/**
 * <FileName> NodeDto
 * <Desc> Node入参
 **/
@Data
public class NodeDto extends Node {

    //节点规则明细
    private List<NodeRules> nodeRules;


}
