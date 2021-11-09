package com.qtgl.iga.service.impl;


import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.dao.*;
import com.qtgl.iga.service.DeptService;
import com.qtgl.iga.service.NodeService;
import com.qtgl.iga.utils.ClassCompareUtil;
import com.qtgl.iga.utils.DataBusUtil;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
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
@Transactional(rollbackFor = Exception.class)
public class DeptServiceImpl implements DeptService {


    public static Logger logger = LoggerFactory.getLogger(DeptServiceImpl.class);


    @Autowired
    DeptDao deptDao;
    @Autowired
    DomainInfoDao domainInfoDao;
    @Autowired
    NodeDao nodeDao;
    @Autowired
    DeptTreeTypeDao deptTreeTypeDao;
    @Autowired
    TenantDao tenantDao;
    @Autowired
    NodeService nodeService;
    @Autowired
    NodeRulesCalculationServiceImpl calculationService;
    @Autowired
    UpstreamTypeDao upstreamTypeDao;
    @Autowired
    OccupyDao occupyDao;
    @Autowired
    MonitorRulesDao monitorRulesDao;
    @Autowired
    NodeRulesCalculationServiceImpl rulesCalculationService;

    @Value("${iga.hostname}")
    String hostname;
    //类型
    private final String TYPE = "dept";


    /**
     * graphql 查询方法实现
     *
     * @param arguments 查询参数
     * @param domain    租户信息
     * @return
     * @throws Exception
     * @Description: 根据状态租户查询对应规则拉取对应部门数据, 首先遍历SSO中API数据的规则, 然后遍历非API数据, 覆盖方式 以PULL源覆盖API源
     */
    @Override
    public List<TreeBean> findDept(Map<String, Object> arguments, DomainInfo domain) throws Exception {
        Integer status = (Integer) arguments.get("status");
        //是否需要复制规则
        if ((Boolean) arguments.get("scenes")) {
            status = nodeService.judgeEdit(arguments, domain, TYPE);
            if (status == null) {
                return null;
            }
        }
        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        if (null == tenant) {
            throw new CustomException(ResultCode.FAILED, "租户不存在");
        }
        //通过tenantId查询ssoApis库中的数据
        List<TreeBean> beans = deptDao.findByTenantId(tenant.getId(), null, null);
        if (null != beans && beans.size() > 0) {
            //将null赋为""
            for (TreeBean bean : beans) {
                if (null == bean.getParentCode()) {
                    bean.setParentCode("");
                }
            }
        }
        //轮训比对标记(是否有主键id)
        Map<TreeBean, String> result = new HashMap<>();
        ArrayList<TreeBean> insert = new ArrayList<>();
        List<TreeBean> mainTreeBeans = new ArrayList<>();

        // 获取租户下开启的部门类型
        List<DeptTreeType> deptTreeTypes = deptTreeTypeDao.findAll(new HashMap<>(), domain.getId());
        final LocalDateTime now = LocalDateTime.now();
        for (DeptTreeType deptType : deptTreeTypes) {

            List<TreeBean> ssoApiBeans = deptDao.findBySourceAndTreeType("API", deptType.getCode(), tenant.getId());
            if (null != ssoApiBeans && ssoApiBeans.size() > 0) {
                mainTreeBeans.addAll(ssoApiBeans);
                rulesCalculationService.groupByCode(mainTreeBeans, status, domain);
                for (TreeBean ssoApiBean : ssoApiBeans) {
                    mainTreeBeans = calculationService.nodeRules(domain, deptType.getCode(), ssoApiBean.getCode(), mainTreeBeans, status, TYPE);
                }
            }

            // id 改为code
            mainTreeBeans = calculationService.nodeRules(domain, deptType.getCode(), "", mainTreeBeans, status, TYPE);

        }
        //同步到sso
        Map<String, TreeBean> mainTreeMap = mainTreeBeans.stream().collect(Collectors.toMap(TreeBean::getCode, deptBean -> deptBean));
        beans = dataProcessing(mainTreeMap, domain, beans, result, insert, now);
//        //如果插入的数据不为空则加入返回集
//        if (null != beans) {
//            beans.addAll(insert);
//        }
        //code重复性校验
        calculationService.groupByCode(beans, status, domain);
        return beans;

    }

    /**
     * @param domainName
     * @param treeType
     * @param delMark
     * @Description: 通过租户获取sso库的数据
     * @return: java.util.List<com.qtgl.iga.bean.DeptBean>
     */
    @Override
    public List<TreeBean> findDeptByDomainName(String domainName, String treeType, Integer delMark) {
        Tenant byDomainName = tenantDao.findByDomainName(domainName);

        return deptDao.findByTenantId(byDomainName.getId(), treeType, delMark);
    }


    /**
     * 构建部门树 并 插入进 sso-api 数据库
     * 返回操作语句记录
     *
     * @param domain
     * @return
     */
    @Override
    public Map<TreeBean, String> buildDeptUpdateResult(DomainInfo domain, TaskLog lastTaskLog) throws Exception {
        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        if (null == tenant) {
            throw new CustomException(ResultCode.FAILED, "租户不存在");
        }
        //通过tenantId查询ssoApis库中的数据
        List<TreeBean> beans = deptDao.findByTenantId(tenant.getId(), null, null);
        if (null != beans && beans.size() > 0) {
            //将null赋为""
            for (TreeBean bean : beans) {
                if (null == bean.getParentCode()) {
                    bean.setParentCode("");
                }
            }
        }
        //轮训比对标记(是否有主键id)
        Map<TreeBean, String> result = new HashMap<>();
        ArrayList<TreeBean> treeBeans = new ArrayList<>();
        List<TreeBean> mainTreeBeans = new ArrayList<>();

        // 获取租户下开启的部门类型
        List<DeptTreeType> deptTreeTypes = deptTreeTypeDao.findAll(new HashMap<>(), domain.getId());
        final LocalDateTime now = LocalDateTime.now();
        for (DeptTreeType deptType : deptTreeTypes) {

            List<TreeBean> ssoApiBeans = deptDao.findBySourceAndTreeType("API", deptType.getCode(), tenant.getId());

            if (null != ssoApiBeans && ssoApiBeans.size() > 0) {
                mainTreeBeans.addAll(ssoApiBeans);
                rulesCalculationService.groupByCode(mainTreeBeans, 0, domain);
                for (TreeBean ssoApiBean : ssoApiBeans) {
                    mainTreeBeans = calculationService.nodeRules(domain, deptType.getCode(), ssoApiBean.getCode(), mainTreeBeans, 0, TYPE);
                }
            }
            //  id 改为code
            mainTreeBeans = calculationService.nodeRules(domain, deptType.getCode(), "", mainTreeBeans, 0, TYPE);

        }
        //同步到sso
        Map<String, TreeBean> mainTreeMap = mainTreeBeans.stream().collect(Collectors.toMap(TreeBean::getCode, deptBean -> deptBean));
        beans = dataProcessing(mainTreeMap, domain, beans, result, treeBeans, now);
//        if (null != treeBeans && treeBeans.size() > 0) {
//            beans.addAll(treeBeans);
//        }
        //code重复性校验
        calculationService.groupByCode(beans, 0, domain);

        //  检测删除该部门 对 人员身份造成的影响。若影响范围过大，则停止操作。
        Map<String, List<Map.Entry<TreeBean, String>>> collect = result.entrySet().stream().collect(Collectors.groupingBy(c -> c.getValue()));
        List<Map.Entry<TreeBean, String>> delete = collect.get("delete");
        calculationService.monitorRules(domain, lastTaskLog, beans.size(), delete, "dept");

        //保存到数据库
        saveToSso(collect, tenant.getId());
        return result;
    }


    /**
     * @param collect
     * @param tenantId
     * @Description: 插入sso数据库
     * @return: void
     */
    public void saveToSso(Map<String, List<Map.Entry<TreeBean, String>>> collect, String tenantId) {
        //声明存储插入,修改,删除数据的容器
        ArrayList<TreeBean> insertList = new ArrayList<>();
        ArrayList<TreeBean> updateList = new ArrayList<>();
        ArrayList<TreeBean> deleteList = new ArrayList<>();
        ArrayList<TreeBean> invalidList = new ArrayList<>();

        List<Map.Entry<TreeBean, String>> delete = collect.get("delete");
        //删除数据
        if (null != delete && delete.size() > 0) {
            for (Map.Entry<TreeBean, String> key : delete) {
                logger.info("部门对比后删除{}", key.getKey().toString());
                deleteList.add(key.getKey());
            }
        }
        List<Map.Entry<TreeBean, String>> insert = collect.get("insert");
        if (null != insert && insert.size() > 0) {
            for (Map.Entry<TreeBean, String> key : insert) {
                logger.debug("部门对比后新增{}", key.getKey().toString());
                insertList.add(key.getKey());
            }
        }
        List<Map.Entry<TreeBean, String>> obsolete = collect.get("obsolete");
        if (null != obsolete && obsolete.size() > 0) {
            ArrayList<TreeBean> list = new ArrayList<>();
            for (Map.Entry<TreeBean, String> key : obsolete) {
                list.add(key.getKey());
            }
            logger.info("忽略 {} 条已过时数据{}", list.size(), obsolete);

        }
        List<Map.Entry<TreeBean, String>> update = collect.get("update");
        //修改数据
        if (null != update && update.size() > 0) {
            for (Map.Entry<TreeBean, String> key : update) {

                logger.info("部门对比后需要修改{}", key.getKey().toString());
                updateList.add(key.getKey());

            }
        }

        List<Map.Entry<TreeBean, String>> invalid = collect.get("invalid");
        //失效数据
        if (null != invalid && invalid.size() > 0) {
            for (Map.Entry<TreeBean, String> key : invalid) {

                logger.info("部门对比后需要置为失效{}", key.getKey().toString());
                invalidList.add(key.getKey());

            }
        }
        //恢复数据
        List<Map.Entry<TreeBean, String>> recover = collect.get("recover");
        if (null != recover && recover.size() > 0) {
            for (Map.Entry<TreeBean, String> key : recover) {
                logger.debug("部门对比后恢复新增{}", key.getKey().toString());
                updateList.add(key.getKey());
            }
        }


        Integer flag = deptDao.renewData(insertList, updateList, deleteList, invalidList, tenantId);
        if (null != flag && flag > 0) {


            logger.info("dept插入 {} 条数据  {}", null == insertList ? 0 : insertList.size(), System.currentTimeMillis());


            logger.info("dept更新 {} 条数据  {}", null == updateList ? 0 : updateList.size(), System.currentTimeMillis());


            logger.info("dept删除 {} 条数据  {}", null == deleteList ? 0 : deleteList.size(), System.currentTimeMillis());

            logger.info("dept失效 {} 条数据  {}", null == invalidList ? 0 : invalidList.size(), System.currentTimeMillis());
        } else {

            logger.error("数据更新ssoApi数据库失败{}", System.currentTimeMillis());
        }


    }


    /**
     * 1：根据规则拉取所有权威源部门数据
     * 2：获取sso所有的部门数据(包括已删除的)
     * <p>
     * 3：根据上游部门和数据库中部门进行对比
     * A：新增  上游提供、sso数据库中没有            对于active和del_mark字段 上游提供使用上游,不提供则默认有效未删除
     * B：修改  上游和sso对比后字段值有差异
     * C：删除  上游提供了del_mark             (上游必须del_mark字段)
     * D: 无效  上游曾经提供后，不再提供 OR 上游提供了active
     * E: 删除恢复  之前被标记为删除后再通过推送了相同的数据   (上游必须del_mark字段)
     * E: 失效恢复  之前被标记为失效后再通过推送了相同的数据   提供active则修改对比时直接覆盖,不提供则手动恢复
     **/
    //String treeTypeId
    private List<TreeBean> dataProcessing(Map<String, TreeBean> mainTree, DomainInfo domainInfo, List<TreeBean> ssoBeans, Map<TreeBean, String> result, ArrayList<TreeBean> insert, LocalDateTime now) {
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
            //标记新增标识
            boolean flag = true;

            if (null != ssoBeans) {
                //遍历数据库数据
                for (TreeBean ssoBean : ssoBeans) {
                    if (pullBean.getCode().equals(ssoBean.getCode())) {
                        ssoBean.setIsRuled(pullBean.getIsRuled());
                        ssoBean.setColor(pullBean.getColor());
                        if (null != pullBean.getCreateTime()) {
                            //修改
                            if (null == ssoBean.getCreateTime() || pullBean.getCreateTime().isAfter(ssoBean.getCreateTime())) {

                                //修改标识 1.source为API 则进行覆盖  2.source都为PULL则对比字段 有修改再标识为true
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

                                //使用sso的对象,将需要修改的值赋值
                                if ("API".equals(ssoBean.getDataSource())) {
                                    updateFlag = true;
                                }
                                ssoBean.setDataSource("PULL");
                                ssoBean.setSource(pullBean.getSource());
                                ssoBean.setUpdateTime(now);
                                ssoBean.setTreeType(pullBean.getTreeType());
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
                                        logger.debug("部门信息更新{}:字段{}: {} -> {} ", pullBean.getCode(), sourceField, oldValue, newValue);
                                    }
                                }
                                //标识为恢复数据
                                if (delRecoverFlag) {
                                    ssoBean.setDelMark(0);
                                    result.put(ssoBean, "recover");
                                    //修改标记置为false
                                    updateFlag = false;
                                    logger.info("部门信息{}从删除恢复", ssoBean.getCode());
                                }
                                //标识为删除的数据
                                if (delFlag) {
                                    //将数据放入删除集合
                                    ssoBean.setDelMark(1);
                                    ssoCollect.remove(ssoBean.getCode());
                                    result.put(ssoBean, "delete");
                                    //修改标记置为false
                                    updateFlag = false;
                                    logger.info("部门对比后需要删除{}", ssoBean);
                                }


                                //修改不为删除的数据
                                if (updateFlag && ssoBean.getDelMark() != 1) {

                                    ssoBean.setUpdateTime(now);
                                    //失效
                                    if (invalidFlag) {
                                        result.put(ssoBean, "invalid");
                                    } else {
//                                        //恢复失效数据  未提供active手动处理
//                                        if (invalidRecoverFlag && ssoBean.getActive() != 1) {
//                                            ssoBean.setActive(1);
//                                        }
                                        //将数据放入修改集合
                                        ssoCollect.put(ssoBean.getCode(), ssoBean);

                                        result.put(ssoBean, "update");
                                    }
                                    logger.info("部门对比后需要修改{}", ssoBean);

                                }
                                //上游未提供active并且sso与上游源该字段值不一致
                                if (!activeFlag && (!ssoBean.getActive().equals(pullBean.getActive()))) {
                                    ssoBean.setUpdateTime(now);
                                    ssoBean.setActive(pullBean.getActive());
                                    //将数据放入修改集合
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
//                    insert.add(pullBean);
                    ssoCollect.put(pullBean.getCode(), pullBean);
                    result.put(pullBean, "insert");
                }
            } else {
                //数据库数据为空的话,则默认全部新增
//                insert.add(pullBean);
                ssoCollect.put(pullBean.getCode(), pullBean);
                result.put(pullBean, "insert");
            }
        }


        if (null != ssoBeans) {
            //查询数据库需要置为失效的数据
            for (TreeBean ssoBean : ssoBeans) {
                //仅能操作拉取的数据
                if ("PULL".equalsIgnoreCase(ssoBean.getDataSource())) {
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
                        //只处理有效的置为无效, 本身就无效的忽略
                        if (ssoBean.getActive() == 1) {
                            ssoBean.setActive(0);
                            ssoBean.setUpdateTime(now);
                            result.put(ssoBean, "invalid");
                        }
                    }
                }
            }
        }


        Collection<TreeBean> values = ssoCollect.values();
        List<TreeBean> collect = new ArrayList<>(values).stream().filter(dept -> dept.getActive() != 0 && dept.getDelMark() != 1).collect(Collectors.toList());
        return collect;


    }


}
