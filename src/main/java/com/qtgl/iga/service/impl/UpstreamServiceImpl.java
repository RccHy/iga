package com.qtgl.iga.service.impl;


import com.qtgl.iga.AutoUpRunner;
import com.qtgl.iga.bean.UpstreamDto;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.dao.UpstreamDao;
import com.qtgl.iga.service.DomainIgnoreService;
import com.qtgl.iga.service.NodeRulesService;
import com.qtgl.iga.service.UpstreamService;
import com.qtgl.iga.service.UpstreamTypeService;
import com.qtgl.iga.utils.RoleBingUtil;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service

public class UpstreamServiceImpl implements UpstreamService {

    @Resource
    UpstreamDao upstreamDao;
    @Resource
    UpstreamTypeService upstreamTypeService;
    @Resource
    NodeRulesService nodeRulesService;
    @Resource
    RoleBingUtil roleBingUtil;
    @Resource
    DomainIgnoreService ignoreService;

    @Override
    public List<UpstreamDto> findAll(Map<String, Object> arguments, String domain) {
        List<Upstream> upstreamList = upstreamDao.findAll(arguments, domain);
        if (!CollectionUtils.isEmpty(upstreamList)) {

            //权威源去重
            List<UpstreamDto> upstreams = distinctSuperUpstream(upstreamList, domain);

            return upstreams;
        }
        return new ArrayList<>();
    }

    private ArrayList<UpstreamDto> distinctSuperUpstream(List<Upstream> upstreamList, String domain) {

        //获取当前租户禁用的权威源信息
        List<DomainIgnore> byDomain = ignoreService.findByDomain(domain);
        List<String> ignoreUpstreamIds = new ArrayList<>();
        if (!CollectionUtils.isEmpty(byDomain)) {
            ignoreUpstreamIds = byDomain.stream().map(DomainIgnore::getUpstreamId).filter(StringUtils::isNotBlank).collect(Collectors.toList());
        }

        HashMap<String, UpstreamDto> map = new HashMap<>();
        for (Upstream upstream : upstreamList) {
            UpstreamDto upstreamDto = new UpstreamDto(upstream);
            //逻辑字段值判断赋值
            if (!upstream.getDomain().equals(AutoUpRunner.superDomainId)) {
                upstreamDto.setLocal(true);
            } else {
                if (!CollectionUtils.isEmpty(ignoreUpstreamIds) && ignoreUpstreamIds.contains(upstreamDto.getId())) {
                    upstreamDto.setDisEnable(true);
                }
                upstreamDto.setLocal(false);
            }
            //覆盖判断
            if (map.containsKey(upstreamDto.getAppCode())) {
                if (!upstream.getDomain().equals(AutoUpRunner.superDomainId)) {
                    map.put( upstreamDto.getAppCode(), upstreamDto);
                }
            } else {
                map.put(upstreamDto.getAppCode(), upstreamDto);
            }
        }

        return new ArrayList<>(map.values());
    }

    @Override
    public Upstream findByCodeAndDomain(String code, String domain) {
        return upstreamDao.findByCodeAndDomain(code, domain);
    }

    @Override
    @Transactional
    public Upstream deleteUpstream(Map<String, Object> arguments, String domain) throws Exception {
        String id = (String) arguments.get("id");
        //查看是否来自超级租户
        Upstream byId = upstreamDao.findById(id);
        if (null != byId) {
            if (null != AutoUpRunner.superDomainId && AutoUpRunner.superDomainId.equals(byId.getDomain())) {
                //超级租户的权威源则处理为忽略
                DomainIgnore domainIgnore = new DomainIgnore();
                domainIgnore.setDomain(domain);
                domainIgnore.setUpstreamId(id);
                ignoreService.save(domainIgnore);
                return new Upstream();
            } else {
                //查看是否有关联node_rules
                List<UpstreamType> byUpstreamId = upstreamTypeService.findByUpstreamId(id);
                if (null != byUpstreamId && byUpstreamId.size() > 0) {
                    for (UpstreamType upstreamType : byUpstreamId) {
                        List<NodeRules> nodeRules = nodeRulesService.findNodeRulesByUpStreamTypeId(upstreamType.getId(), null);
                        List<NodeRules> oldNodeRules = nodeRulesService.findNodeRulesByUpStreamTypeId(upstreamType.getId(), 2);
                        //编辑和正式的规则提示
                        if (null != nodeRules && nodeRules.size() > 0) {
                            throw new CustomException(ResultCode.FAILED, "有绑定的nodeRules规则,请查看后再删除");
                        }
                        //历史版本,提示
                        if (null != oldNodeRules && oldNodeRules.size() > 0) {
                            //删除历史版本
                            nodeRulesService.deleteBatchRules(oldNodeRules, domain);
                        }

                    }
                }


                //删除权威源数据类型
                if (null != byUpstreamId && byUpstreamId.size() > 0) {
                    Integer integer = upstreamTypeService.deleteByUpstreamId(id, domain);
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

        } else {
            throw new CustomException(ResultCode.FAILED, "当前标识无对应权威源");
        }
    }

    @Override
    @Transactional
    public Upstream saveUpstream(Upstream upstream, String domain) throws Exception {
        //判重
        Upstream byCodeAndDomain = upstreamDao.findByCodeAndDomain(upstream.getAppCode(), domain);
        if (null != byCodeAndDomain) {
            throw new CustomException(ResultCode.REPEAT_UPSTREAM_ERROR, null, null, upstream.getAppCode(), upstream.getAppName());
        }
        if (StringUtils.isNotBlank(AutoUpRunner.superDomainId)) {

            Upstream superUpstream = upstreamDao.findByCodeAndDomain(upstream.getAppCode(), AutoUpRunner.superDomainId);
            if (null != superUpstream) {
                //将超级租户的权威源置为禁用
                DomainIgnore domainIgnore = new DomainIgnore();
                domainIgnore.setDomain(domain);
                domainIgnore.setUpstreamId(superUpstream.getId());
                ignoreService.save(domainIgnore);
            }
        }
        return upstreamDao.saveUpstream(upstream, domain);
    }

    @Override
    @Transactional
    public Upstream updateUpstream(Upstream upstream) throws Exception {

        return upstreamDao.updateUpstream(upstream);
    }

    @Override
    public Integer delAboutNode(Upstream upstream, DomainInfo domainInfo) throws Exception {
        return upstreamDao.delAboutNode(upstream, domainInfo);
    }

    @Override
    @Transactional
    public UpstreamDto saveUpstreamAndTypes(UpstreamDto upstreamDto, String domain) throws Exception {
        // 添加权威源
        Upstream upstream = this.saveUpstream(upstreamDto, domain);
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
                List<UpstreamType> upstreamTypeList = upstreamTypeService.findByUpstreamIdAndDescription(upstreamType, domain);
                if (null != upstreamTypeList && upstreamTypeList.size() > 0) {
                    throw new CustomException(ResultCode.FAILED, "权威源类型描述重复");
                }
                UpstreamType upstreamTypeDb = upstreamTypeService.saveUpstreamType(upstreamType, domain);
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
     * @param domainId
     * @Description: 查询权威源及类型
     * @return: java.util.List<com.qtgl.iga.bean.UpstreamDto>
     */
    @Override
    public List<UpstreamDto> upstreamsAndTypes(Map<String, Object> arguments, String domainId) {
        ArrayList<UpstreamDto> upstreamDtos = new ArrayList<>();
        //查询权威源
        List<Upstream> upstreamList = upstreamDao.findAll(arguments, domainId);
        //查询权威源类型
        if (!CollectionUtils.isEmpty(upstreamList)) {

           List<UpstreamDto> upstreamDtoList = distinctSuperUpstream(upstreamList, domainId);
            for (UpstreamDto upstreamDto : upstreamDtoList) {
                List<UpstreamType> byUpstreamId = upstreamTypeService.findByUpstreamId(upstreamDto.getId());
                upstreamDto.setUpstreamTypes(byUpstreamId);
                upstreamDtos.add(upstreamDto);
            }
            return upstreamDtos;
        }


        return upstreamDtos;
    }

    @Override
    @Transactional
    public UpstreamDto updateUpstreamAndTypes(UpstreamDto upstreamDto) throws Exception {


        //修改权威源类型
        //1.判断node绑定状态
        if (null != upstreamDto && null != upstreamDto.getUpstreamTypes() && upstreamDto.getUpstreamTypes().size() > 0) {
            for (UpstreamType upstreamType : upstreamDto.getUpstreamTypes()) {
                //查看是否有关联node_rules
                List<NodeRules> nodeRules = nodeRulesService.findNodeRulesByUpStreamTypeId(upstreamType.getId(), null);
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
                List<UpstreamType> upstreamTypeList = upstreamTypeService.findByUpstreamIdAndDescription(upstreamType, upstreamDto.getDomain());
                if (null != upstreamTypeList && upstreamTypeList.size() > 0) {
                    throw new CustomException(ResultCode.FAILED, "权威源类型描述重复");
                }
                if (null != upstreamType.getId()) {
                    upstreamResult = upstreamTypeService.updateUpstreamType(upstreamType);
                } else {
                    upstreamResult = upstreamTypeService.saveUpstreamType(upstreamType, upstreamDto.getDomain());
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
    @Transactional

    public Integer saveUpstreamTypesAndFields(List<UpstreamType> upstreamTypes, List<UpstreamType> updateUpstreamTypes, List<UpstreamTypeField> upstreamTypeFields, DomainInfo domainInfo) {
        return upstreamDao.saveUpstreamTypesAndFields(upstreamTypes, updateUpstreamTypes, upstreamTypeFields, domainInfo);

    }

    @Override
    @Transactional
    public Integer saveUpstreamAboutNodes(List<Node> nodes, List<NodeRules> nodeRulesList, List<NodeRulesRange> nodeRulesRanges, DomainInfo domainInfo) {
        return upstreamDao.saveUpstreamAbountNodes(nodes, nodeRulesList, nodeRulesRanges, domainInfo);
    }

    @Override
    @Transactional
    public void saveRoleBing(List<UpstreamType> upstreamTypes, List<Node> nodes, List<NodeRules> nodeRulesList, DomainInfo domainInfo) {

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


    }

    @Override
    public List<UpstreamDto> findByRecover(String domain) {
        ArrayList<UpstreamDto> upstreamDtos = new ArrayList<>();

        //获取被覆盖的权威源
       List<Upstream> upstreams =  upstreamDao.findByRecover(domain);
       if(!CollectionUtils.isEmpty(upstreams)){
           List<String> ids = upstreams.stream().map(Upstream::getId).collect(Collectors.toList());
           //获取被覆盖权威源所涉及的权威源类型
           List<UpstreamType> byUpstreamIds = upstreamTypeService.findByUpstreamIds(ids, domain);
           if(!CollectionUtils.isEmpty(byUpstreamIds)){
               Map<String, List<UpstreamType>> typesMap = byUpstreamIds.stream().collect(Collectors.groupingBy(UpstreamType::getUpstreamId));
               for (Upstream upstream : upstreams) {
                   UpstreamDto upstreamDto = new UpstreamDto(upstream);
                   upstreamDto.setUpstreamTypes(typesMap.get(upstream.getId()));
                   upstreamDtos.add(upstreamDto);
               }
           }
       }
        return upstreamDtos;
    }


    @Override
    public List<UpstreamDto> findByDomainAndActiveIsFalse(String domainId) {
        ArrayList<Upstream> upstreams = upstreamDao.findByDomainAndActiveIsFalse(domainId);
        if (!CollectionUtils.isEmpty(upstreams)) {
            return distinctSuperUpstream(upstreams, domainId);
        }
        return new ArrayList<>();
    }

    @Override
    public List<UpstreamDto> getUpstreams(String upstreamId, String domainId) {
        ArrayList<Upstream> upstreams = upstreamDao.getUpstreams(upstreamId, domainId);
        if (!CollectionUtils.isEmpty(upstreams)) {
            return distinctSuperUpstream(upstreams, domainId);
        }
        return new ArrayList<>();
    }

    @Override
    public List<UpstreamDto> findByOtherUpstream(List<String> ids, String domain) {
        ArrayList<Upstream> otherDomain = upstreamDao.findByOtherUpstream(ids, domain);
        if (!CollectionUtils.isEmpty(otherDomain)) {
            return distinctSuperUpstream(otherDomain, domain);
        }
        return new ArrayList<>();
    }

    @Override
    public List<Upstream> findByUpstreamTypeIds(ArrayList<String> ids, String domainId) {
        return upstreamDao.findByUpstreamTypeIds(ids, domainId);
    }

    @Override
    public Upstream findById(String upstreamId) {
        return upstreamDao.findById(upstreamId);
    }

    @Transactional
    public HashMap<String, Object> dealNodeByUpstreamType(ArrayList<UpstreamType> list, String domain, String deptTreeType) {
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
