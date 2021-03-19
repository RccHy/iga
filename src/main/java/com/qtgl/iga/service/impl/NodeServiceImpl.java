package com.qtgl.iga.service.impl;

import com.qtgl.iga.bean.NodeDto;
import com.qtgl.iga.bo.Node;
import com.qtgl.iga.bo.NodeRules;
import com.qtgl.iga.bo.NodeRulesRange;
import com.qtgl.iga.dao.NodeDao;
import com.qtgl.iga.dao.NodeRulesDao;
import com.qtgl.iga.dao.NodeRulesRangeDao;
import com.qtgl.iga.service.NodeService;
import com.qtgl.iga.vo.NodeRulesVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Map;

@Service
@Transactional
public class NodeServiceImpl implements NodeService {


    @Autowired
    NodeDao nodeDao;
    @Autowired
    NodeRulesDao nodeRulesDao;
    @Autowired
    NodeRulesRangeDao nodeRulesRangeDao;


    @Override
    public NodeDto saveNode(NodeDto node, String domain) throws Exception {
        //添加节点规则
        node.setDomain(domain);
        NodeDto save = nodeDao.save(node);
        if (null == save) {
            throw new Exception("添加节点失败");
        }
        //添加节点规则明细
        NodeDto nodeRule = nodeRulesDao.saveNodeRules(save);
        if (null == nodeRule) {
            throw new Exception("添加节点规则明细失败");
        }
        //添加节点规则明细作用域
        NodeDto range = nodeRulesRangeDao.saveNodeRuleRange(nodeRule);
        if (null == range) {
            throw new Exception("添加节点规则明细作用域失败");
        }

        return range;
    }

    @Override
    public Node getRoot(String domain,String deptTreeType) {
        return nodeDao.getByCode(domain, deptTreeType,"").get(0);
    }

    @Override
    public List<Node> getByCode(String domain,String deptTreeType, String nodeCode) {
        return nodeDao.getByCode(domain,deptTreeType, nodeCode);
    }

    @Override
    public NodeDto deleteNode(Map<String, Object> arguments, String id) throws Exception {
        //根据id查询规则是否为禁用状态
        List<NodeRules> nodeRules = nodeRulesDao.getByNodeAndType((String) arguments.get("id"), null, true);
        if (nodeRules.size() > 0) {
            throw new Exception("有节点包含启用规则,请关闭后删除");
        }
        //删除node
        Node node = nodeDao.findNodes(arguments, id);
        NodeDto nodeDto = new NodeDto(node);
        Integer integer = nodeDao.deleteNode(arguments, id);
        if (integer > 0) {
            //删除rule
            List<NodeRulesVo> rules = nodeRulesDao.findNodeRulesByNodeId((String) arguments.get("id"));
            Integer i = nodeRulesDao.deleteNodeRules((String) arguments.get("id"));

            //删除range
            if (i > 0) {

                for (NodeRulesVo rule : rules) {
                    List<NodeRulesRange> byRulesId = nodeRulesRangeDao.getByRulesId(rule.getId());
                    Integer flag = nodeRulesRangeDao.deleteNodeRulesRange(rule.getId());
                    if (flag > 0) {
                        rule.setNodeRulesRanges(byRulesId);
                    } else {
                        throw new Exception("删除节点规则作用域失败");
                    }
                }
            }
            nodeDto.setNodeRules(rules);
            return nodeDto;
        }


        return null;
    }

    @Override
    public Node findNodes(Map<String, Object> arguments, String id) {
        Node node = nodeDao.findNodes(arguments, id);
        return node;
    }

    @Override
    public NodeDto updateNode(NodeDto nodeDto) {
        return null;
    }


}
