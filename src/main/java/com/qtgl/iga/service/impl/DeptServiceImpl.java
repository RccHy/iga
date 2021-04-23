package com.qtgl.iga.service.impl;


import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.dao.*;
import com.qtgl.iga.service.DeptService;
import com.qtgl.iga.service.NodeService;

import com.qtgl.iga.utils.ClassCompareUtil;
import com.qtgl.iga.utils.DataBusUtil;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Value("${iga.hostname}")
    String hostname;

    private final String TYPE = "dept";


    /**
     * graphql 查询方法实现
     *
     * @param arguments 查询参数
     * @param domain    租户信息
     * @return
     * @throws Exception
     */
    @Override
    public List<TreeBean> findDept(Map<String, Object> arguments, DomainInfo domain) throws Exception {
        Integer status = nodeService.judgeEdit(arguments, domain, TYPE);
        if (status == null) {
            return null;
        }
        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        if (null == tenant) {
            throw new Exception("租户不存在");
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
        // 获取租户下开启的部门类型
        List<DeptTreeType> deptTreeTypes = deptTreeTypeDao.findAll(new HashMap<>(), domain.getId());
        for (DeptTreeType deptType : deptTreeTypes) {
//            Map<String, TreeBean> mainTreeMap = new ConcurrentHashMap<>();
            List<TreeBean> mainTreeBeans = new ArrayList<>();
            // id 改为code
            calculationService.nodeRules(domain, deptType.getCode(), "", mainTreeBeans, status, TYPE, "system");
            //  数据合法性
//            Collection<TreeBean> mainDept = mainTreeMap.values();
//            ArrayList<TreeBean> mainList = new ArrayList<>(mainDept);
            // 判断重复(code)
            calculationService.groupByCode(mainTreeBeans);
//            //同步到sso
            Map<String, TreeBean> mainTreeMap = mainTreeBeans.stream().collect(Collectors.toMap(TreeBean::getCode, deptBean -> deptBean));
            beans = dataProcessing(mainTreeMap, domain, deptType.getCode(), beans, result, insert);
        }
        if (null != beans) {
            beans.addAll(insert);
        }
        calculationService.groupByCode(beans);
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
        List<TreeBean> byTenantId = deptDao.findByTenantId(byDomainName.getId(), treeType, delMark);

        return byTenantId;
    }


    /**
     * 构建部门树 并 插入进 sso-api 数据库
     * 返回操作语句记录
     *
     * @param domain
     * @return
     */
    @SneakyThrows
    @Override
    public Map<TreeBean, String> buildDeptUpdateResult(DomainInfo domain) {
        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        if (null == tenant) {
            throw new Exception("租户不存在");
        }
        //通过tenantId查询ssoApis库中的数据
        List<TreeBean> beans = deptDao.findByTenantId(tenant.getId(), null, null);
        ArrayList<TreeBean> logBeans = new ArrayList<>();
        if (null != beans && beans.size() > 0) {
            //将null赋为""
            for (TreeBean bean : beans) {
                if (null == bean.getParentCode()) {
                    bean.setParentCode("");
                }
            }
            logBeans.addAll(beans);
        }


        //轮训比对标记(是否有主键id)
        Map<TreeBean, String> result = new HashMap<>();
        ArrayList<TreeBean> treeBeans = new ArrayList<>();
        // 获取租户下开启的部门类型
        List<DeptTreeType> deptTreeTypes = deptTreeTypeDao.findAll(new HashMap<>(), domain.getId());
        for (DeptTreeType deptType : deptTreeTypes) {
//            Map<String, TreeBean> mainTreeMap = new ConcurrentHashMap<>();
            List<TreeBean> mainTreeBeans = new ArrayList<>();
            //  id 改为code
            calculationService.nodeRules(domain, deptType.getCode(), "", mainTreeBeans, 0, TYPE, "task");
            //  数据合法性
//            Collection<TreeBean> mainDept = mainTreeMap.values();
//            ArrayList<TreeBean> mainList = new ArrayList<>(mainDept);
            // 判断重复(code)
            calculationService.groupByCode(mainTreeBeans);
            //同步到sso
            Map<String, TreeBean> mainTreeMap = mainTreeBeans.stream().collect(Collectors.toMap(TreeBean::getCode, deptBean -> deptBean));
            beans = dataProcessing(mainTreeMap, domain, deptType.getCode(), beans, result, treeBeans);
        }
        if (null != treeBeans && treeBeans.size() > 0) {
            beans.addAll(treeBeans);
        }
        calculationService.groupByCode(beans);
        saveToSso(result, tenant.getId(), logBeans);
        return result;
    }


    /**
     * @param result
     * @param tenantId
     * @Description: 插入sso数据库
     * @return: void
     */
    public void saveToSso(Map<TreeBean, String> result, String tenantId, List<TreeBean> logBeans) {
        //插入数据
        Map<String, List<Map.Entry<TreeBean, String>>> collect = result.entrySet().stream().collect(Collectors.groupingBy(c -> c.getValue()));
        Map<String, TreeBean> logCollect = null;

        ArrayList<TreeBean> insertList = new ArrayList<>();
        ArrayList<TreeBean> updateList = new ArrayList<>();
        ArrayList<TreeBean> deleteList = new ArrayList<>();


        if (null != logBeans && logBeans.size() > 0) {
            logCollect = logBeans.stream().collect(Collectors.toMap((TreeBean::getCode), (dept -> dept)));
        }
        List<Map.Entry<TreeBean, String>> insert = collect.get("insert");
        if (null != insert && insert.size() > 0) {
            for (Map.Entry<TreeBean, String> key : insert) {

                insertList.add(key.getKey());
            }
        }
        List<Map.Entry<TreeBean, String>> obsolete = collect.get("obsolete");
        if (null != obsolete && obsolete.size() > 0) {
            ArrayList<TreeBean> list = new ArrayList<>();
            for (Map.Entry<TreeBean, String> key : obsolete) {
                list.add(key.getKey());
            }
            logger.info("忽略" + list.size() + "条已过时数据{}", obsolete.toString());

        }
        List<Map.Entry<TreeBean, String>> update = collect.get("update");
        //修改数据
        if (null != update && update.size() > 0) {
            for (Map.Entry<TreeBean, String> key : update) {
                key.getKey().setDataSource("PULL");
//                //统计修改字段
//                if (null != logBeans && logBeans.size() > 0) {
//                    TreeBean treeBean = logCollect.get(key.getKey().getCode());
//                    Map<String, Map<String, Object>> stringMapMap = ClassCompareUtil.compareObject(treeBean, key.getKey());
//                    for (Map.Entry<String, Map<String, Object>> stringMapEntry : stringMapMap.entrySet()) {
//                        System.out.println(treeBean.getCode() + "-----------" + stringMapEntry.getKey() + "-------------" + stringMapEntry.getValue());
//                    }
//                }
                TreeBean newTreeBean = key.getKey();
                TreeBean oldTreeBean = logCollect.get(newTreeBean.getCode());
                oldTreeBean.setSource(newTreeBean.getSource());
                oldTreeBean.setUpdateTime(newTreeBean.getUpdateTime());
                oldTreeBean.setActive(newTreeBean.getActive());
                //根据upstreamTypeId查询fileds
                boolean flag = false;
                boolean delFlag = true;


                List<UpstreamTypeField> fields = DataBusUtil.typeFields.get(newTreeBean.getUpstreamTypeId());
                if (null != fields && fields.size() > 0) {
                    for (UpstreamTypeField field : fields) {
                        String sourceField = field.getSourceField();
                        if ("delMark".equals(sourceField)) {
                            delFlag = false;
                        }
                        Object newValue = ClassCompareUtil.getGetMethod(newTreeBean, sourceField);
                        Object oldValue = ClassCompareUtil.getGetMethod(oldTreeBean, sourceField);
                        if (null == oldValue && null == newValue) {
                            continue;
                        }
                        if (null != oldValue && oldValue.equals(newValue)) {
                            continue;
                        }
                        flag = true;
                        ClassCompareUtil.setValue(oldTreeBean, oldTreeBean.getClass(), sourceField, oldValue, newValue);
                        logger.info(newTreeBean.getCode() + "字段" + sourceField + "-----------" + oldValue + "-------------" + newValue);
                    }
                }

                if (delFlag && oldTreeBean.getDelMark().equals(1)) {
                    flag = true;
                    logger.info(newTreeBean.getCode() + "重新启用");
                }

                if (flag) {
                    updateList.add(oldTreeBean);
                }
            }
        }
        List<Map.Entry<TreeBean, String>> delete = collect.get("delete");
        //删除数据
        if (null != delete && delete.size() > 0) {
            for (Map.Entry<TreeBean, String> key : delete) {
                TreeBean newTreeBean = key.getKey();
                TreeBean oldTreeBean = logCollect.get(newTreeBean.getCode());
                if (oldTreeBean.getDelMark() == 0) {
                    if (null != oldTreeBean.getUpdateTime() && (newTreeBean.getCreateTime().isAfter(oldTreeBean.getUpdateTime()) || newTreeBean.getCreateTime().isEqual(oldTreeBean.getUpdateTime()))) {
                        deleteList.add(key.getKey());
                    }
                }
            }
        }


        Integer flag = deptDao.renewData(insertList, updateList, deleteList, tenantId);
        if (null != flag && flag > 0) {


            logger.info("dept插入" + insertList.size() + "条数据  {}", System.currentTimeMillis());


            logger.info("dept更新" + updateList.size() + "条数据  {}", System.currentTimeMillis());


            logger.info("dept删除" + deleteList.size() + "条数据  {}", System.currentTimeMillis());
        } else {

            logger.error("数据更新ssoApi数据库失败{}", System.currentTimeMillis());
        }


    }


    /**
     * @param mainTree
     * @Description: 处理数据
     * @return: void
     */
    private List<TreeBean> dataProcessing(Map<String, TreeBean> mainTree, DomainInfo domainInfo, String treeTypeId, List<TreeBean> beans, Map<TreeBean, String> result, ArrayList<TreeBean> insert) {
        Map<String, TreeBean> collect = new HashMap<>();
        if (null != beans && beans.size() > 0) {
            collect = beans.stream().collect(Collectors.toMap((TreeBean::getCode), (dept -> dept)));
        }

        //拉取的数据
        Collection<TreeBean> mainDept = mainTree.values();
        ArrayList<TreeBean> treeBeans = new ArrayList<>(mainDept);


        //遍历拉取数据
        for (TreeBean treeBean : treeBeans) {
            //标记新增还是修改
            boolean flag = true;
            //赋值treeTypeId
            treeBean.setTreeType(treeTypeId);
            if (null != beans) {
                //遍历数据库数据
                for (TreeBean bean : beans) {
                    if (treeBean.getCode().equals(bean.getCode())) {
                        //
//                        if (treeBean.getTreeType().equals(bean.getTreeType())) {
                        if (null != treeBean.getCreateTime()) {
                            //修改
                            if (null == bean.getCreateTime() || treeBean.getCreateTime().isAfter(bean.getCreateTime())) {
                                //新来的数据更实时
                                collect.put(treeBean.getCode(), treeBean);
                                result.put(treeBean, "update");
                            } else {
                                result.put(treeBean, "obsolete");
                            }
                        } else {
                            result.put(treeBean, "obsolete");
                        }
                        flag = false;
                    }
//                        else {
//                            throw new RuntimeException(deptBean + "与" + bean + "code重复");
//                        }
//                    }
                }
                //没有相等的应该是新增
                if (flag) {
                    //新增
//                    collect.put(treeBean.getCode(), treeBean);
                    insert.add(treeBean);
                    result.put(treeBean, "insert");
                }
            } else {
//                collect.put(treeBean.getCode(), treeBean);
                insert.add(treeBean);
                result.put(treeBean, "insert");
            }
        }
        if (null != beans) {
            //查询数据库需要删除的数据
            for (TreeBean bean : beans) {
                if (null != bean.getTreeType() && bean.getTreeType().equals(treeTypeId)) {
                    boolean flag = true;
                    for (TreeBean treeBean : result.keySet()) {
                        if (bean.getCode().equals(treeBean.getCode()) || (!"PULL".equals(bean.getDataSource()))) {
                            flag = false;
                            break;
                        }
                    }
                    if (flag) {
                        TreeBean treeBean = new TreeBean();
                        treeBean.setCode(bean.getCode());
                        collect.remove(treeBean.getCode());
                        result.put(bean, "delete");
                    }
                }
            }
        }


        Collection<TreeBean> values = collect.values();
        return new ArrayList<>(values);


    }


}
