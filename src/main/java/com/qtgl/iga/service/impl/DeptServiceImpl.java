package com.qtgl.iga.service.impl;


import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.dao.*;
import com.qtgl.iga.service.DeptService;
import com.qtgl.iga.service.NodeService;

import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Transactional
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
        // 获取租户下开启的部门类型
        List<DeptTreeType> deptTreeTypes = deptTreeTypeDao.findAll(new HashMap<>(), domain.getId());
        for (DeptTreeType deptType : deptTreeTypes) {
            Map<String, TreeBean> mainTreeMap = new ConcurrentHashMap<>();
            // id 改为code
            calculationService.nodeRules(domain, deptType.getCode(), "", mainTreeMap, status, TYPE);
            //  数据合法性
            Collection<TreeBean> mainDept = mainTreeMap.values();
            ArrayList<TreeBean> mainList = new ArrayList<>(mainDept);
            // 判断重复(code)
            calculationService.groupByCode(mainList);
            //同步到sso
            beans = saveToSso(mainTreeMap, domain, deptType.getCode(), beans, result);
        }
        calculationService.groupByCode(beans);

        return beans;

    }

    /**
     * @Description: 通过租户获取sso库的数据
     * @param domainName
     * @param treeType
     * @param delMark
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
        List<TreeBean> beans = deptDao.findByTenantId(tenant.getId(), null,null);
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
        // 获取租户下开启的部门类型
        List<DeptTreeType> deptTreeTypes = deptTreeTypeDao.findAll(new HashMap<>(), domain.getId());
        for (DeptTreeType deptType : deptTreeTypes) {
            Map<String, TreeBean> mainTreeMap = new ConcurrentHashMap<>();
            //  id 改为code
            calculationService.nodeRules(domain, deptType.getCode(), "", mainTreeMap, 0, TYPE);
            //  数据合法性
            Collection<TreeBean> mainDept = mainTreeMap.values();
            ArrayList<TreeBean> mainList = new ArrayList<>(mainDept);
            // 判断重复(code)
            calculationService.groupByCode(mainList);
            //同步到sso
            beans = saveToSso(mainTreeMap, domain, deptType.getCode(), beans, result);
        }
        calculationService.groupByCode(beans);
        saveToSso(result, tenant.getId());
        return result;
    }

//    @SneakyThrows
//    @Override
//    public List<DeptBean> buildDeptByDomain(DomainInfo domain) {
//      /*  logger.info("hostname{}", hostname);
//        //
//        Map<String, List<DeptBean>> mainTreeMapGroupType = new ConcurrentHashMap<>();
//        List<DomainInfo> domainInfos = domainInfoDao.findAll();
//
//        for (DomainInfo domain : domainInfos) {*/
//
//        //  获取sso整树
//        //sso dept库的数据(通过domain 关联tenant查询)
//        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
//        if (null == tenant) {
//            throw new Exception("租户不存在");
//        }
//        //通过tenantId查询ssoApis库中的数据
//        List<DeptBean> beans = deptDao.findByTenantId(tenant.getId(), null, null);
//        if (null != beans && beans.size() > 0) {
//            //将null赋为""
//            for (DeptBean bean : beans) {
//                if (null == bean.getParentCode()) {
//                    bean.setParentCode("");
//                }
//            }
//        }
//        //轮训比对标记(是否有主键id)
//        Map<DeptBean, String> result = new HashMap<>();
//        // 获取租户下开启的部门类型
//        List<DeptTreeType> deptTreeTypes = deptTreeTypeDao.findAll(new HashMap<>(), domain.getId());
//        for (DeptTreeType deptType : deptTreeTypes) {
//            Map<String, DeptBean> mainTreeMap = new ConcurrentHashMap<>();
//            // id 改为code
//            calculationService.nodeRules(domain, deptType.getCode(), "", mainTreeMap, 0, TYPE);
//            //  数据合法性
//            Collection<DeptBean> mainDept = mainTreeMap.values();
//            ArrayList<DeptBean> mainList = new ArrayList<>(mainDept);
//            // 判断重复(code)
//            calculationService.groupByCode(mainList);
//            //同步到sso
//            beans = saveToSso(mainTreeMap, domain, deptType.getCode(), beans, result);
//        }
//        //}
//        calculationService.groupByCode(beans);
//
////        if (status) {
////            saveToSso(result, tenant.getId());
////        }
//
//
//        return beans;
//    }

    /**
     * @param result
     * @param tenantId
     * @Description: 插入sso数据库
     * @return: void
     */
    private void saveToSso(Map<TreeBean, String> result, String tenantId) throws Exception {
        //插入数据
        Map<String, List<Map.Entry<TreeBean, String>>> collect = result.entrySet().stream().collect(Collectors.groupingBy(c -> c.getValue()));
        List<Map.Entry<TreeBean, String>> insert = collect.get("insert");
        if (null != insert && insert.size() > 0) {
            ArrayList<TreeBean> list = new ArrayList<>();
            for (Map.Entry<TreeBean, String> key : insert) {

                list.add(key.getKey());
            }
            ArrayList<TreeBean> depts = deptDao.saveDept(list, tenantId);
            if (null != depts && depts.size() > 0) {
                logger.info("插入" + list.size() + "条数据{}", depts.toString());
            } else {
                throw new Exception("插入失败");
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
            ArrayList<TreeBean> list = new ArrayList<>();
            for (Map.Entry<TreeBean, String> key : update) {
                list.add(key.getKey());
            }
            // 修改同时要比对是否时间戳比数据库中更新
            ArrayList<TreeBean> depts = deptDao.updateDept(list, tenantId);
            if (null != depts && depts.size() > 0) {
                logger.info("更新" + list.size() + "条数据{}", depts.toString());
            } else {
                throw new Exception("更新失败");
            }

        }
        List<Map.Entry<TreeBean, String>> delete = collect.get("delete");
        //删除数据
        if (null != delete && delete.size() > 0) {
            ArrayList<TreeBean> list = new ArrayList<>();
            for (Map.Entry<TreeBean, String> key : delete) {
                list.add(key.getKey());
            }
            ArrayList<TreeBean> depts = deptDao.deleteDept(list);
            if (null != depts && depts.size() > 0) {
                logger.info("删除" + list.size() + "条数据{}", depts.toString());
            } else {
                throw new Exception("删除失败");
            }
        }
    }


    /**
     * @param mainTree
     * @Description: 处理数据
     * @return: void
     */
    private List<TreeBean> saveToSso(Map<String, TreeBean> mainTree, DomainInfo domainInfo, String treeTypeId, List<TreeBean> beans, Map<TreeBean, String> result) {
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
                        if (treeBean.getTreeType().equals(bean.getTreeType())) {
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
                    }
                }
                //没有相等的应该是新增
                if (flag) {
                    //新增
                    collect.put(treeBean.getCode(), treeBean);
                    result.put(treeBean, "insert");
                }
            } else {
                collect.put(treeBean.getCode(), treeBean);
                result.put(treeBean, "insert");
            }
        }
        if (null != beans) {
            //查询数据库需要删除的数据
            for (TreeBean bean : beans) {
                if (null != bean.getTreeType() && bean.getTreeType().equals(treeTypeId)) {
                    boolean flag = true;
                    for (TreeBean treeBean : result.keySet()) {
                        if (bean.getCode().equals(treeBean.getCode())) {
                            flag = false;
                            break;
                        }
                    }
                    if (flag) {
                        TreeBean treeBean = new TreeBean();
                        treeBean.setCode(bean.getCode());
                        collect.remove(treeBean.getCode());
                        result.put(treeBean, "delete");
                    }
                }
            }
        }


        Collection<TreeBean> values = collect.values();
        return new ArrayList<>(values);


    }


}
