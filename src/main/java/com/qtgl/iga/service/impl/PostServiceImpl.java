package com.qtgl.iga.service.impl;


import com.qtgl.iga.bean.TreeBean;

import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.Tenant;
import com.qtgl.iga.bo.UpstreamTypeField;
import com.qtgl.iga.dao.PostDao;
import com.qtgl.iga.dao.TenantDao;
import com.qtgl.iga.dao.UpstreamTypeDao;
import com.qtgl.iga.service.NodeService;
import com.qtgl.iga.service.PostService;

import com.qtgl.iga.utils.ClassCompareUtil;
import com.qtgl.iga.utils.DataBusUtil;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


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


    @Value("${iga.hostname}")
    String hostname;

    final String TYPE = "post";


    @SneakyThrows
    @Override
    @Transactional
    public Map<TreeBean, String> buildPostUpdateResult(DomainInfo domain) {
        //获取默认数据
        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        if (null == tenant) {
            throw new Exception("租户不存在");
        }
        //  查sso BUILTIN 的岗位
        List<TreeBean> rootBeans = postDao.findRootData(tenant.getId());
        if (null != rootBeans && rootBeans.size() > 0) {
            for (TreeBean rootBean : rootBeans) {
                if (null == rootBean.getParentCode()) {
                    rootBean.setParentCode("");
                }
            }
        } else {
            logger.error("租户 :{} 无BUILTIN岗位，请先创建", tenant.getId());
            throw new Exception("无BUILTIN岗位，请先创建");
        }
        //轮训比对标记(是否有主键id)
        Map<TreeBean, String> result = new HashMap<>();
        ArrayList<TreeBean> treeBeans = new ArrayList<>();


        List<TreeBean> mainTreeBeans = new ArrayList<>();
        final LocalDateTime now = LocalDateTime.now();
        for (TreeBean rootBean : rootBeans) {
            mainTreeBeans = calculationService.nodeRules(domain, null, rootBean.getCode(), mainTreeBeans, 0, TYPE, "task", rootBeans);
            // 判断重复(code)
            calculationService.groupByCode(mainTreeBeans, 0, rootBeans);
        }
//
        // 判断重复(code)
        calculationService.groupByCode(mainTreeBeans, 0, rootBeans);

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
        Map<String, TreeBean> collect = mainTreeBeans.stream().collect(Collectors.toMap(TreeBean::getCode, deptBean -> deptBean));

        //同步到sso
        beans = dataProcessing(collect, domain, "", beans, result, treeBeans, now);
        if (null != beans) {
            beans.addAll(treeBeans);
        }

        // 判断重复(code)
        calculationService.groupByCode(beans, 0, rootBeans);

        saveToSso(result, tenant.getId(), null);

        return result;

    }

    @Override
    public List<TreeBean> findPosts(Map<String, Object> arguments, DomainInfo domain) throws Exception {
        Integer status = nodeService.judgeEdit(arguments, domain, TYPE);

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
        if (status == null) {
            return null;
        }
        //sso dept库的数据(通过domain 关联tenant查询)
        if (null == tenant) {
            throw new Exception("租户不存在");
        }

        //轮训比对标记(是否有主键id)
        Map<TreeBean, String> result = new HashMap<>();
        ArrayList<TreeBean> treeBeans = new ArrayList<>();

        List<TreeBean> mainTreeBeans = new ArrayList<>();
        final LocalDateTime now = LocalDateTime.now();

        for (TreeBean rootBean : rootBeans) {
            mainTreeBeans = calculationService.nodeRules(domain, null, rootBean.getCode(), mainTreeBeans, status, TYPE, "system", rootBeans);
//            // 判断重复(code)
//            calculationService.groupByCode(mainTreeBeans, status, rootBeans);
        }

//        // 判断重复(code)
//        calculationService.groupByCode(mainTreeBeans, status, rootBeans);


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
        //同步到sso
        Map<String, TreeBean> mainTreeMap = mainTreeBeans.stream().collect(Collectors.toMap(TreeBean::getCode, deptBean -> deptBean));
        beans = dataProcessing(mainTreeMap, domain, "", beans, result, treeBeans, now);

        if (null != beans) {
            beans.addAll(treeBeans);
        }
        // 判断重复(code)
        calculationService.groupByCode(beans, status, rootBeans);

        return beans;

    }

    /**
     * @param result
     * @param tenantId
     * @Description: 增量插入sso数据库
     * @return: void
     */
    private void saveToSso(Map<TreeBean, String> result, String tenantId, List<TreeBean> logBeans) {
        Map<String, List<Map.Entry<TreeBean, String>>> collect = result.entrySet().stream().collect(Collectors.groupingBy(c -> c.getValue()));
//        Map<String, TreeBean> logCollect = null;

        ArrayList<TreeBean> insertList = new ArrayList<>();
        ArrayList<TreeBean> updateList = new ArrayList<>();
        ArrayList<TreeBean> deleteList = new ArrayList<>();

//        if (null != logBeans && logBeans.size() > 0) {
//            logCollect = logBeans.stream().collect(Collectors.toMap((TreeBean::getCode), (dept -> dept)));
//        }

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
        List<Map.Entry<TreeBean, String>> delete = collect.get("delete");
        //删除数据

        if (null != delete && delete.size() > 0) {
            for (Map.Entry<TreeBean, String> key : delete) {
//                TreeBean newTreeBean = key.getKey();
//                TreeBean oldTreeBean = logCollect.get(newTreeBean.getCode());
//                if (oldTreeBean.getDelMark() == 0) {
//                    if (null != oldTreeBean.getUpdateTime() && (newTreeBean.getCreateTime().isAfter(oldTreeBean.getUpdateTime()) || newTreeBean.getCreateTime().isEqual(oldTreeBean.getUpdateTime()))) {
                logger.info("岗位对比后删除{}", key.getKey().toString());
                deleteList.add(key.getKey());
//                    }
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
        Map<String, TreeBean> ssoCollect = new HashMap<>();
        if (null != ssoBeans && ssoBeans.size() > 0) {
            ssoCollect = ssoBeans.stream().collect(Collectors.toMap((TreeBean::getCode), (dept -> dept)));
        }
        //拉取的数据
        Collection<TreeBean> mainDept = mainTree.values();
        ArrayList<TreeBean> pullBeans = new ArrayList<>(mainDept);
        //遍历拉取数据
        for (TreeBean pullBean : pullBeans) {
            if (!"BUILTIN".equals(pullBean.getDataSource())) {
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
                                    ssoBean.setDataSource("PULL");
                                    ssoBean.setSource(pullBean.getSource());
                                    ssoBean.setUpdateTime(now);
                                    ssoBean.setActive(pullBean.getActive());
                                    //新来的数据更实时
                                    boolean updateFlag = false;
                                    boolean delFlag = true;

                                    List<UpstreamTypeField> fields = DataBusUtil.typeFields.get(pullBean.getUpstreamTypeId());
                                    if (null != fields && fields.size() > 0) {
                                        for (UpstreamTypeField field : fields) {
                                            String sourceField = field.getSourceField();
                                            if ("delMark".equals(sourceField)) {
                                                delFlag = false;
                                            }
                                            Object newValue = ClassCompareUtil.getGetMethod(pullBean, sourceField);
                                            Object oldValue = ClassCompareUtil.getGetMethod(ssoBean, sourceField);
                                            if (null == oldValue && null == newValue) {
                                                continue;
                                            }
                                            if (null != oldValue && oldValue.equals(newValue)) {
                                                continue;
                                            }
                                            updateFlag = true;
                                            ClassCompareUtil.setValue(ssoBean, ssoBean.getClass(), sourceField, oldValue, newValue);
                                            logger.debug("岗位信息更新{}:字段{}: {} -> {} ", pullBean.getCode(), sourceField, oldValue, newValue);
                                        }
                                    }
                                    if (delFlag && ssoBean.getDelMark().equals(1)) {
                                        updateFlag = true;
                                        logger.info("岗位信息{}从删除恢复", ssoBean.getCode());
                                    }
                                    if (updateFlag) {

                                        logger.info("岗位对比后需要修改{}", ssoBean.toString());

                                        ssoCollect.put(ssoBean.getCode(), ssoBean);
                                        result.put(ssoBean, "update");
                                    }

                                } else {
                                    result.put(pullBean, "obsolete");
                                }
                            } else {
                                result.put(pullBean, "obsolete");
                            }
                            flag = false;
                        }

                    }
                    //没有相等的应该是新增
                    if (flag) {
                        //新增
                        insert.add(pullBean);
                        result.put(pullBean, "insert");
                    }
                } else {
                    insert.add(pullBean);
                    result.put(pullBean, "insert");
                }
            }
        }
        if (null != ssoBeans) {
            //查询数据库需要删除的数据
            for (TreeBean ssoBean : ssoBeans) {
                if (!"BUILTIN".equals(ssoBean.getDataSource()) || "PULL".equalsIgnoreCase(ssoBean.getDataSource())) {
                    boolean flag = true;
                    for (TreeBean pullBean : pullBeans) {
                        if (ssoBean.getCode().equals(pullBean.getCode())) {
                            flag = false;
                            break;
                        }
                    }
                    if (flag) {
                        ssoCollect.remove(ssoBean.getCode());
                        if (0 == ssoBean.getDelMark()) {
                            ssoBean.setUpdateTime(now);
                            result.put(ssoBean, "delete");

                        }
                    }
                }
            }
        }


        Collection<TreeBean> values = ssoCollect.values();
        return new ArrayList<>(values);

    }


}
