package com.qtgl.iga.service.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.dao.*;
import com.qtgl.iga.service.NodeService;
import com.qtgl.iga.service.PostService;
import com.qtgl.iga.task.TaskConfig;
import com.qtgl.iga.utils.ClassCompareUtil;
import com.qtgl.iga.utils.DataBusUtil;
import com.qtgl.iga.utils.TreeUtil;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
//@Transactional
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


    @Value("${iga.hostname}")
    String hostname;

    final String TYPE = "post";


    @Override
    @Transactional
    public Map<TreeBean, String> buildPostUpdateResult(DomainInfo domain, TaskLog lastTaskLog) throws Exception {
        //获取默认数据
        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        if (null == tenant) {
            throw new Exception("租户不存在");
        }
        //  查sso BUILTIN 的岗位
        //  置空 mainTree
        ArrayList<TreeBean> rootBeans = new ArrayList<>();
        TreeBean treeBean = new TreeBean();
        treeBean.setCode("");
        rootBeans.add(treeBean);
        List<TreeBean> ssoBeans = postDao.findRootData(tenant.getId());
        if (null != ssoBeans && ssoBeans.size() > 0) {
            for (TreeBean rootBean : ssoBeans) {
                if (null == rootBean.getParentCode()) {
                    rootBean.setParentCode("");
                }
            }
        } else {
            logger.error("请检查根树是否合法{}", tenant.getId());
            throw new Exception("请检查根树是否合法");
        }
        rootBeans.addAll(ssoBeans);
        //轮训比对标记(是否有主键id)
        Map<TreeBean, String> result = new HashMap<>();
        ArrayList<TreeBean> treeBeans = new ArrayList<>();

        List<TreeBean> mainTreeBeans = new ArrayList<>();
        Map<String, TreeBean> rootBeansMap = rootBeans.stream().collect(Collectors.toMap(TreeBean::getCode, deptBean -> deptBean));
        final LocalDateTime now = LocalDateTime.now();
        mainTreeBeans.addAll(ssoBeans);

        for (TreeBean rootBean : rootBeans) {


            mainTreeBeans = calculationService.nodeRules(domain, null, rootBean.getCode(), mainTreeBeans, 0, TYPE, "task", rootBeans, rootBeansMap, ssoBeans);
            // 判断重复(code)
            calculationService.groupByCode(mainTreeBeans, 0, domain);
        }
//
        // 判断重复(code)
        calculationService.groupByCode(mainTreeBeans, 0, domain);

        //通过tenantId查询ssoApis库中的数据
        List<TreeBean> beans = postDao.findByTenantId(tenant.getId());
        ArrayList<TreeBean> logBeans = new ArrayList<>();

        //将null赋为""
        if (null != beans && beans.size() > 0) {
            for (TreeBean bean : beans) {
                if (StringUtils.isBlank(bean.getParentCode())) {
                    bean.setParentCode("");
                }
            }
            logBeans.addAll(beans);
        }
        Map<String, TreeBean> mainTreeMap = mainTreeBeans.stream().collect(Collectors.toMap(TreeBean::getCode, deptBean -> deptBean));

        //同步到sso
        beans = dataProcessing(mainTreeMap, domain, "", beans, result, treeBeans, now);
        if (null != beans) {
            beans.addAll(treeBeans);
        }
        if (null != rootBeansMap && rootBeansMap.size() > 0) {
            rootBeans = new ArrayList<>(rootBeansMap.values());
        }
        // 判断重复(code)
        calculationService.groupByCode(beans, 0, domain);
        //

        Map<String, List<Map.Entry<TreeBean, String>>> collect = result.entrySet().stream().collect(Collectors.groupingBy(c -> c.getValue()));
        // 验证监控规则
        List<Map.Entry<TreeBean, String>> delete = collect.get("delete");
        calculationService.monitorRules(domain, lastTaskLog, beans.size(), delete, "post");
        saveToSso(collect, tenant.getId(), null);

        return result;

    }


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
            throw new Exception("租户不存在");
        }
        //  置空 mainTree
        ArrayList<TreeBean> rootBeans = new ArrayList<>();
        TreeBean treeBean = new TreeBean();
        treeBean.setCode("");
        rootBeans.add(treeBean);
        List<TreeBean> ssoBeans = postDao.findRootData(tenant.getId());
        if (null != ssoBeans && ssoBeans.size() > 0) {
            for (TreeBean rootBean : ssoBeans) {
                if (null == rootBean.getParentCode()) {
                    rootBean.setParentCode("");
                }
            }
        } else {
            logger.error("请检查根树是否合法{}", tenant.getId());
            throw new Exception("请检查根树是否合法");
        }
        rootBeans.addAll(ssoBeans);

        //sso dept库的数据(通过domain 关联tenant查询)
        if (null == tenant) {
            throw new Exception("租户不存在");
        }

        //轮训比对标记(是否有主键id)
        Map<TreeBean, String> result = new HashMap<>();
        ArrayList<TreeBean> treeBeans = new ArrayList<>();

        List<TreeBean> mainTreeBeans = new ArrayList<>();
        final LocalDateTime now = LocalDateTime.now();
        Map<String, TreeBean> rootBeansMap = rootBeans.stream().collect(Collectors.toMap(TreeBean::getCode, deptBean -> deptBean));

        mainTreeBeans.addAll(ssoBeans);
        for (TreeBean rootBean : rootBeans) {


            mainTreeBeans = calculationService.nodeRules(domain, null, rootBean.getCode(), mainTreeBeans, status, TYPE, "system", rootBeans, rootBeansMap, ssoBeans);

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
        beans = dataProcessing(mainTreeMap, domain, "", beans, result, treeBeans, now);

        if (null != beans) {
            beans.addAll(treeBeans);
        }
        if (null != rootBeansMap && rootBeansMap.size() > 0) {
            rootBeans = new ArrayList<>(rootBeansMap.values());
        }
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
    private void saveToSso(Map<String, List<Map.Entry<TreeBean, String>>> collect, String tenantId, List<TreeBean> logBeans) throws Exception {

//        Map<String, TreeBean> logCollect = null;

        ArrayList<TreeBean> insertList = new ArrayList<>();
        ArrayList<TreeBean> updateList = new ArrayList<>();
        ArrayList<TreeBean> deleteList = new ArrayList<>();

//        if (null != logBeans && logBeans.size() > 0) {
//            logCollect = logBeans.stream().collect(Collectors.toMap((TreeBean::getCode), (dept -> dept)));
//        }

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
//                key.getKey().setDataSource("PULL");
//                //统计修改字段
//                TreeBean newTreeBean = key.getKey();
//                TreeBean oldTreeBean = logCollect.get(newTreeBean.getCode());
//                oldTreeBean.setSource(newTreeBean.getSource());
//                oldTreeBean.setUpdateTime(newTreeBean.getUpdateTime());
//                oldTreeBean.setActive(newTreeBean.getActive());
//                //根据upstreamTypeId查询fileds
//                boolean flag = false;
//                boolean delFlag = true;
//
//
//                List<UpstreamTypeField> fields = DataBusUtil.typeFields.get(newTreeBean.getUpstreamTypeId());
//                if (null != fields && fields.size() > 0) {
//                    for (UpstreamTypeField field : fields) {
//                        String sourceField = field.getSourceField();
//                        if ("delMark".equals(sourceField)) {
//                            delFlag = false;
//                        }
//                        Object newValue = ClassCompareUtil.getGetMethod(newTreeBean, sourceField);
//                        Object oldValue = ClassCompareUtil.getGetMethod(oldTreeBean, sourceField);
//                        if (null == oldValue && null == newValue) {
//                            continue;
//                        }
//                        if (null != oldValue && oldValue.equals(newValue)) {
//                            continue;
//                        }
//                        flag = true;
//                        ClassCompareUtil.setValue(oldTreeBean, oldTreeBean.getClass(), sourceField, oldValue, newValue);
//                        logger.debug("岗位信息更新{}:字段{}: {} -> {} ", newTreeBean.getCode(), sourceField, oldValue, newValue);
//                    }
//                }
//
//                if (delFlag && oldTreeBean.getDelMark().equals(1)) {
//                    flag = true;
//                    logger.info("岗位信息{}从删除恢复", oldTreeBean.getCode());
//                }

//                if (flag) {
                logger.info("岗位对比后需要修改{}", key.getKey().toString());
                updateList.add(key.getKey());
//                }
            }
        }


        Integer flag = postDao.renewData(insertList, updateList, deleteList, tenantId);
        if (null != flag && flag > 0) {


            logger.info("post插入 {} 条数据  {}", insertList.size(), System.currentTimeMillis());


            logger.info("post更新 {} 条数据  {}", updateList.size(), System.currentTimeMillis());


            logger.info("post删除 {} 条数据  {}", deleteList.size(), System.currentTimeMillis());
        } else {

            logger.error("数据更新sso数据库失败{}", System.currentTimeMillis());
        }

    }

    /**
     * @param domainName
     * @Description: 通过租户获取sso库的数据
     * @return: java.util.List<com.qtgl.iga.bean.DeptBean>
     */
    @Override
    public List<TreeBean> findDeptByDomainName(String domainName) throws Exception {
//        获取tenantId
        Tenant byDomainName = tenantDao.findByDomainName(domainName);
        if (null != byDomainName) {
            return postDao.findPostType(byDomainName.getId());
        } else {
            throw new Exception("租户不存在");
        }


    }


    /**
     * @param mainTree
     * @Description: 处理数据
     * @return: void
     */
    private List<TreeBean> dataProcessing(Map<String, TreeBean> mainTree, DomainInfo domainInfo, String treeTypeId, List<TreeBean> ssoBeans, Map<TreeBean, String> result, ArrayList<TreeBean> insert, LocalDateTime now) {
        //将sso的数据转化为map方便对比
        Map<String, TreeBean> ssoCollect = new HashMap<>();
        if (null != ssoBeans && ssoBeans.size() > 0) {
            ssoCollect = ssoBeans.stream().collect(Collectors.toMap((TreeBean::getCode), (dept -> dept)));
        }
        //拉取的数据
        Collection<TreeBean> mainDept = mainTree.values();
        ArrayList<TreeBean> pullBeans = new ArrayList<>(mainDept);
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
                                //使用sso的对象,将需要修改的值赋值
                                if (!"BUILTIN".equals(pullBean.getDataSource())) {
                                    ssoBean.setDataSource("PULL");
                                    ssoBean.setSource(pullBean.getSource());
                                    ssoBean.setUpdateTime(now);
                                    ssoBean.setActive(pullBean.getActive());
                                }
                                ssoBean.setColor(pullBean.getColor());
                                ssoBean.setIsRuled(pullBean.getIsRuled());
                                //标识数据是否需要修改
                                boolean updateFlag = false;
                                //标识上游是否给出删除标记
                                boolean delFlag = true;

                                List<UpstreamTypeField> fields = null;
                                if (null != pullBean.getUpstreamTypeId()) {
                                    fields = DataBusUtil.typeFields.get(pullBean.getUpstreamTypeId());
                                }
                                //获取对应上游源的映射字段
                                if (null != fields && fields.size() > 0) {
                                    for (UpstreamTypeField field : fields) {
                                        String sourceField = field.getSourceField();
                                        //如果上游给出删除标记 则使用上游的
                                        if ("delMark".equals(sourceField)) {
                                            //将删除标记置为false标识(不是用自己的delMark)
                                            delFlag = false;
                                        }
                                        //如果上游给出启用状态 则使用上游的 否则不改动
                                        if ("active".equals(sourceField)) {
                                            ssoBean.setActive(pullBean.getActive());
                                        }
                                        Object newValue = ClassCompareUtil.getGetMethod(pullBean, sourceField);
                                        Object oldValue = ClassCompareUtil.getGetMethod(ssoBean, sourceField);
                                        if (null == oldValue && null == newValue) {
                                            continue;
                                        }
                                        if (null != oldValue && oldValue.equals(newValue)) {
                                            continue;
                                        }
                                        //将修改表示改为true 标识数据需要修改
                                        updateFlag = true;
                                        //将值更新到sso对象
                                        ClassCompareUtil.setValue(ssoBean, ssoBean.getClass(), sourceField, oldValue, newValue);
                                        logger.debug("岗位信息更新{}:字段{}: {} -> {} ", pullBean.getCode(), sourceField, oldValue, newValue);
                                    }
                                }
                                //如果上游没给出删除标识,并且sso数据显示数据已被删除,则直接从删除恢复
                                //该操作主要是给删除标识手动赋值
                                if (delFlag && ssoBean.getDelMark().equals(1)) {
                                    ssoBean.setDelMark(0);
                                    updateFlag = true;
                                    logger.info("岗位信息{}从删除恢复", ssoBean.getCode());
                                }
                                if (updateFlag) {
                                    //将数据放入修改集合
                                    logger.info("岗位对比后需要修改{}", ssoBean.toString());

                                    ssoCollect.put(ssoBean.getCode(), ssoBean);
                                    result.put(ssoBean, "update");
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
                    insert.add(pullBean);
                    result.put(pullBean, "insert");
                }
            } else {
                //数据库数据为空的话,则默认全部新增
                insert.add(pullBean);
                result.put(pullBean, "insert");
            }

        }
        if (null != ssoBeans) {
            //查询数据库需要删除的数据
            for (TreeBean ssoBean : ssoBeans) {
                if (!"BUILTIN".equals(ssoBean.getDataSource()) || "PULL".equalsIgnoreCase(ssoBean.getDataSource())) {
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
                        //移除集合中删除对象
                        ssoCollect.remove(ssoBean.getCode());
                        //排除逻辑根节点
                        if (!("".equals(ssoBean.getCode()))) {
                            //如果为启用的再走删除并更新修改时间,
                            //本身就是删除的则不做处理
                            if (0 == ssoBean.getDelMark()) {
                                ssoBean.setUpdateTime(now);
                                result.put(ssoBean, "delete");

                            }
                        }
                    }
                }
            }
        }


        Collection<TreeBean> values = ssoCollect.values();
        return new ArrayList<>(values);
    }


}
