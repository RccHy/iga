package com.qtgl.iga.service.impl;


import com.qtgl.iga.AutoUpRunner;
import com.qtgl.iga.bean.NodeDto;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.dao.NodeRulesDao;
import com.qtgl.iga.service.*;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import com.qtgl.iga.vo.NodeRulesVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class NodeRulesServiceImpl implements NodeRulesService {


    @Resource
    NodeRulesDao nodeRulesDao;
    @Resource
    NodeRulesRangeService nodeRulesRangeService;
    @Resource
    NodeService nodeService;
    @Resource
    UpstreamTypeService upstreamTypeService;
    @Resource
    DomainIgnoreService domainIgnoreService;

    @Override
    public List<NodeRules> findNodeRules(Map<String, Object> arguments, String domain) {
        return nodeRulesDao.findNodeRules(arguments, domain);
    }

    @Override
    public NodeRulesVo deleteRules(Map<String, Object> arguments, String domain) {
        String id = (String) arguments.get("id");
        //  查询rule  删除编辑中 或正式(person或occupy)
        NodeRules nodeRules = nodeRulesDao.findNodeRulesById(id, null);
        if (null != nodeRules) {
            Node nodeByNodeRuleId = nodeService.finNodeById(nodeRules.getNodeId());
            if (null == nodeByNodeRuleId) {
                throw new CustomException(ResultCode.FAILED, "删除规则失败,当前规则没有合法的node节点");
            }
            if (StringUtils.isNotBlank(AutoUpRunner.superDomainId) && AutoUpRunner.superDomainId.equals(nodeByNodeRuleId.getDomain())) {
                //忽略
                DomainIgnore domainIgnore = new DomainIgnore();
                domainIgnore.setDomain(domain);
                domainIgnore.setNodeRuleId(nodeRules.getId());
                domainIgnoreService.save(domainIgnore);
                return new NodeRulesVo();
            } else {
                //   查询删除规则拉取的数据子节点是否有规则
                List<String> codes = (List<String>) arguments.get("codes");
                String type = (String) arguments.get("type");
                if (null != codes && codes.size() > 0) {
                    //根据code查询是否有对应的规则
                    for (String code : codes) {
                        List<Node> nodes = nodeService.findNodesByCode(code, domain, type);
                        if (null != nodes && nodes.size() > 0) {
                            for (Node node : nodes) {
                                HashMap<String, Object> hashMap = new HashMap<>();
                                hashMap.put("id", node.getId());
                                nodeService.deleteNode(hashMap, domain);
                            }
                        }
                    }
                }
                //查询是否有range需要删除
                List<NodeRulesRange> ranges = nodeRulesRangeService.getByRulesId(id, null);
                //删除range
                if (null != ranges && ranges.size() > 0) {
                    Integer i = nodeRulesRangeService.deleteNodeRulesRangeByRuleId(id);
                    if (!(i > 0)) {
                        throw new CustomException(ResultCode.FAILED, "删除range失败");
                    }
                }

                //删除rules
                Integer flag = nodeRulesDao.deleteNodeRulesById(id);
                //查询node
                List<NodeRulesVo> nodeRulesByNodeId = nodeRulesDao.findNodeRulesByNodeId(nodeRules.getNodeId(), null);
                //node下没有对应的nodeRules则将node删除
                if (CollectionUtils.isEmpty(nodeRulesByNodeId)) {
                    nodeService.deleteNodeById(nodeRules.getNodeId(), domain);
                }

                if (flag > 0) {
                    return new NodeRulesVo();
                } else {
                    throw new CustomException(ResultCode.FAILED, "删除rule失败");
                }
            }


        } else {
            throw new CustomException(ResultCode.FAILED, "删除rule失败,请检查当前标识");
        }
    }

    @Override
    public NodeRulesVo saveRules(NodeRulesVo nodeRules) throws Exception {
//        ArrayList<NodeRulesRange> nodeRulesRanges = new ArrayList<>();
        //添加rules
        NodeRulesVo nodeRulesVo = nodeRulesDao.saveNodeRules(nodeRules);
//        //添加range
//        if (null == nodeRulesVo) {
//            throw new Exception("添加节点规则失败");
//        }
//        if (null != nodeRulesVo.getNodeRulesRanges()) {
//            for (NodeRulesRange nodeRulesRange : nodeRulesVo.getNodeRulesRanges()) {
//                NodeRulesRange range = nodeRulesRangeDao.saveNodeRuleRange(nodeRulesRange);
//                if (null == range) {
//                    throw new Exception("添加节点规则作用域失败");
//                }
//                nodeRulesRanges.add(range);
//
//            }
//        }
//        nodeRulesVo.setNodeRulesRanges(nodeRulesRanges);
        return nodeRulesVo;
    }

    @Override
    public NodeRulesVo updateRules(NodeRulesVo nodeRules) {
        //清除所有range
        Integer integer = nodeRulesRangeService.deleteNodeRulesRangeByRuleId(nodeRules.getId());
        if (!(integer > 0)) {
            throw new CustomException(ResultCode.FAILED, "删除range失败");
        }
        List<NodeRulesRange> ranges = null;
        if (null != nodeRules.getNodeRulesRanges()) {
            //修改range
            for (NodeRulesRange rulesRange : nodeRules.getNodeRulesRanges()) {
                NodeRulesRange nodeRulesRange;
                nodeRulesRange = nodeRulesRangeService.saveNodeRuleRange(rulesRange);

                if (null == nodeRulesRange) {
                    throw new CustomException(ResultCode.FAILED, "操作节点作用域失败");
                }
                ranges.add(nodeRulesRange);

            }
        }

        //修改rules
        NodeRules rules = nodeRulesDao.updateRules(nodeRules);
        NodeRulesVo nodeRulesVo = new NodeRulesVo(rules);
        nodeRulesVo.setNodeRulesRanges(ranges);
        return nodeRulesVo;
    }

    @Override
    public List<NodeRulesVo> deleteBatchRules(Map<String, Object> arguments, String id) throws Exception {

        List<String> ruleIds = (List<String>) arguments.get("ids");
        ArrayList<NodeRulesVo> nodeRulesVos = new ArrayList<>();
        for (String ruleId : ruleIds) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("id", ruleId);
            map.put("type", arguments.get("type"));
            NodeRulesVo nodeRulesVo = deleteRules(map, id);
            nodeRulesVos.add(nodeRulesVo);
        }

        return nodeRulesVos;
    }

    @Override
    public List<NodeRules> deleteBatchRules(List<NodeRules> nodeRules, String domain) {
        for (NodeRules nodeRule : nodeRules) {
            deleteRules(nodeRule, domain);
        }
        return nodeRules;
    }

    @Override
    public List<NodeRules> getByNodeAndType(String nodeId, int type, Boolean active, int status) {
        return nodeRulesDao.getByNodeAndType(nodeId, type, active, status);
    }

    @Override
    public List<NodeRulesVo> findNodeRulesByUpStreamIdAndType(List<String> ids, String type, String domain, Integer status) {
        List<UpstreamType> upstreamTypes = upstreamTypeService.findByUpstreamIds(ids, domain);
        if (!CollectionUtils.isEmpty(upstreamTypes)) {
            List<String> upstreamTypeIds = upstreamTypes.stream().map(UpstreamType::getId).collect(Collectors.toList());
            return nodeRulesDao.findNodeRulesByUpStreamTypeIdsAndType(upstreamTypeIds, type, domain, status);
        }

        return null;
    }

    @Override
    public List<NodeRules> findNodeRulesByService(String serviceName, String domain, String synType) {
        return nodeRulesDao.findNodeRulesByService(serviceName, domain, synType);
    }

    @Override
    public NodeDto saveNodeRules(NodeDto save) {
        return nodeRulesDao.saveNodeRules(save);
    }

    @Override
    public List<NodeRulesVo> findNodeRulesByNodeId(String nodeId, Integer status) {
        //根据权威源及权威源类型判断是否需要忽略

        return nodeRulesDao.findNodeRulesByNodeId(nodeId, status);
    }

    @Override
    public Integer deleteNodeRules(String id) {
        return nodeRulesDao.deleteNodeRules(id);
    }

    @Override
    public Integer makeNodeRulesToHistory(String ruleId, Integer status) {
        return nodeRulesDao.makeNodeRulesToHistory(ruleId, status);
    }

    @Override
    public List<NodeRules> findNodeRulesByServiceKey(String serviceKey, Integer status, Integer synWay) {
        return nodeRulesDao.findNodeRulesByServiceKey(serviceKey, status, synWay);
    }

    @Override
    public List<NodeRules> findNodeRulesByUpStreamTypeId(String upstreamTypeId, Integer status) {
        return nodeRulesDao.findNodeRulesByUpStreamTypeId(upstreamTypeId, status);
    }

    @Override
    public List<NodeRules> findNodeRulesByDomain(String superDomainId, Integer status, String type) {
        return nodeRulesDao.findNodeRulesByDomain(superDomainId, status, type);
    }

    @Override
    public List<NodeRulesVo> findSuperNodeRulesByNodeId(String nodeId, Integer status, String domainId) {
        List<NodeRulesVo> nodeRulesByNodeId = findNodeRulesByNodeId(nodeId, status);
        //if (!CollectionUtils.isEmpty(nodeRulesByNodeId)) {
        //    //查找本租户是否有忽略的超级租户权威源类型
        //    List<DomainIgnore> domainIgnores = domainIgnoreService.findByDomain(domainId);
        //    if (!CollectionUtils.isEmpty(domainIgnores)) {
        //        List<String> collect = domainIgnores.stream().map(DomainIgnore::getUpstreamTypeId).collect(Collectors.toList());
        //        ArrayList<NodeRulesVo> list = new ArrayList<>();
        //        for (NodeRulesVo nodeRulesVo : nodeRulesByNodeId) {
        //            if (collect.contains(nodeRulesVo.getUpstreamTypesId())) {
        //                //当前规则被忽略
        //                nodeRulesVo.setIsIgnore(true);
        //            }
        //            list.add(nodeRulesVo);
        //        }
        //        return list;
        //    }
        //}
        return nodeRulesByNodeId;
    }

    @Override
    public List<NodeRulesVo> findNodeRulesByNodeIds(List<String> nodeIds,String domain) {
        return nodeRulesDao.findNodeRulesByNodeIds(nodeIds,domain);
    }

    @Override
    public NodeRules findNodeRulesById(String ruleId, Integer status) {
        return nodeRulesDao.findNodeRulesById(ruleId, status);
    }

    public NodeRules deleteRules(NodeRules rules, String domain) {
        //查询是否有range需要删除
        List<NodeRulesRange> ranges = nodeRulesRangeService.getByRulesId(rules.getId(), null);
        //删除range
        if (null != ranges && ranges.size() > 0) {
            Integer i = nodeRulesRangeService.deleteNodeRulesRangeByRuleId(rules.getId());
            if (!(i > 0)) {
                throw new CustomException(ResultCode.FAILED, "删除range失败");
            }
        }
        //删除rules
        Integer flag = nodeRulesDao.deleteNodeRulesById(rules.getId());
        //查询node
        List<NodeRulesVo> nodeRulesByNodeId = nodeRulesDao.findNodeRulesByNodeId(rules.getNodeId(), null);
        //node下没有对应的nodeRules则将node删除
        if (CollectionUtils.isEmpty(nodeRulesByNodeId)) {
            nodeService.deleteNodeById(rules.getNodeId(), domain);
        }
        //if (flag > 0) {
        //    return new NodeRulesVo();
        //} else {
        //    throw new CustomException(ResultCode.FAILED, "删除rule失败");
        //}
        return new NodeRulesVo();
    }
}
