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
        //如果插入的数据不为空则加入返回集
        if (null != beans) {
            beans.addAll(insert);
        }
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
        if (null != treeBeans && treeBeans.size() > 0) {
            beans.addAll(treeBeans);
        }
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


        Integer flag = deptDao.renewData(insertList, updateList, deleteList, tenantId);
        if (null != flag && flag > 0) {


            logger.info("dept插入 {} 条数据  {}", insertList.size(), System.currentTimeMillis());


            logger.info("dept更新 {} 条数据  {}", updateList.size(), System.currentTimeMillis());


            logger.info("dept删除 {} 条数据  {}", deleteList.size(), System.currentTimeMillis());
        } else {

            logger.error("数据更新ssoApi数据库失败{}", System.currentTimeMillis());
        }


    }


    /**
     * @param mainTree
     * @Description: 处理数据
     * @return: void
     */
    //, String treeTypeId
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
            //标记新增还是修改
            boolean flag = true;

            if (null != ssoBeans) {
                //遍历数据库数据
                for (TreeBean ssoBean : ssoBeans) {
                    if (pullBean.getCode().equals(ssoBean.getCode())) {
                        //
                        ssoBean.setIsRuled(pullBean.getIsRuled());
                        ssoBean.setColor(pullBean.getColor());
                        if (null != pullBean.getCreateTime()) {
                            //修改
                            if (null == ssoBean.getCreateTime() || pullBean.getCreateTime().isAfter(ssoBean.getCreateTime())) {

                                //标识数据是否需要修改
                                boolean updateFlag = false;
                                //标识上游是否给出删除标记
                                boolean delFlag = true;
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
                                        //如果上游给出删除标记 则使用上游的
                                        if ("delMark".equals(sourceField)) {
                                            ssoBean.setDelMark(pullBean.getDelMark());
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
                                        logger.debug("部门信息更新{}:字段{}: {} -> {} ", pullBean.getCode(), sourceField, oldValue, newValue);
                                    }
                                }
                                //如果上游没给出删除标识,并且sso数据显示数据已被删除,则直接从删除恢复
                                //该操作主要是给删除标识手动赋值
                                if (delFlag && ssoBean.getDelMark().equals(1)) {
                                    ssoBean.setDelMark(0);
                                    updateFlag = true;
                                    logger.info("部门信息{}从删除恢复", ssoBean.getCode());
                                }
                                if (updateFlag) {
                                    //将数据放入修改集合
                                    logger.info("部门对比后需要修改{}", ssoBean);

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
                //仅能操作拉取的数据
                if ("PULL".equals(ssoBean.getDataSource())) {
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


        Collection<TreeBean> values = ssoCollect.values();
        return new ArrayList<>(values);


    }


}
