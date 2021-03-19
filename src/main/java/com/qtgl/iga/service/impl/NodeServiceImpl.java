package com.qtgl.iga.service.impl;

import com.qtgl.iga.bean.NodeDto;
import com.qtgl.iga.bo.Node;
import com.qtgl.iga.bo.NodeRules;
import com.qtgl.iga.bo.NodeRulesRange;
import com.qtgl.iga.dao.NodeDao;
import com.qtgl.iga.dao.NodeRulesDao;
import com.qtgl.iga.dao.NodeRulesRangeDao;
import com.qtgl.iga.service.NodeService;
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
        NodeDto save = nodeDao.save(node);
        if(null==save){
            throw new Exception("添加节点失败");
        }
        //添加节点规则明细
        NodeDto nodeRule = nodeRulesDao.saveNodeRules(save);
        if(null==nodeRule){
            throw new Exception("添加节点规则明细失败");
        }
        //添加节点规则明细作用域
//        NodeDto range = nodeRulesRangeDao.saveNodeRuleRange(nodeRule);
//        if(null==range || range.getNodeRulesRanges().size()==0){
//            throw new Exception("添加节点规则明细作用域失败");
//        }

        return save;
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
    public NodeDto deleteNode(Map<String, Object> arguments, String id) {
        return null;
    }

    @Override
    public NodeDto findNodes(Map<String, Object> arguments, String id) {
        return null;
    }

    @Override
    public NodeDto updateNode(NodeDto nodeDto) {
        return null;
    }


}
