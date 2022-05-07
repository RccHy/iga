package com.qtgl.iga.service.impl;


import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.dao.*;
import com.qtgl.iga.dao.impl.DynamicAttrDaoImpl;
import com.qtgl.iga.dao.impl.DynamicValueDaoImpl;
import com.qtgl.iga.service.NodeService;
import com.qtgl.iga.service.PostService;
import com.qtgl.iga.utils.ClassCompareUtil;
import com.qtgl.iga.utils.DataBusUtil;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl implements PostService {


    public static Logger logger = LoggerFactory.getLogger(PostServiceImpl.class);


    @Autowired
    NodeRulesCalculationServiceImpl calculationService;
    @Autowired
    PostDao postDao;
    @Autowired
    TenantDao tenantDao;
    @Autowired
    NodeService nodeService;
    @Autowired
    UpstreamTypeDao upstreamTypeDao;
    @Autowired
    OccupyDao occupyDao;
    @Autowired
    MonitorRulesDao monitorRulesDao;
    @Autowired
    DynamicAttrDaoImpl dynamicAttrDao;
    @Autowired
    DynamicValueDaoImpl dynamicValueDao;

    @Value("${iga.hostname}")
    String hostname;

    final String TYPE = "post";


    @Override
    @Transactional
    public Map<TreeBean, String> buildPostUpdateResult(DomainInfo domain, TaskLog lastTaskLog) throws Exception {
        //获取默认数据
        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        if (null == tenant) {
            throw new CustomException(ResultCode.FAILED, "租户不存在");
        }
        //  查sso BUILTIN 的岗位
        //  置空 mainTree
        ArrayList<TreeBean> rootBeans = new ArrayList<>();
        TreeBean treeBean = new TreeBean();
        treeBean.setCode("");
        treeBean.setActive(1);
        treeBean.setDelMark(0);
        rootBeans.add(treeBean);
        List<TreeBean> ssoBeans = postDao.findRootData(tenant.getId());
        //if (null != ssoBeans && ssoBeans.size() > 0) {
        //    for (TreeBean rootBean : ssoBeans) {
        //        if (null == rootBean.getParentCode()) {
        //            rootBean.setParentCode("");
        //        }
        //    }
        //} else {
        //    logger.error("请在‘ 身份管理->岗位管理 ’中进行岗位数据初始化导入后再进行治理", tenant.getId());
        //    throw new CustomException(ResultCode.FAILED, "请在‘身份管理->岗位管理’中进行岗位数据初始化导入后再进行治理");
        //}
        //获取完整的非PULL根数据以及加入逻辑根节点
        rootBeans.addAll(ssoBeans);
        //轮训比对标记(是否有主键id)
        Map<TreeBean, String> result = new HashMap<>();
        //ArrayList<TreeBean> treeBeans = new ArrayList<>();

        List<TreeBean> mainTreeBeans = new ArrayList<>();
//        Map<String, TreeBean> rootBeansMap = rootBeans.stream().collect(Collectors.toMap(TreeBean::getCode, deptBean -> deptBean));
        final LocalDateTime now = LocalDateTime.now();

        //获取扩展字段列表
        List<String> dynamicCodes = new ArrayList<>();

        List<DynamicValue> dynamicValues = new ArrayList<>();

        List<DynamicAttr> dynamicAttrs = dynamicAttrDao.findAllByType(TYPE, tenant.getId());

        List<DynamicValue> valueUpdate = new ArrayList<>();

        //扩展字段新增容器
        ArrayList<DynamicValue> valueInsert = new ArrayList<>();

        //扩展字段id与code对应map
        Map<String, String> attrMap = new ConcurrentHashMap<>();
        if (!CollectionUtils.isEmpty(dynamicAttrs)) {
            attrMap = dynamicAttrs.stream().collect(Collectors.toMap(DynamicAttr::getCode, DynamicAttr::getId));
        }
        if (!CollectionUtils.isEmpty(dynamicAttrs)) {
            dynamicCodes = dynamicAttrs.stream().map(DynamicAttr -> DynamicAttr.getCode()).collect(Collectors.toList());
            //获取扩展value
            List<String> attrIds = dynamicAttrs.stream().map(DynamicAttr -> DynamicAttr.getId()).collect(Collectors.toList());

            dynamicValues = dynamicValueDao.findAllByAttrId(attrIds, tenant.getId());
        }
        //扩展字段值分组
        Map<String, List<DynamicValue>> valueMap = new ConcurrentHashMap<>();
        if (!CollectionUtils.isEmpty(dynamicValues)) {
            valueMap = dynamicValues.stream().collect(Collectors.groupingBy(dynamicValue -> dynamicValue.getEntityId()));
        }
        mainTreeBeans.addAll(ssoBeans);

        for (TreeBean rootBean : rootBeans) {

            mainTreeBeans = calculationService.nodeRules(domain, null, rootBean.getCode(), mainTreeBeans, 0, TYPE, dynamicCodes);

        }
        // 判断重复(code)
        calculationService.groupByCode(mainTreeBeans, 0, domain);

        //通过tenantId查询ssoApis库中的数据
        List<TreeBean> beans = postDao.findByTenantId(tenant.getId());

        //将null赋为""
        if (null != beans && beans.size() > 0) {
            for (TreeBean bean : beans) {
                if (StringUtils.isBlank(bean.getParentCode())) {
                    bean.setParentCode("");
                }
            }
        }
        Map<String, TreeBean> mainTreeMap = mainTreeBeans.stream().collect(Collectors.toMap(TreeBean::getCode, deptBean -> deptBean));

        //同步到sso
        beans = dataProcessing(mainTreeMap, domain, "", beans, result, now, dynamicAttrs, valueMap, valueUpdate, valueInsert);
//        if (null != beans) {
//            beans.addAll(treeBeans);
//        }
//        if (null != rootBeansMap && rootBeansMap.size() > 0) {
//            rootBeans = new ArrayList<>(rootBeansMap.values());
//        }

        // 判断重复(code)
        calculationService.groupByCode(beans, 0, domain);
        //

        Map<String, List<Map.Entry<TreeBean, String>>> collect = result.entrySet().stream().collect(Collectors.groupingBy(c -> c.getValue()));
        // 验证监控规则
        List<Map.Entry<TreeBean, String>> delete = collect.get("delete");
        calculationService.monitorRules(domain, lastTaskLog, beans.size(), delete, "post");
        saveToSso(collect, tenant.getId(), attrMap, valueUpdate, valueInsert);

        return result;

    }

    /**
     * @param arguments
     * @param domain
     * @Description: 根据状态(0 : 正式版本, 1 : 编辑版本) 获取规则拉取岗位数据(以遍历根结构数据的方式,逻辑根节点手动添加)  覆盖方式  BUILTIN源覆盖PULL源
     * @return: java.util.List<com.qtgl.iga.bean.TreeBean>
     */
    @Override
    public List<TreeBean> findPosts(Map<String, Object> arguments, DomainInfo domain) throws Exception {
        Integer status = (Integer) arguments.get("status");
        //是否需要复制规则
        if ((Boolean) arguments.get("scenes")) {
            status = nodeService.judgeEdit(arguments, domain, TYPE);
            if (status == null) {
                return null;
            }
        }
        //获取默认数据
        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        if (null == tenant) {
            logger.error("请检查根树是否合法{}", domain.getId());
            throw new CustomException(ResultCode.FAILED, "租户不存在");
        }
        //  置空 mainTree
        ArrayList<TreeBean> rootBeans = new ArrayList<>();
        TreeBean treeBean = new TreeBean();
        treeBean.setCode("");
        treeBean.setActive(1);
        treeBean.setDelMark(0);
        rootBeans.add(treeBean);
        List<TreeBean> ssoBeans = postDao.findRootData(tenant.getId());
        //if (null != ssoBeans && ssoBeans.size() > 0) {
        //    for (TreeBean rootBean : ssoBeans) {
        //        if (null == rootBean.getParentCode()) {
        //            rootBean.setParentCode("");
        //        }
        //    }
        //} else {
        //    logger.error("请在‘ 身份管理->岗位管理 ’中进行岗位数据初始化导入后再进行治理{}", tenant.getId());
        //    throw new CustomException(ResultCode.FAILED, "请在‘ 身份管理->岗位管理 ’中进行岗位数据初始化导入后再进行治理");
        //}
        //获取完整的非PULL根数据以及加入逻辑根节点
        rootBeans.addAll(ssoBeans);

        //轮训比对标记(是否有主键id)
        Map<TreeBean, String> result = new HashMap<>();
        //ArrayList<TreeBean> treeBeans = new ArrayList<>();

        List<TreeBean> mainTreeBeans = new ArrayList<>();
        final LocalDateTime now = LocalDateTime.now();
        //获取扩展字段列表
        List<String> dynamicCodes = new ArrayList<>();

        List<DynamicValue> dynamicValues = new ArrayList<>();

        List<DynamicAttr> dynamicAttrs = dynamicAttrDao.findAllByType(TYPE, tenant.getId());

        List<DynamicValue> valueUpdate = new ArrayList<>();

        //扩展字段新增容器
        ArrayList<DynamicValue> valueInsert = new ArrayList<>();

        ////扩展字段id与code对应map
        //Map<String, String> attrMap = new ConcurrentHashMap<>();
        //if (!CollectionUtils.isEmpty(dynamicAttrs)) {
        //    attrMap = dynamicAttrs.stream().collect(Collectors.toMap(DynamicAttr::getCode, DynamicAttr::getId));
        //}
        if (!CollectionUtils.isEmpty(dynamicAttrs)) {
            dynamicCodes = dynamicAttrs.stream().map(DynamicAttr -> DynamicAttr.getCode()).collect(Collectors.toList());
            //获取扩展value
            List<String> attrIds = dynamicAttrs.stream().map(DynamicAttr -> DynamicAttr.getId()).collect(Collectors.toList());

            dynamicValues = dynamicValueDao.findAllByAttrId(attrIds, tenant.getId());
        }
        //扩展字段值分组
        Map<String, List<DynamicValue>> valueMap = new ConcurrentHashMap<>();
        if (!CollectionUtils.isEmpty(dynamicValues)) {
            valueMap = dynamicValues.stream().collect(Collectors.groupingBy(dynamicValue -> dynamicValue.getEntityId()));
        }
        logger.info("获取到当前租户{}的映射字段集为{}", tenant.getId(), dynamicAttrs);

        Map<String, TreeBean> rootBeansMap = rootBeans.stream().collect(Collectors.toMap(TreeBean::getCode, deptBean -> deptBean));

        mainTreeBeans.addAll(ssoBeans);
        for (TreeBean rootBean : rootBeans) {

            mainTreeBeans = calculationService.nodeRules(domain, null, rootBean.getCode(), mainTreeBeans, status, TYPE, dynamicCodes);

        }

        //通过tenantId查询ssoApis库中的数据
        List<TreeBean> beans = postDao.findByTenantId(tenant.getId());
        //将null赋为""
        if (null != beans && beans.size() > 0) {
            for (TreeBean bean : beans) {
                if (StringUtils.isBlank(bean.getParentCode())) {
                    bean.setParentCode("");
                }
            }
        }
        Map<String, TreeBean> collect = beans.stream().collect(Collectors.toMap(TreeBean::getCode, deptBean -> deptBean));
        collect.putAll(rootBeansMap);
        beans = new ArrayList<>(collect.values());
        //数据对比处理
        Map<String, TreeBean> mainTreeMap = mainTreeBeans.stream().collect(Collectors.toMap(TreeBean::getCode, deptBean -> deptBean));
        beans = dataProcessing(mainTreeMap, domain, "", beans, result, now, dynamicAttrs, valueMap, valueUpdate, valueInsert);

//        if (null != beans) {
//            beans.addAll(treeBeans);
//        }
//        if (null != rootBeansMap && rootBeansMap.size() > 0) {
//            rootBeans = new ArrayList<>(rootBeansMap.values());
//        }

        // 判断重复(code)
        calculationService.groupByCode(beans, status, domain);

        return beans;

    }

    /**
     * @param collect
     * @param tenantId
     * @Description: 增量插入sso数据库
     * @return: void
     */
    private void saveToSso(Map<String, List<Map.Entry<TreeBean, String>>> collect, String tenantId, Map<String, String> attrMap, List<DynamicValue> valueUpdate, List<DynamicValue> valueInsert) {


        ArrayList<TreeBean> insertList = new ArrayList<>();
        ArrayList<TreeBean> updateList = new ArrayList<>();
        ArrayList<TreeBean> deleteList = new ArrayList<>();
        ArrayList<TreeBean> invalidList = new ArrayList<>();

        List<Map.Entry<TreeBean, String>> delete = collect.get("delete");
        //删除数据
        if (null != delete && delete.size() > 0) {
            for (Map.Entry<TreeBean, String> key : delete) {
                logger.info("岗位对比后删除{}", key.getKey().toString());
                deleteList.add(key.getKey());
            }
        }
        List<Map.Entry<TreeBean, String>> insert = collect.get("insert");

        if (null != insert && insert.size() > 0) {
            for (Map.Entry<TreeBean, String> key : insert) {
                TreeBean bean = key.getKey();
                String id = UUID.randomUUID().toString();
                bean.setId(id);
                Map<String, String> dynamic = bean.getDynamic();
                if (!CollectionUtils.isEmpty(dynamic)) {
                    for (Map.Entry<String, String> str : dynamic.entrySet()) {
                        DynamicValue dynamicValue = new DynamicValue();
                        dynamicValue.setId(UUID.randomUUID().toString());
                        dynamicValue.setValue(str.getValue());
                        dynamicValue.setEntityId(id);
                        dynamicValue.setTenantId(tenantId);
                        dynamicValue.setAttrId(attrMap.get(str.getKey()));
                        valueInsert.add(dynamicValue);
                    }
                }
                logger.debug("岗位对比后新增{}", key.getKey().toString());
                insertList.add(key.getKey());
            }
        }
        List<Map.Entry<TreeBean, String>> obsolete = collect.get("obsolete");
        if (null != obsolete && obsolete.size() > 0) {
            ArrayList<TreeBean> list = new ArrayList<>();
            for (Map.Entry<TreeBean, String> key : obsolete) {
                list.add(key.getKey());
            }
            logger.info("忽略 {} 条已过时数据{}", list.size(), obsolete.toString());

        }
        List<Map.Entry<TreeBean, String>> update = collect.get("update");
        //修改数据
        if (null != update && update.size() > 0) {
            for (Map.Entry<TreeBean, String> key : update) {

                logger.info("岗位对比后需要修改{}", key.getKey().toString());
                updateList.add(key.getKey());
            }
        }
        List<Map.Entry<TreeBean, String>> invalid = collect.get("invalid");
        //失效数据
        if (null != invalid && invalid.size() > 0) {
            for (Map.Entry<TreeBean, String> key : invalid) {
                logger.info("岗位对比后失效{}", key.getKey().toString());
                invalidList.add(key.getKey());
            }
        }
        List<Map.Entry<TreeBean, String>> recover = collect.get("recover");
        //失效数据
        if (null != recover && recover.size() > 0) {
            for (Map.Entry<TreeBean, String> key : recover) {
                logger.info("岗位对比后恢复新增{}", key.getKey().toString());
                updateList.add(key.getKey());
            }
        }
        logger.info("组织机构处理结束:扩展字段处理需要修改{},需要新增{}", CollectionUtils.isEmpty(valueUpdate) ? 0 : valueUpdate.size(), CollectionUtils.isEmpty(valueInsert) ? 0 : valueInsert.size());
        logger.debug("组织机构处理结束:扩展字段处理需要修改{},需要新增{}", valueUpdate, valueInsert);
        Integer flag = postDao.renewData(insertList, updateList, deleteList, invalidList, valueUpdate, valueInsert, tenantId);
        if (null != flag && flag > 0) {

            logger.info("post插入 {} 条数据  {}", insertList.size(), System.currentTimeMillis());

            logger.info("post更新 {} 条数据  {}", updateList.size(), System.currentTimeMillis());

            logger.info("post删除 {} 条数据  {}", deleteList.size(), System.currentTimeMillis());

            logger.info("post失效 {} 条数据  {}", invalidList.size(), System.currentTimeMillis());
        } else {

            logger.error("数据更新sso数据库失败{}", System.currentTimeMillis());
        }

    }

//    /**
//     * @param domainName
//     * @Description: 通过租户获取sso库的数据
//     * @return: java.util.List<com.qtgl.iga.bean.DeptBean>
//     */
//    @Override
//    public List<TreeBean> findDeptByDomainName(String domainName) throws Exception {
////        获取tenantId
//        Tenant byDomainName = tenantDao.findByDomainName(domainName);
//        if (null != byDomainName) {
//            return postDao.findPostType(byDomainName.getId());
//        } else {
//            throw new CustomException(ResultCode.FAILED, "租户不存在");
//        }
//
//
//    }


    /**
     * 1：根据规则拉取所有权威源岗位数据
     * 2：获取sso所有的岗位数据(包括已删除的)
     * <p>
     * 3：根据上游部门和数据库中岗位进行对比
     * A：新增  上游提供、sso数据库中没有            对于active和del_mark字段 上游提供使用上游,不提供则默认有效未删除
     * B：修改  上游和sso对比后字段值有差异
     * C：删除  上游提供了del_mark             (上游必须del_mark字段)
     * D: 无效  上游曾经提供后，不再提供 OR 上游提供了active
     * E: 删除恢复  之前被标记为删除后再通过推送了相同的数据   (上游必须del_mark字段)
     * E: 失效恢复  之前被标记为失效后再通过推送了相同的数据   提供active则修改对比时直接覆盖,不提供则手动恢复
     **/
    private List<TreeBean> dataProcessing(Map<String, TreeBean> mainTree, DomainInfo domainInfo, String treeTypeId, List<TreeBean> ssoBeans, Map<TreeBean, String> result, LocalDateTime now, List<DynamicAttr> dynamicAttrs, Map<String, List<DynamicValue>> valueMap, List<DynamicValue> valueUpdate, List<DynamicValue> valueInsert) {
        //将sso的数据转化为map方便对比
        Map<String, TreeBean> ssoCollect = new HashMap<>();
        if (null != ssoBeans && ssoBeans.size() > 0) {
            ssoCollect = ssoBeans.stream().collect(Collectors.toMap((TreeBean::getCode), (dept -> dept)));
        }
        //拉取的数据
        Collection<TreeBean> mainDept = mainTree.values();
        ArrayList<TreeBean> pullBeans = new ArrayList<>(mainDept);
        // 处理扩展字段
        //扩展字段id与code对应map
        Map<String, String> attrMap = new ConcurrentHashMap<>();
        if (!CollectionUtils.isEmpty(dynamicAttrs)) {
            attrMap = dynamicAttrs.stream().collect(Collectors.toMap(DynamicAttr::getId, DynamicAttr::getCode));
        }
        //遍历拉取数据
        for (TreeBean pullBean : pullBeans) {
            //标记新增还是修改
            boolean flag = true;
            //赋值treeTypeId
            pullBean.setTreeType(treeTypeId);
            if (null != ssoBeans) {
                //遍历数据库数据
                for (TreeBean ssoBean : ssoBeans) {
                    if (pullBean.getCode().equals(ssoBean.getCode())) {

                        if (null != pullBean.getCreateTime()) {
                            //修改
                            if (null == ssoBean.getCreateTime() || pullBean.getCreateTime().isAfter(ssoBean.getCreateTime())) {
                                //修改标识 1.source为非BUILTIN 则进行覆盖  2.source都为PULL则对比字段 有修改再标识为true
                                boolean updateFlag = false;
                                //删除恢复标识
                                boolean delRecoverFlag = false;
                                //del字段标识
                                boolean delFlag = false;
                                //失效标识
                                boolean invalidFlag = false;
//                                //是否手动恢复有效标识 (如果上游提供则置为false)
//                                boolean invalidRecoverFlag = true;
                                //上游是否提供active字段
                                boolean activeFlag = false;
                                //是否处理扩展字段标识
                                boolean dyFlag = true;
                                //处理sso数据的active为null的情况
                                if(null==ssoBean.getActive()||"".equals(ssoBean.getActive())){
                                    ssoBean.setActive(1);
                                }
                                if (!"BUILTIN".equalsIgnoreCase(pullBean.getDataSource())) {
                                    ssoBean.setDataSource("PULL");
                                    ssoBean.setSource(pullBean.getSource());
                                    ssoBean.setUpdateTime(now);
                                    ssoBean.setFormal(pullBean.getFormal());
                                }
                                ssoBean.setColor(pullBean.getColor());
                                ssoBean.setIsRuled(pullBean.getIsRuled());


                                List<UpstreamTypeField> fields = null;
                                if (null != pullBean.getUpstreamTypeId()) {
                                    fields = DataBusUtil.typeFields.get(pullBean.getUpstreamTypeId());
                                }
                                //获取对应权威源的映射字段
                                if (null != fields && fields.size() > 0) {
                                    for (UpstreamTypeField field : fields) {
                                        String sourceField = field.getSourceField();

                                        Object newValue = ClassCompareUtil.getGetMethod(pullBean, sourceField);
                                        Object oldValue = ClassCompareUtil.getGetMethod(ssoBean, sourceField);
                                        //均为空 跳过
                                        if (null == oldValue && null == newValue) {
                                            continue;
                                        }
                                        //不为空且相同 跳过
                                        if (null != oldValue && oldValue.equals(newValue)) {
                                            continue;
                                        }
                                        //将修改表示改为true 标识数据需要修改
                                        updateFlag = true;
                                        //如果上游给出删除标记 则使用上游的   不给则不处理
                                        if ("delMark".equalsIgnoreCase(sourceField) && null != ssoBean.getDelMark() && null != pullBean.getDelMark() && (ssoBean.getDelMark() == 1) && (pullBean.getDelMark() == 0)) {
                                            //恢复标识
                                            delRecoverFlag = true;
                                            continue;
                                        }
                                        if ("delMark".equalsIgnoreCase(sourceField) && null != ssoBean.getDelMark() && null != pullBean.getDelMark() && (ssoBean.getDelMark() == 0) && (pullBean.getDelMark() == 1)) {
                                            //删除标识
                                            delFlag = true;
                                            continue;
                                        }
                                        if (sourceField.equalsIgnoreCase("active") && (Integer) oldValue == 1 && (Integer) newValue == 0) {
                                            invalidFlag = true;
                                        }
//                                        if (sourceField.equalsIgnoreCase("active") && (Integer) oldValue == 0 && (Integer) newValue == 1) {
//                                            invalidRecoverFlag = false;
//                                        }
                                        if (sourceField.equalsIgnoreCase("active")) {
                                            activeFlag = true;
                                        }

                                        //将值更新到sso对象
                                        ClassCompareUtil.setValue(ssoBean, ssoBean.getClass(), sourceField, oldValue, newValue);
                                        logger.debug("岗位信息更新{}:字段{}: {} -> {} ", pullBean.getCode(), sourceField, oldValue, newValue);
                                    }
                                }

                                //标识为恢复数据
                                if (delRecoverFlag) {
                                    ssoBean.setDelMark(0);
                                    result.put(ssoBean, "recover");
                                    //修改标记置为false
                                    updateFlag = false;
                                    logger.info("岗位信息{}从删除恢复", ssoBean.getCode());
                                }
                                //标识为删除的数据
                                if (delFlag) {
                                    //将数据放入删除集合
                                    ssoBean.setDelMark(1);
                                    ssoCollect.remove(ssoBean.getCode());
                                    result.put(ssoBean, "delete");
                                    //修改标记置为false
                                    updateFlag = false;
                                    logger.info("岗位对比后需要删除{}", ssoBean);
                                }
//                                //恢复失效数据  未提供active手动处理
//                                if (invalidRecoverFlag && ssoBean.getActive() != 1) {
//                                    //修改标记置为false
//                                    updateFlag = false;
//                                    ssoBean.setActive(1);
//                                    result.put(ssoBean, "update");
//                                    logger.info("岗位信息{}从失效恢复", ssoBean.getCode());
//                                }

                                //修改不为删除的数据
                                if (updateFlag && ssoBean.getDelMark() != 1) {

                                    ssoBean.setUpdateTime(now);
                                    //失效
                                    if (invalidFlag) {
                                        result.put(ssoBean, "invalid");
                                    } else {
                                        //将数据放入修改集合
                                        ssoCollect.put(ssoBean.getCode(), ssoBean);

                                        if (dyFlag) {
                                            //上游的扩展字段
                                            Map<String, String> dynamic = pullBean.getDynamic();
                                            List<DynamicValue> dyValuesFromSSO = null;
                                            //数据库的扩展字段
                                            if (!CollectionUtils.isEmpty(valueMap)) {
                                                dyValuesFromSSO = valueMap.get(ssoBean.getId());
                                            }
                                            dynamicProcessing(valueUpdate, valueInsert, attrMap, ssoBean, dynamic, dyValuesFromSSO);
                                            dyFlag = false;
                                        }
                                    }
                                    logger.info("岗位对比后需要修改{}", ssoBean);

                                }
                                //上游未提供active并且sso与上游源该字段值不一致
                                if (!activeFlag && (!ssoBean.getActive().equals(pullBean.getActive()))) {
                                    ssoBean.setUpdateTime(now);
                                    ssoBean.setActive(pullBean.getActive());
                                    //将数据放入修改集合
                                    ssoCollect.put(ssoBean.getCode(), ssoBean);
                                    logger.info("手动从失效中恢复{}", ssoBean);
                                    if (dyFlag) {
                                        //上游的扩展字段
                                        Map<String, String> dynamic = pullBean.getDynamic();
                                        List<DynamicValue> dyValuesFromSSO = null;
                                        //数据库的扩展字段
                                        if (!CollectionUtils.isEmpty(valueMap)) {
                                            dyValuesFromSSO = valueMap.get(ssoBean.getId());
                                        }
                                        dynamicProcessing(valueUpdate, valueInsert, attrMap, ssoBean, dynamic, dyValuesFromSSO);
                                        dyFlag = false;
                                    }
                                }

                                //上游未提供delmark并且sso与上游源该字段值不一致
                                if (!delFlag && !delRecoverFlag && (!ssoBean.getDelMark().equals(pullBean.getDelMark()))) {
                                    ssoBean.setUpdateTime(now);
                                    ssoBean.setDelMark(pullBean.getDelMark());
                                    //将数据放入修改集合
                                    ssoCollect.put(ssoBean.getCode(), ssoBean);
                                    logger.info("手动从删除中恢复{}", ssoBean);
                                    if (dyFlag) {
                                        //上游的扩展字段
                                        Map<String, String> dynamic = pullBean.getDynamic();
                                        List<DynamicValue> dyValuesFromSSO = null;
                                        //数据库的扩展字段
                                        if (!CollectionUtils.isEmpty(valueMap)) {
                                            dyValuesFromSSO = valueMap.get(ssoBean.getId());
                                        }
                                        dynamicProcessing(valueUpdate, valueInsert, attrMap, ssoBean, dynamic, dyValuesFromSSO);
                                        dyFlag = false;
                                    }
                                }

                                //防止重复将数据放入
                                if (!dyFlag) {
                                    result.put(ssoBean, "update");
                                }

                                //处理扩展字段对比     修改标识为false则认为主体字段没有差异
                                if (!updateFlag && dyFlag) {
                                    //上游的扩展字段
                                    Map<String, String> dynamic = pullBean.getDynamic();
                                    List<DynamicValue> dyValuesFromSSO = null;
                                    //数据库的扩展字段
                                    if (!CollectionUtils.isEmpty(valueMap)) {
                                        dyValuesFromSSO = valueMap.get(ssoBean.getId());
                                    }
                                    Boolean valueFlag = dynamicProcessing(valueUpdate, valueInsert, attrMap, ssoBean, dynamic, dyValuesFromSSO);
                                    if (valueFlag) {
                                        result.put(ssoBean, "update");
                                    }

                                }
                            } else {
                                //如果数据不是最新的则忽略
                                result.put(pullBean, "obsolete");
                            }
                        } else {
                            //上游创建时间为null,则默认忽略
                            result.put(pullBean, "obsolete");
                        }
                        flag = false;
                    }

                }
                //没有相等的应该是新增(对比code没有对应的标识为新增)
                if (flag) {
                    //新增
                    ssoCollect.put(pullBean.getCode(), pullBean);
                    result.put(pullBean, "insert");
                }
            } else {
                //数据库数据为空的话,则默认全部新增
                ssoCollect.put(pullBean.getCode(), pullBean);
                result.put(pullBean, "insert");
            }

        }
        if (null != ssoBeans) {
            //查询数据库需要删除的数据
            for (TreeBean ssoBean : ssoBeans) {
                if ("PULL".equalsIgnoreCase(ssoBean.getDataSource())) {
                    //仅能操作拉取的数据
                    boolean flag = true;
                    for (TreeBean pullBean : pullBeans) {
                        //如果sso与拉取的都有则说明为修改或者忽略,关闭删除标记
                        if (ssoBean.getCode().equals(pullBean.getCode())) {
                            flag = false;
                            break;
                        }
                    }
                    if (flag) {
                        //移除集合中失效对象
                        ssoCollect.remove(ssoBean.getCode());
                        //排除逻辑根节点
                        if (!("".equals(ssoBean.getCode()))) {
                            //如果为有效的再走失效并更新修改时间,
                            //本身就是无效的则不做处理
                            if (1 == ssoBean.getActive()) {
                                ssoBean.setActive(0);
                                ssoBean.setUpdateTime(now);
                                result.put(ssoBean, "invalid");

                            }
                        }
                    }
                }
            }
        }


        Collection<TreeBean> values = ssoCollect.values();
        List<TreeBean> collect = new ArrayList<>(values).stream().filter(post -> post.getActive() != 0 && post.getDelMark() != 1).collect(Collectors.toList());
        return collect;
    }

    private Boolean dynamicProcessing(List<DynamicValue> valueUpdate, List<DynamicValue> valueInsert, Map<String, String> attrMap, TreeBean ssoBean, Map<String, String> dynamic, List<DynamicValue> dyValuesFromSSO) {
        Boolean valueFlag = false;
        if (!CollectionUtils.isEmpty(dyValuesFromSSO)) {
            Map<String, DynamicValue> collect = dyValuesFromSSO.stream().collect(Collectors.toMap(DynamicValue::getAttrId, dynamicValue -> dynamicValue));
            for (Map.Entry<String, String> str : attrMap.entrySet()) {
                String o = dynamic.get(str.getValue());
                if (collect.containsKey(str.getKey())) {
                    DynamicValue dynamicValue = collect.get(str.getKey());
                    if (null != o && !o.equals(dynamicValue.getValue())) {
                        dynamicValue.setValue(o);
                        valueUpdate.add(dynamicValue);
                        valueFlag = true;
                    }
                } else {
                    //上游有  数据库没有则新增
                    DynamicValue dynamicValue = new DynamicValue();
                    dynamicValue.setId(UUID.randomUUID().toString());
                    dynamicValue.setValue(o);
                    dynamicValue.setEntityId(ssoBean.getId());
                    dynamicValue.setAttrId(str.getKey());
                    valueFlag = true;
                    valueInsert.add(dynamicValue);
                }
            }
        } else {
            for (Map.Entry<String, String> str : attrMap.entrySet()) {
                String o = dynamic.get(str.getValue());
                valueFlag = true;
                //上游有  数据库没有则新增
                DynamicValue dynamicValue = new DynamicValue();
                dynamicValue.setId(UUID.randomUUID().toString());
                dynamicValue.setValue(o);
                dynamicValue.setEntityId(ssoBean.getId());
                dynamicValue.setAttrId(str.getKey());
                valueInsert.add(dynamicValue);

            }
        }
        return valueFlag;
    }


}
