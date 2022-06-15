package com.qtgl.iga.service.impl;


import com.qtgl.iga.bean.UpstreamDto;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.dao.NodeRulesDao;
import com.qtgl.iga.dao.UpstreamDao;
import com.qtgl.iga.dao.impl.UpstreamTypeDaoImpl;
import com.qtgl.iga.service.NodeRulesService;
import com.qtgl.iga.service.UpstreamService;
import com.qtgl.iga.utils.RoleBingUtil;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class UpstreamServiceImpl implements UpstreamService {

    @Autowired
    UpstreamDao upstreamDao;
    @Autowired
    UpstreamTypeDaoImpl upstreamTypeDao;
    @Autowired
    NodeRulesDao nodeRulesDao;
    @Resource
    RoleBingUtil roleBingUtil;
    @Resource
    NodeRulesService nodeRulesService;

    @Override
    public List<Upstream> findAll(Map<String, Object> arguments, String domain) {
        return upstreamDao.findAll(arguments, domain);
    }

    @Override
    public Upstream deleteUpstream(Map<String, Object> arguments, String domain) throws Exception {

        //查看是否有关联node_rules
        List<UpstreamType> byUpstreamId = upstreamTypeDao.findByUpstreamId((String) arguments.get("id"));
        if (null != byUpstreamId && byUpstreamId.size() > 0) {
            for (UpstreamType upstreamType : byUpstreamId) {
                List<NodeRules> nodeRules = nodeRulesDao.findNodeRulesByUpStreamTypeId(upstreamType.getId(), null);
                List<NodeRules> oldNodeRules = nodeRulesDao.findNodeRulesByUpStreamTypeId(upstreamType.getId(), 2);
                //编辑和正式的规则提示
                if (null != nodeRules && nodeRules.size() > 0) {
                    throw new CustomException(ResultCode.FAILED, "有绑定的nodeRules规则,请查看后再删除");
                }
                //历史版本,提示
                if (null != oldNodeRules && oldNodeRules.size() > 0) {
                    //删除历史版本
                    nodeRulesService.deleteBatchRules(oldNodeRules, domain);
                    //throw new CustomException(ResultCode.FAILED, "有历史版本的nodeRules规则");
                }

            }
        }


        //删除权威源数据类型
        if (null != byUpstreamId && byUpstreamId.size() > 0) {
            Integer integer = upstreamTypeDao.deleteByUpstreamId((String) arguments.get("id"), domain);
            if (integer < 0) {

                throw new CustomException(ResultCode.FAILED, "删除权威源类型失败");
            }

        }
        //删除权威源数据
        Integer flag = upstreamDao.deleteUpstream((String) arguments.get("id"));
        if (flag > 0) {
            return new Upstream();
        } else {
            throw new CustomException(ResultCode.FAILED, "删除权威源失败");
        }

    }

    @Override
    public Upstream saveUpstream(Upstream upstream, String domain) throws Exception {
        return upstreamDao.saveUpstream(upstream, domain);
    }

    @Override
    public Upstream updateUpstream(Upstream upstream) throws Exception {

        return upstreamDao.updateUpstream(upstream);
    }

    @Override
    @Transactional
    public UpstreamDto saveUpstreamAndTypes(UpstreamDto upstreamDto, String id) throws Exception {
        // 添加权威源
        Upstream upstream = upstreamDao.saveUpstream(upstreamDto, id);
        UpstreamDto upstreamDb = new UpstreamDto(upstream);
        //添加权威源类型
        if (null == upstream) {
            throw new CustomException(ResultCode.ADD_UPSTREAM_ERROR, null, null, upstreamDto.getAppName());
        }
        ArrayList<UpstreamType> list = new ArrayList<>();
        List<UpstreamType> upstreamTypes = upstreamDto.getUpstreamTypes();
        if (null != upstreamTypes) {
            for (UpstreamType upstreamType : upstreamTypes) {
                upstreamType.setUpstreamId(upstream.getId());
                //校验名称重复
                List<UpstreamType> upstreamTypeList = upstreamTypeDao.findByUpstreamIdAndDescription(upstreamType);
                if (null != upstreamTypeList && upstreamTypeList.size() > 0) {
                    throw new CustomException(ResultCode.FAILED, "权威源类型描述重复");
                }
                UpstreamType upstreamTypeDb = upstreamTypeDao.saveUpstreamType(upstreamType, id);
                if (null != upstreamTypeDb) {
                    list.add(upstreamTypeDb);
                } else {
                    throw new CustomException(ResultCode.ADD_UPSTREAM_ERROR, null, null, upstreamDto.getAppName());
                }
            }
        }
        upstreamDb.setUpstreamTypes(list);

        return upstreamDb;
    }

    /**
     * @param arguments
     * @param id
     * @Description: 查询权威源及类型
     * @return: java.util.List<com.qtgl.iga.bean.UpstreamDto>
     */
    @Override
    public List<UpstreamDto> upstreamsAndTypes(Map<String, Object> arguments, String id) {
        ArrayList<UpstreamDto> upstreamDtos = new ArrayList<>();
        //查询权威源
        List<Upstream> upstreamList = upstreamDao.findAll(arguments, id);
        //查询权威源类型
        if (null != upstreamList && upstreamList.size() > 0) {
            for (Upstream upstream : upstreamList) {
                UpstreamDto upstreamDto = new UpstreamDto(upstream);
                List<UpstreamType> byUpstreamId = upstreamTypeDao.findByUpstreamId(upstream.getId());
                upstreamDto.setUpstreamTypes(byUpstreamId);
                upstreamDtos.add(upstreamDto);
            }
        }


        return upstreamDtos;
    }

    @Override
    public UpstreamDto updateUpstreamAndTypes(UpstreamDto upstreamDto) throws Exception {


        //修改权威源类型
        //1.判断node绑定状态
        if (null != upstreamDto && null != upstreamDto.getUpstreamTypes() && upstreamDto.getUpstreamTypes().size() > 0) {
            for (UpstreamType upstreamType : upstreamDto.getUpstreamTypes()) {
                //查看是否有关联node_rules
                List<NodeRules> nodeRules = nodeRulesDao.findNodeRulesByUpStreamTypeId(upstreamType.getId(), null);
                if (null != nodeRules && nodeRules.size() > 0) {
                    throw new CustomException(ResultCode.FAILED, "有绑定的node规则,请查看后再操作");
                }
            }
        }

        Upstream upstream = upstreamDao.updateUpstream(upstreamDto);

        if (null != upstream) {
            UpstreamDto upstreamVo = new UpstreamDto(upstream);
            //修改权威源
            ArrayList<UpstreamType> list = new ArrayList<>();
            List<UpstreamType> upstreamTypes = upstreamDto.getUpstreamTypes();
            for (UpstreamType upstreamType : upstreamTypes) {
                UpstreamType upstreamResult = null;
                //校验名称重复
                List<UpstreamType> upstreamTypeList = upstreamTypeDao.findByUpstreamIdAndDescription(upstreamType);
                if (null != upstreamTypeList && upstreamTypeList.size() > 0) {
                    throw new CustomException(ResultCode.FAILED, "权威源类型描述重复");
                }
                if (null != upstreamType.getId()) {
                    upstreamResult = upstreamTypeDao.updateUpstreamType(upstreamType);
                } else {
                    upstreamResult = upstreamTypeDao.saveUpstreamType(upstreamType, upstreamDto.getDomain());
                }
                if (null != upstreamResult) {
                    list.add(upstreamResult);
                } else {
                    throw new CustomException(ResultCode.UPDATE_UPSTREAM_ERROR, null, null, upstreamDto.getAppCode());
                }
            }
            upstreamVo.setUpstreamTypes(list);
            return upstreamVo;
        } else {

            throw new CustomException(ResultCode.UPDATE_UPSTREAM_ERROR, null, null, upstreamDto.getAppCode());
        }

    }

    @Override
    public void saveUpstreamAndTypesAndRoleBing(Upstream upstream, List<UpstreamType> upstreamTypes, List<Node> nodes, List<NodeRules> nodeRulesList, DomainInfo domainInfo) throws Exception {
        //// 权威源
        ////判重
        //upstreamDao.findByAppNameAndAppCode(upstreamDto.getAppName(), upstreamDto.getAppCode(), domain);
        //
        //String upstreamId = UUID.randomUUID().toString();
        //upstreamDto.setId(upstreamId);
        //// 处理权威源类型
        //ArrayList<UpstreamType> upstreamTypesRes = new ArrayList<>();
        //List<UpstreamType> upstreamTypes = upstreamDto.getUpstreamTypes();
        //if (null != upstreamTypes) {
        //    for (UpstreamType upstreamType : upstreamTypes) {
        //        upstreamType.setUpstreamId(upstreamId);
        //        //校验名称重复
        //        List<UpstreamType> upstreamTypeList = upstreamTypeDao.findByUpstreamIdAndDescription(upstreamType);
        //        if (null != upstreamTypeList && upstreamTypeList.size() > 0) {
        //            throw new CustomException(ResultCode.FAILED, "权威源类型描述重复");
        //        }
        //        upstreamType.setId(UUID.randomUUID().toString());
        //        upstreamTypesRes.add(upstreamType);
        //    }
        //}
        //upstreamDto.setUpstreamTypes(upstreamTypesRes);
        //添加roleBing
        ArrayList<String> deptPermissions = new ArrayList<>();
        deptPermissions.add("departments");
        deptPermissions.add("addDepartment");
        deptPermissions.add("editDepartment");
        deptPermissions.add("deleteDepartment");
        ArrayList<String> postPermissions = new ArrayList<>();
        postPermissions.add("posts");
        postPermissions.add("addPost");
        postPermissions.add("editPost");
        postPermissions.add("deletePost");
        ArrayList<String> personPermissions = new ArrayList<>();
        personPermissions.add("users");
        personPermissions.add("addUser");
        personPermissions.add("editUser");
        personPermissions.add("deleteUser");
        ArrayList<String> occupyPermissions = new ArrayList<>();
        occupyPermissions.add("triples");
        occupyPermissions.add("addTriple");
        occupyPermissions.add("editTriple");
        occupyPermissions.add("deleteTriple");


        if (!CollectionUtils.isEmpty(nodes) && !CollectionUtils.isEmpty(nodeRulesList)) {
            Map<String, List<NodeRules>> collect = nodeRulesList.stream().collect(Collectors.groupingBy(nodeRules -> nodeRules.getNodeId()));
            if (CollectionUtils.isEmpty(upstreamTypes)) {
                throw new CustomException(ResultCode.FAILED, "没有添加权威源权限");
            }
            Map<String, UpstreamType> upstreamTypeMap = upstreamTypes.stream().collect(Collectors.toMap((upstreamType -> upstreamType.getId()), (upstreamType -> upstreamType)));
            for (Node node : nodes) {
                if ("dept".equals(node.getType())) {
                    //组织机构
                    List<NodeRules> nodeRules = collect.get(node.getId());
                    if (!CollectionUtils.isEmpty(nodeRules)) {
                        for (NodeRules nodeRule : nodeRules) {

                            HashMap<String, String> deptMap = new HashMap<>();

                            if (!StringUtils.isBlank(node.getNodeCode())) {
                                deptMap.put("parent", node.getNodeCode());
                            } else {
                                deptMap.put("deptTreeType", node.getDeptTreeType());
                            }
                            roleBingUtil.addRoleBinding(upstreamTypeMap.get(nodeRule.getServiceKey()).getServiceCode(), domainInfo.getDomainName(), "dept", deptMap, deptPermissions);
                        }
                    }
                } else if ("post".equals(node.getType())) {
                    //岗位
                    List<NodeRules> nodeRules = collect.get(node.getId());
                    if (!CollectionUtils.isEmpty(nodeRules)) {
                        for (NodeRules nodeRule : nodeRules) {
                            HashMap<String, String> postMap = new HashMap<>();
                            if (!StringUtils.isBlank(node.getNodeCode())) {
                                postMap.put("parent", node.getNodeCode());
                            } else {
                                postMap.put("postTreeType", "*");
                            }
                            roleBingUtil.addRoleBinding(upstreamTypeMap.get(nodeRule.getServiceKey()).getServiceCode(), domainInfo.getDomainName(), "post", postMap, postPermissions);
                        }
                    }
                } else if ("person".equals(node.getType())) {
                    //人员
                    List<NodeRules> nodeRules = collect.get(node.getId());
                    if (!CollectionUtils.isEmpty(nodeRules)) {
                        for (NodeRules nodeRule : nodeRules) {
                            roleBingUtil.addRoleBinding(upstreamTypeMap.get(nodeRule.getServiceKey()).getServiceCode(), domainInfo.getDomainName(), "person", null, personPermissions);
                        }
                    }
                } else if ("occupy".equals(node.getType())) {
                    //人员身份
                    List<NodeRules> nodeRules = collect.get(node.getId());
                    if (!CollectionUtils.isEmpty(nodeRules)) {
                        for (NodeRules nodeRule : nodeRules) {
                            roleBingUtil.addRoleBinding(upstreamTypeMap.get(nodeRule.getServiceKey()).getServiceCode(), domainInfo.getDomainName(), "occupy", null, occupyPermissions);
                        }
                    }
                }
            }


        }


        //处理规则
        //HashMap<String, Object> map = dealNodeByUpstreamType(upstreamTypesRes, domain, deptTreeType);
        upstreamDao.saveUpstreamAndTypesAndNode(upstream, upstreamTypes, nodes, nodeRulesList, domainInfo);

    }

    @Transactional
    public HashMap<String, Object> dealNodeByUpstreamType(ArrayList<UpstreamType> list, String domain, String deptTreeType) throws Exception {
        long now = System.currentTimeMillis();
        List<Node> nodes = new ArrayList<>();
        List<NodeRules> nodeRulesList = new ArrayList<>();
        for (UpstreamType upstreamType : list) {
            Node node = new Node();
            String nodeId = UUID.randomUUID().toString();
            node.setId(nodeId);
            node.setCreateTime(now);
            node.setDomain(domain);
            node.setManual(node.getManual());
            node.setNodeCode("");
            node.setStatus(0);
            node.setType(upstreamType.getSynType());
            if ("dept".equals(upstreamType.getSynType())) {
                node.setDeptTreeType(deptTreeType);
            }
            nodes.add(node);
            NodeRules nodeRules = new NodeRules();
            nodeRules.setId(UUID.randomUUID().toString());
            nodeRules.setNodeId(nodeId);
            nodeRules.setType(0);
            nodeRules.setActive(true);
            nodeRules.setActiveTime(now);
            nodeRules.setServiceKey(upstreamType.getId());
            nodeRules.setStatus(0);
            nodeRulesList.add(nodeRules);
        }
        HashMap<String, Object> map = new HashMap<>();
        map.put("node", nodes);
        map.put("rules", nodeRulesList);
        return map;
    }


}
