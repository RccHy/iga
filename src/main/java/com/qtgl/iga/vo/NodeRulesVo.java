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
}
