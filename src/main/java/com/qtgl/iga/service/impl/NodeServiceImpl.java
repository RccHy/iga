package com.qtgl.iga.service.impl;

import com.qtgl.iga.AutoUpRunner;
import com.qtgl.iga.bean.NodeDto;
import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bean.UpstreamDto;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.dao.NodeDao;
import com.qtgl.iga.service.*;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import com.qtgl.iga.vo.NodeRulesVo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Transactional
public class NodeServiceImpl implements NodeService {

    public static Logger logger = LoggerFactory.getLogger(NodeServiceImpl.class);

    @Resource
    NodeDao nodeDao;
    @Resource
    NodeRulesService nodeRulesService;
    @Resource
    NodeRulesRangeService nodeRulesRangeService;
    @Resource
    TaskLogService taskLogService;
    @Resource
    UpstreamService upstreamService;


    @Override
    public NodeDto saveNode(NodeDto node, String domain) {
        if (null == node.getLocal() || node.getLocal()) {
            //删除原有数据
            if (null != node.getId()) {
                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("id", node.getId());
                hashMap.put("type", node.getType());
                deleteNode(hashMap, domain);
            }
        } else {
            node.setId(UUID.randomUUID().toString());
        }

        //保留版本号

        if (null == node.getCreateTime()) {
            List<Node> nodesByStatusAndType = nodeDao.findNodesByStatusAndType(node.getStatus(), node.getType(), domain, null);
            if (null != nodesByStatusAndType) {
                node.setCreateTime(nodesByStatusAndType.get(0).getCreateTime());
            }
        }

        //添加节点规则
        node.setDomain(domain);
        NodeDto save = nodeDao.save(node);
        if (null == save) {
            throw new CustomException(ResultCode.FAILED, "添加节点失败");
        }
        if (!CollectionUtils.isEmpty(node.getNodeRules())) {
            //获取超级租户的规则
            if (StringUtils.isNotBlank(AutoUpRunner.superDomainId)) {
                List<NodeRules> superNodeRules = nodeRulesService.findNodeRulesByDomain(AutoUpRunner.superDomainId, 0, node.getType());
                if (!CollectionUtils.isEmpty(superNodeRules)) {
                    Map<String, NodeRules> superNodeRulesMap = superNodeRules.stream().collect(Collectors.toMap(NodeRules::getId, n -> n));
                    ArrayList<NodeRulesVo> resultNodeRules = new ArrayList<>();
                    //剔除掉超级租户的规则
                    for (NodeRulesVo nodeRule : save.getNodeRules()) {
                        if (!superNodeRulesMap.containsKey(nodeRule.getId())) {
                            nodeRule.setNodeId(save.getId());
                            resultNodeRules.add(nodeRule);
                        }
                    }
                    save.setNodeRules(resultNodeRules);

                }
            }
            //添加节点规则明细
            NodeDto nodeRule = nodeRulesService.saveNodeRules(save);
            if (null == nodeRule) {
                throw new CustomException(ResultCode.FAILED, "添加节点规则明细失败");
            }
            //添加节点规则明细作用域
            NodeDto range = nodeRulesRangeService.saveNodeRuleRange(nodeRule);
            if (null == range) {
                throw new CustomException(ResultCode.FAILED, "添加节点规则明细作用域失败");
            }
            return range;
        }

        return save;

    }

    @Override
    public Node getRoot(String domain, String deptTreeType) {
        return nodeDao.getByCode(domain, deptTreeType, "", 0, "dept").get(0);
    }

    @Override
    public List<Node> getByCode(String domain, String deptTreeType, String nodeCode) {
        return nodeDao.getByCode(domain, deptTreeType, nodeCode, 0, "dept");
    }

    @Override
    public NodeDto deleteNode(Map<String, Object> arguments, String domainId) {
        String id = (String) arguments.get("id");
        //根据id 查询是否为本租户的节点
        List<Node> nodeList = nodeDao.findNodeById(id);

        if (!CollectionUtils.isEmpty(nodeList)) {
            if (nodeList.size() > 1) {
                throw new CustomException(ResultCode.FAILED, "删除节点失败,当前标识不唯一");
            }
            Node node = nodeList.get(0);
            if (StringUtils.isNotBlank(AutoUpRunner.superDomainId) && AutoUpRunner.superDomainId.equals(node.getDomain())) {
                throw new CustomException(ResultCode.FAILED, "删除节点失败,当前节点不属于当前租户");
            }
        }

        //根据id查询规则是否为禁用状态
        Integer i = 0;
        Integer flag = 0;

        List<NodeDto> nodes = findNodes(arguments, domainId, false);
        if (CollectionUtils.isEmpty(nodes)) {
            return null;
        }

        NodeDto nodeDto = nodes.get(0);

        Integer status = (Integer) arguments.get("status");
        List<NodeRulesVo> rules = nodeRulesService.findNodeRulesByNodeId(id, status);

        if (null != rules) {

            //删除range
            for (NodeRulesVo rule : rules) {
                List<NodeRulesRange> byRulesId = nodeRulesRangeService.getByRulesId(rule.getId(), status);
                flag = nodeRulesRangeService.deleteNodeRulesRangeByRuleId(rule.getId());
                if (flag >= 0) {
                    rule.setNodeRulesRanges(byRulesId);
                } else {
                    throw new CustomException(ResultCode.FAILED, "删除节点规则作用域失败");
                }
            }
        }
        if (null != rules) {
            //删除rule
            i = nodeRulesService.deleteNodeRules(id);
            nodeDto.setNodeRules(rules);
        }
        if (i < 0 && null != rules) {
            throw new CustomException(ResultCode.FAILED, "删除节点规则失败");
        }
        //如果节点规则明细为空,直接删除node并返回
        //删除node
        Integer integer = nodeDao.deleteNode(arguments, domainId);
        if (integer >= 0) {

            return nodeDto;
        }


        return null;
    }

    /**
     * 外部接口直接调用  解析复杂入参
     *
     * @param arguments 入参
     * @param domainId  租户id
     * @param flag      是否查询超级租户相关信息
     * @return
     */
    @Override
    public List<NodeDto> findNodes(Map<String, Object> arguments, String domainId, Boolean flag) {

        //获取当前租户的node
        List<Node> nodeList = nodeDao.findNodes(arguments, domainId);
        //查询是否有超级租户规则
        List<Node> superNodeList = new ArrayList<>();
        if (flag) {
            if (StringUtils.isNotBlank(AutoUpRunner.superDomainId)) {
                superNodeList = nodeDao.findNodes(arguments, AutoUpRunner.superDomainId);
            }
        }
        return getNodeDtoByNodeList(nodeList, superNodeList, domainId);


    }

    private List<NodeDto> getNodeDtoByNodeList(List<Node> nodeList, List<Node> superNodeList, String domain) {
        List<NodeDto> nodeDos = new ArrayList<>();
        List<String> ignoreUpstreamTypeIds = new ArrayList<>();
        if (StringUtils.isNotBlank(AutoUpRunner.superDomainId)) {
            List<UpstreamType> ignoreUpstreamTypes = new ArrayList<>();
            //获取呗本租户覆盖的权威源类型
            List<UpstreamDto> byRecover = upstreamService.findByRecover(domain);
            //装配涉及的权威源类型来忽略返回
            if (!CollectionUtils.isEmpty(byRecover)) {
                for (UpstreamDto upstreamDto : byRecover) {
                    List<UpstreamType> upstreamTypes = upstreamDto.getUpstreamTypes();
                    if (!CollectionUtils.isEmpty(upstreamTypes)) {
                        ignoreUpstreamTypes.addAll(upstreamTypes);
                    }
                }
            }
            if (!CollectionUtils.isEmpty(ignoreUpstreamTypes)) {
                ignoreUpstreamTypeIds = ignoreUpstreamTypes.stream().map(UpstreamType::getId).collect(Collectors.toList());
            }
        }

        if (!CollectionUtils.isEmpty(nodeList)) {
            //当前租户下有规则,根据node查询对应规则
            ArrayList<Node> allNodes = new ArrayList<>(nodeList);
            //本租户规则与超级租户规则合并对应关系  localDomain-> superDomain
            Map<String, String> keyMap = new ConcurrentHashMap<>();

            if (!CollectionUtils.isEmpty(superNodeList)) {
                //处理node 合并指向关系
                Map<String, String> superNodeMap = superNodeList.stream().collect(Collectors.toMap(node -> (StringUtils.isBlank(node.getNodeCode()) ? "*" : node.getNodeCode()) + "_" + (StringUtils.isBlank(node.getDeptTreeType()) ? "*" : node.getDeptTreeType()), Node::getId));
                for (Node node : nodeList) {
                    String key = (StringUtils.isBlank(node.getNodeCode()) ? "*" : node.getNodeCode()) + "_" + (StringUtils.isBlank(node.getDeptTreeType()) ? "*" : node.getDeptTreeType());
                    if (superNodeMap.containsKey(key)) {
                        keyMap.put(node.getId(), superNodeMap.get(key));
                    }
                }
                //超级租户下有规则 加入集合一并查询规则
                allNodes.addAll(superNodeList);
            }

            //获取所有规则
            List<String> nodeIds = allNodes.stream().map(Node::getId).collect(Collectors.toList());
            List<NodeRulesVo> nodeRulesVos = nodeRulesService.findNodeRulesByNodeIds(nodeIds);

            if (!CollectionUtils.isEmpty(nodeRulesVos)) {
                //根据node分组
                Map<String, List<NodeRulesVo>> rulesMap = nodeRulesVos.stream().collect(Collectors.groupingBy(NodeRules::getNodeId));
                for (Node node : nodeList) {
                    NodeDto nodeDto = new NodeDto(node);
                    nodeDto.setLocal(true);
                    if (rulesMap.containsKey(node.getId())) {
                        List<NodeRulesVo> resultRules = rulesMap.get(node.getId());
                        //有跟超级租户对应的node
                        if (keyMap.containsKey(node.getId()) && rulesMap.containsKey(keyMap.get(node.getId()))) {
                            resultRules.addAll(rulesMap.get(keyMap.get(node.getId())));
                        }
                        dealWithNodeRules(nodeDto, resultRules, true, ignoreUpstreamTypeIds);

                    }
                    nodeDos.add(nodeDto);
                }
                if (!CollectionUtils.isEmpty(superNodeList)) {
                    for (Node node : superNodeList) {
                        //当前租户没有对应的超级租户 节点 单独装配
                        if (!keyMap.containsValue(node.getId()) && rulesMap.containsKey(node.getId())) {
                            NodeDto nodeDto = new NodeDto(node);
                            nodeDto.setLocal(false);
                            List<NodeRulesVo> resultRules = rulesMap.get(node.getId());
                            dealWithNodeRules(nodeDto, resultRules, false, ignoreUpstreamTypeIds);
                            nodeDos.add(nodeDto);

                        }
                    }
                }
            }
        } else if (!CollectionUtils.isEmpty(superNodeList)) {
            //当前租户下没有规则,根据超级租户的node 处理rules
            List<String> nodeIds = superNodeList.stream().map(Node::getId).collect(Collectors.toList());
            //获取规则
            List<NodeRulesVo> nodeRulesVos = nodeRulesService.findNodeRulesByNodeIds(nodeIds);
            if (!CollectionUtils.isEmpty(nodeRulesVos)) {
                //根据node分组
                Map<String, List<NodeRulesVo>> rulesMap = nodeRulesVos.stream().collect(Collectors.groupingBy(NodeRules::getNodeId));
                for (Node node : superNodeList) {
                    NodeDto nodeDto = new NodeDto(node);
                    //来自超级租户的规则node 标识为非本租户
                    nodeDto.setLocal(false);
                    if (rulesMap.containsKey(node.getId())) {
                        dealWithNodeRules(nodeDto, nodeRulesVos, false, ignoreUpstreamTypeIds);
                        nodeDto.setNodeRules(nodeRulesVos);
                    }
                    nodeDos.add(nodeDto);
                }
            }

        }
        return nodeDos;
    }

    private void dealWithNode(String domainId, ArrayList<NodeDto> nodeDos, List<Node> nodeList, Map<String, Node> superNodeMap, Boolean isLocal) {


        ////以当前租户为主,将超级租户相关规则补充
        //for (Node node : nodeList) {
        //    NodeDto nodeDto = new NodeDto(node);
        //    nodeDto.setLocal(isLocal);
        //
        //    String key = (StringUtils.isBlank(node.getNodeCode()) ? "*" : node.getNodeCode()) + "_" + (StringUtils.isBlank(node.getDeptTreeType()) ? "*" : node.getDeptTreeType());
        //    if (!CollectionUtils.isEmpty(superNodeMap) && superNodeMap.containsKey(key)) {
        //        Node superNode = superNodeMap.get(key);
        //        List<NodeRulesVo> superNodeRulesByNodeId = nodeRulesService.findSuperNodeRulesByNodeId(superNode.getId(), null, domainId);
        //        dealWithNodeRules(nodeDto, superNodeRulesByNodeId, false);
        //    }
        //
        //    List<NodeRulesVo> nodeRulesByNodeId = nodeRulesService.findNodeRulesByNodeId(node.getId(), null);
        //    dealWithNodeRules(nodeDto, nodeRulesByNodeId, true);
        //
        //    nodeDos.add(nodeDto);
        //}
        ////将当前租户没有超级租户有的node补充到结果集
        //if (!CollectionUtils.isEmpty(superNodeMap)) {
        //    Map<String, Node> collect = nodeList.stream().collect(Collectors.toMap((node -> (StringUtils.isBlank(node.getNodeCode()) ? "*" : node.getNodeCode()) + "_" + (StringUtils.isBlank(node.getDeptTreeType()) ? "*" : node.getDeptTreeType())), (node -> node)));
        //    for (String key : superNodeMap.keySet()) {
        //        if (!collect.containsKey(key)) {
        //            Node node = superNodeMap.get(key);
        //            NodeDto nodeDto = new NodeDto(node);
        //            nodeDto.setLocal(false);
        //            List<NodeRulesVo> superNodeRulesByNodeId = nodeRulesService.findSuperNodeRulesByNodeId(node.getId(), null, domainId);
        //            dealWithNodeRules(nodeDto, superNodeRulesByNodeId, false);
        //            nodeDos.add(nodeDto);
        //        }
        //    }
        //}


    }

    /**
     * /**
     *
     * @param nodeDto               节点容器
     * @param nodeRulesVos          需要处理的规则
     * @param isLocal               是否来自本租户
     * @param ignoreUpstreamTypeIds 被当前租户覆盖
     */
    private void dealWithNodeRules(NodeDto nodeDto, List<NodeRulesVo> nodeRulesVos, Boolean isLocal, List<String> ignoreUpstreamTypeIds) {
        List<NodeRulesVo> nodeRules = nodeDto.getNodeRules();
        //标识是否含有继承
        boolean flag = false;
        if (null != nodeRulesVos) {
            //根据rules查询对应的range
            for (NodeRulesVo nodeRulesVo : nodeRulesVos) {
                if (!CollectionUtils.isEmpty(ignoreUpstreamTypeIds)) {
                    if (null != nodeRulesVo.getUpstreamTypesId() && ignoreUpstreamTypeIds.contains(nodeRulesVo.getUpstreamTypesId())) {
                        //被覆盖的拉取规则 跳过
                        continue;
                    }
                    if (null != nodeRulesVo.getServiceKey() && ignoreUpstreamTypeIds.contains(nodeRulesVo.getServiceKey())) {
                        //被覆盖的推送规则 跳过
                        continue;
                    }
                }
                nodeRulesVo.setLocal(isLocal);
                //rule指向的nodeId 与当前 装配的node 不一致  为超级租户补充的规则
                if (!nodeRulesVo.getNodeId().equals(nodeDto.getId())) {
                    nodeRulesVo.setNodeId(nodeDto.getId());
                    nodeRulesVo.setLocal(false);
                }
                if (StringUtils.isBlank(nodeRulesVo.getInheritId())) {
                    List<NodeRulesRange> byRulesId = nodeRulesRangeService.getByRulesId(nodeRulesVo.getId(), null);
                    nodeRulesVo.setNodeRulesRanges(byRulesId);
                } else {
                    flag = true;
                }
                nodeRules.add(nodeRulesVo);
            }
            nodeDto.setInherit(flag);
            nodeDto.setNodeRules(nodeRules);
        }
    }


    @Override
    public NodeDto updateNode(NodeDto nodeDto) {
        return null;
    }

    @Override
    public List<NodeDto> findNodesPlus(Map<String, Object> arguments, String domainId) {

        //获取node
        List<Node> nodeList = nodeDao.findNodesPlus(arguments, domainId);

        //查询是否有超级租户规则
        List<Node> superNodeList = new ArrayList<>();
        if (StringUtils.isNotBlank(AutoUpRunner.superDomainId)) {
            superNodeList = nodeDao.findNodesPlus(arguments, AutoUpRunner.superDomainId);
        }
        return getNodeDtoByNodeList(nodeList, superNodeList, domainId);
    }

    @Override
    public List<Node> findNodesByCode(String code, String domain, String type) {
        return nodeDao.findNodesByCode(code, domain, type);
    }


    /**
     * @param arguments
     * @param domain
     * @Description: 根据传入版本回滚或应用对应版本(不传入回滚以当前生产版本作为回滚版本 ( 无生产版本, 则提示无法回滚))
     * 回滚:先获取传入的版本(不传入的话,则取当前生产版本),然后将所有版本置为历史版本,最后根据版本号将对应版本应用为生产版本
     * 应用:获取传入的版本(不传入的话,则取编辑中版本),然后将所有版本置为历史版本,最后根据版本号将对应版本应用为生产版本
     * @return: com.qtgl.iga.bo.Node
     */
    @Override
    public Node applyNode(Map<String, Object> arguments, String domain) {
        //传入的版本数据类型转换为数据库对应类型
        Timestamp version = null;
        if (null != arguments.get("version")) {
            version = new Timestamp(((Date) arguments.get("version")).getTime());
        }
        String type = (String) arguments.get("type");
        Boolean mark = (Boolean) arguments.get("mark");

        //生产版本  用于回滚
        List<Node> proNodes = new ArrayList<>();
        //当前编辑中的规则
        List<Node> editNodes = new ArrayList<>();
        //mark为true 则为回滚  false为应用
        if (mark) {
            //查询有无生产版本
            proNodes = nodeDao.findNodesByStatusAndType(0, type, domain, null);
            if (null == proNodes) {
                return null;
            }

        }
        //查询是否在同步中
        List<TaskLog> logList = taskLogService.findByStatus(domain);
        if (!CollectionUtils.isEmpty(logList)) {
            if ("doing".equals(logList.get(0).getStatus())) {
                throw new CustomException(ResultCode.FAILED, "数据正在同步,应用失败,请稍后再试");

            }
        }
        if (null == version) {
            if (mark) {
                //回滚查询生产版本
                //List<Node> nodes = nodeDao.findNodesByStatusAndType(0, type, domain, null);
                if (!CollectionUtils.isEmpty(proNodes)) {
                    version = new Timestamp(proNodes.get(0).getCreateTime());
                }
            } else {
                //应用查询编辑中版本

                editNodes = nodeDao.findNodesByStatusAndType(1, type, domain, null);
                if (!CollectionUtils.isEmpty(editNodes)) {
                    version = new Timestamp(editNodes.get(0).getCreateTime());
                }

            }
        }
        if (mark) {
            //查询编辑中的node
            // 加 type查询
            if (!CollectionUtils.isEmpty(editNodes)) {
                for (Node node : editNodes) {
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("id", node.getId());
                    map.put("status", 1);
                    map.put("type", type);
                    deleteNode(map, domain);
                }
            }
        }
        //将所有版本改为历史版本
        Integer nodeHistory = getInteger(null, type, domain, null);
        //将传入版本改为正式版本
        if (null != version) {
            Integer rangeNew = getInteger(null, type, domain, version);
            if ((nodeHistory >= 0) && (rangeNew >= 0)) {
                return new Node();
            }
        }
        return null;
    }

    private Integer getInteger(Integer status, String type, String domain, Timestamp version) {

        List<Node> nodes = nodeDao.findNodesByStatusAndType(status, type, domain, version);
        if (!CollectionUtils.isEmpty(nodes)) {
            for (Node node : nodes) {
                if (node.getStatus().equals(3)) {
                    continue;
                }
                List<NodeRulesVo> rules = nodeRulesService.findNodeRulesByNodeId(node.getId(), null);
                if (null != rules && rules.size() > 0) {
                    for (NodeRulesVo rule : rules) {
                        List<NodeRulesRange> ranges = nodeRulesRangeService.getByRulesId(rule.getId(), null);
                        if (null != ranges && ranges.size() > 0) {
                            for (NodeRulesRange range : ranges) {
                                Integer rangeHistory = nodeRulesRangeService.makeNodeRulesRangesToHistory(range.getId(), null == version ? 2 : 0);
                                if (rangeHistory < 0) {
                                    logger.error("应用失败 range {}", range);
                                    throw new CustomException(ResultCode.FAILED, "应用range失败");
                                }
                            }
                        }
                        Integer ruleHistory = nodeRulesService.makeNodeRulesToHistory(rule.getId(), null == version ? 2 : 0);
                        if (ruleHistory < 0) {
                            logger.error("应用失败rule  {}", rule);
                            throw new CustomException(ResultCode.FAILED, "应用rule失败");
                        }
                    }
                }
                Integer nodeHistory = nodeDao.makeNodeToHistory(domain, null == version ? 2 : 0, node.getId());
                if (nodeHistory < 0) {
                    logger.error("应用失败  {}", nodes);
                    throw new CustomException(ResultCode.FAILED, "应用node失败");
                }
            }
        }

        return 1;
    }

    /**
     * 弃用方法
     *
     * @param arguments
     * @param domain
     * @return
     */
    @Override
    public Node rollbackNode(Map<String, Object> arguments, String domain) {
        Object version = arguments.get("version");
        if (null == version) {
            throw new CustomException(ResultCode.FAILED, "版本非法,请确认");
        }
        //  删除编辑中的node
        //查询编辑中的node
        List<NodeDto> nodes = findNodes(arguments, domain, false);
        for (NodeDto node : nodes) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("id", node.getId());
            map.put("status", 1);
            NodeDto nodeDto = deleteNode(map, domain);
            if (null == nodeDto) {
                throw new CustomException(ResultCode.FAILED, "回滚失败");
            }
        }
        return new Node();
    }

    /**
     * @param arguments
     * @param domain
     * @param type
     * @Description: 判断是否有编辑中node
     * @return: java.lang.Integer
     */
    @Override
    public Integer judgeEdit(Map<String, Object> arguments, DomainInfo domain, String type) {
        List<NodeDto> nodeList = null;
        Integer status = (Integer) arguments.get("status");
        if (null == status) {
            throw new CustomException(ResultCode.FAILED, "状态不能为空");
        }
        if (1 == status) {
            //查看是否有治理中的规则
            nodeList = findNodesByStatusAndType(1, type, domain.getId(), null);
        }
        //   如果状态为编辑,并且没有治理中的数据
        if (CollectionUtils.isEmpty(nodeList) && status == 1) {
            //复制,再返回
            List<NodeDto> nodes = findNodesByStatusAndType(0, type, domain.getId(), null);
            //复制数据
            if (!CollectionUtils.isEmpty(nodes)) {
                ConcurrentHashMap<String, String> inheritMap = new ConcurrentHashMap<>();
                for (NodeDto node : nodes) {
                    node.setId(null);
                    node.setStatus(1);
                    node.setCreateTime(System.currentTimeMillis());
                    if (null != node.getNodeRules()) {
                        for (NodeRulesVo nodeRule : node.getNodeRules()) {
                            String nodeRulesNewId = UUID.randomUUID().toString().replace("-", "");
                            inheritMap.put(nodeRule.getId(), nodeRulesNewId);
                            nodeRule.setId(nodeRulesNewId);
                            nodeRule.setStatus(1);
                            if (null != nodeRule.getNodeRulesRanges()) {
                                for (NodeRulesRange nodeRulesRange : nodeRule.getNodeRulesRanges()) {
                                    nodeRulesRange.setId(null);
                                    nodeRulesRange.setStatus(1);
                                }
                            }
                        }
                        //继承规则inherit_id赋值
                        for (NodeRulesVo nodeRule : node.getNodeRules()) {
                            if (null != nodeRule.getInheritId()) {
                                String newInheritId = inheritMap.get(nodeRule.getInheritId());
                                nodeRule.setInheritId(newInheritId);
                            }
                        }
                    }

                    saveNode(node, domain.getId());
                }
            }

            return null;

        }
        return status;
    }

    @Override
    public List<NodeDto> findNodesByStatusAndType(Integer status, String type, String domainId, Timestamp version) {
        List<Node> nodesByStatusAndType = nodeDao.findNodesByStatusAndType(status, type, domainId, version);
        if (!CollectionUtils.isEmpty(nodesByStatusAndType)) {

            return getNodeDtoByNodeList(nodesByStatusAndType, null, domainId);
        }
        return null;
    }

    @Override
    public List<Node> getByTreeType(String domainId, String code, Integer status, String type) {
        return nodeDao.getByTreeType(domainId, code, status, type);
    }

    /**
     * 同步任务获取规则
     *
     * @param domainId
     * @param status
     * @param type
     * @param flag
     * @return
     */
    @Override
    public List<NodeDto> findNodes(String domainId, Integer status, String type, Boolean flag) {
        //获取当前租户的node
        List<Node> nodeList = nodeDao.findNodes(domainId, status, type);
        //查询是否有超级租户规则
        List<Node> superNodeList = new ArrayList<>();
        if (flag) {
            if (StringUtils.isNotBlank(AutoUpRunner.superDomainId)) {
                //超级租户规则只获取正式的
                superNodeList = nodeDao.findNodes(AutoUpRunner.superDomainId, 0, type);
            }
        }
        return getNodeDtoByNodeList(nodeList, superNodeList, domainId);
    }

    @Override
    public Node finNodeById(String nodeId) {
        List<Node> nodeById = nodeDao.findNodeById(nodeId);
        if (!CollectionUtils.isEmpty(nodeById)) {
            if (nodeById.size() == 1) {
                return nodeById.get(0);
            } else {
                throw new CustomException(ResultCode.FAILED, "当前标识的node不唯一");
            }
        }
        return null;
    }

    @Override
    public List<Node> nodeStatus(Map<String, Object> arguments, String domain) {
        Integer status = (Integer) arguments.get("status");
        String type = (String) arguments.get("type");
        if (null == status) {
            throw new CustomException(ResultCode.FAILED, "状态不能为空");
        }
        if (null == type) {
            throw new CustomException(ResultCode.FAILED, "类型不能为空");
        }
        return nodeDao.findByStatus(status, domain, type);
    }

    @Override
    public void updateNodeAndRules(List<NodeDto> nodes, List<TreeBean> beans) {
        //运算完成的结果集中不包含node所指定的挂载节点,则该规则需置为无效
        ArrayList<Node> invalidNodes = new ArrayList<>();
        ArrayList<NodeRulesVo> invalidNodeRules = new ArrayList<>();
        ArrayList<NodeRulesRange> invalidNodeRulesRanges = new ArrayList<>();
        if (!CollectionUtils.isEmpty(beans) && !CollectionUtils.isEmpty(nodes)) {
            Map<String, TreeBean> collect = beans.stream().collect(Collectors.toMap(TreeBean::getCode, treeBean -> treeBean));
            for (Node node : nodes) {
                if (!StringUtils.isBlank(node.getNodeCode())) {
                    if (!collect.containsKey(node.getNodeCode())) {
                        invalidNodes.add(node);
                        List<NodeRulesVo> nodeRulesVos = nodeRulesService.findNodeRulesByNodeId(node.getId(), null);
                        if (!CollectionUtils.isEmpty(nodeRulesVos)) {
                            invalidNodeRules.addAll(nodeRulesVos);
                            for (NodeRulesVo nodeRulesVo : nodeRulesVos) {
                                List<NodeRulesRange> nodeRulesRanges = nodeRulesRangeService.getByRulesId(nodeRulesVo.getId(), null);
                                if (!CollectionUtils.isEmpty(nodeRulesRanges)) {
                                    invalidNodeRulesRanges.addAll(nodeRulesRanges);
                                }
                            }
                        }
                    }
                }
            }
            nodeDao.updateNodeAndRules(invalidNodes, invalidNodeRules, invalidNodeRulesRanges);
        }
    }


    //todo 超级租户逻辑待处理
    @Override
    public List<Node> findByTreeTypeCode(String code, Integer status, String domain) {
        return nodeDao.findByTreeTypeCode(code, status, domain);
    }

    @Override
    public void deleteNodeById(String nodeId, String domain) {
        nodeDao.deleteNodeById(nodeId, domain);
    }

}
