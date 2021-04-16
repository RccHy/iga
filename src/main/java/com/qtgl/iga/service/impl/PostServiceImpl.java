package com.qtgl.iga.service.impl;


import com.qtgl.iga.bean.TreeBean;

import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.Tenant;
import com.qtgl.iga.dao.PostDao;
import com.qtgl.iga.dao.TenantDao;
import com.qtgl.iga.service.NodeService;
import com.qtgl.iga.service.PostService;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


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

    @Value("${iga.hostname}")
    String hostname;

    final String TYPE = "post";


    @Override
    public Map<TreeBean, String> buildPostUpdateResult(DomainInfo domain) throws Exception {
        //获取默认数据
        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        if (null == tenant) {
            throw new Exception("租户不存在");
        }
        //  查sso builtin 的岗位
        List<TreeBean> rootBeans = postDao.findRootData(tenant.getId());
        if (null != rootBeans && rootBeans.size() > 0) {
            for (TreeBean rootBean : rootBeans) {
                if (null == rootBean.getParentCode()) {
                    rootBean.setParentCode("");
                }
            }
        } else {
            throw new Exception("无builtin岗位，请先创建");
        }
        //轮训比对标记(是否有主键id)
        Map<TreeBean, String> result = new HashMap<>();
        //转化为map
        Map<String, TreeBean> rootMap = rootBeans.stream().collect(Collectors.toMap(TreeBean::getCode, deptBean -> deptBean));
        Map<String, TreeBean> mainTreeMap = rootBeans.stream().collect(Collectors.toMap(TreeBean::getCode, deptBean -> deptBean));
        // 将本次 add 进的 节点 进行 规则运算
        for (Map.Entry<String, TreeBean> entry : rootMap.entrySet()) {
            calculationService.nodeRules(domain, null, entry.getKey(), mainTreeMap, 0, TYPE, "task");
        }
        Collection<TreeBean> mainDept = mainTreeMap.values();
        ArrayList<TreeBean> mainList = new ArrayList<>(mainDept);
        // 判断重复(code)
        calculationService.groupByCode(mainList);

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
        beans = saveToSso(mainTreeMap, domain, "", beans, result);

        // 判断重复(code)
        calculationService.groupByCode(beans);

        saveToSso(result, tenant.getId());

        return result;
    }

    @Override
    public List<TreeBean> findPosts(Map<String, Object> arguments, DomainInfo domain) throws Exception {
        Integer status = nodeService.judgeEdit(arguments, domain, TYPE);
        //获取默认数据
        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        if (null == tenant) {
            throw new Exception("租户不存在");
        }
        //  置空mainTree
        List<TreeBean> rootBeans = postDao.findRootData(tenant.getId());
        if (null != rootBeans && rootBeans.size() > 0) {
            for (TreeBean rootBean : rootBeans) {
                if (null == rootBean.getParentCode()) {
                    rootBean.setParentCode("");
                }
            }
        } else {
            throw new Exception("请检查根树是否合法");
        }
        if (status == null) {
            return rootBeans;
        }

        //sso dept库的数据(通过domain 关联tenant查询)
        if (null == tenant) {
            throw new Exception("租户不存在");
        }

        //轮训比对标记(是否有主键id)
        Map<TreeBean, String> result = new HashMap<>();
        //转化为map
        Map<String, TreeBean> rootMap = rootBeans.stream().collect(Collectors.toMap(TreeBean::getCode, deptBean -> deptBean));
        Map<String, TreeBean> mainTreeMap = rootBeans.stream().collect(Collectors.toMap(TreeBean::getCode, deptBean -> deptBean));
        // 将本次 add 进的 节点 进行 规则运算
        for (Map.Entry<String, TreeBean> entry : rootMap.entrySet()) {
            calculationService.nodeRules(domain, null, entry.getKey(), mainTreeMap, status, TYPE, "system");
        }
        System.out.println("==");

        Collection<TreeBean> mainDept = mainTreeMap.values();
        ArrayList<TreeBean> mainList = new ArrayList<>(mainDept);

        // 判断重复(code)
        calculationService.groupByCode(mainList);

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
        beans = saveToSso(mainTreeMap, domain, "", beans, result);

        // 判断重复(code)
        calculationService.groupByCode(beans);

//        saveToSso(result, tenant.getId());

        return beans;
    }

    /**
     * @param result
     * @param id
     * @Description: 增量插入sso数据库
     * @return: void
     */
    private void saveToSso(Map<TreeBean, String> result, String id) throws Exception {
        Map<String, List<Map.Entry<TreeBean, String>>> collect = result.entrySet().stream().collect(Collectors.groupingBy(c -> c.getValue()));

        List<Map.Entry<TreeBean, String>> insert = collect.get("insert");
        //插入数据
        if (null != insert && insert.size() > 0) {
            ArrayList<TreeBean> list = new ArrayList<>();
            for (Map.Entry<TreeBean, String> key : insert) {
                list.add(key.getKey());
            }
            ArrayList<TreeBean> depts = postDao.saveDept(list, id);
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
            ArrayList<TreeBean> depts = postDao.updateDept(list, id);
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
            ArrayList<TreeBean> depts = postDao.deleteDept(list);
            if (null != depts && depts.size() > 0) {
                logger.info("删除" + list.size() + "条数据{}", depts.toString());
            } else {
                throw new Exception("删除失败");
            }
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


//    private void groupByCode(List<DeptBean> deptBeans) throws Exception {
//        HashMap<String, Integer> map = new HashMap<>();
//        HashMap<String, Integer> result = new HashMap<>();
//        if (null != deptBeans && deptBeans.size() > 0) {
//            for (DeptBean deptBean : deptBeans) {
//                Integer num = map.get(deptBean.getCode());
//                num = (num == null ? 0 : num) + 1;
//                map.put(deptBean.getCode(), num);
//                if (num > 1) {
//                    result.put(deptBean.getCode(), num);
//                }
//            }
//        }
//
//        if (result != null && result.size() > 0) {
//            throw new Exception("有重复code" + result.toString());
//        }
//    }

    /**
     * @param mainTree
     * @Description: 处理数据
     * @return: void
     */
    private List<TreeBean> saveToSso(Map<String, TreeBean> mainTree, DomainInfo domainInfo, String treeTypeId, List<TreeBean> beans, Map<TreeBean, String> result) throws Exception {
        Map<String, TreeBean> collect = new HashMap<>();
        if (null != beans && beans.size() > 0) {
            collect = beans.stream().collect(Collectors.toMap((TreeBean::getCode), (dept -> dept)));
        }
        //拉取的数据
        Collection<TreeBean> mainDept = mainTree.values();
        ArrayList<TreeBean> treeBeans = new ArrayList<>(mainDept);
//        //sso dept库的数据(通过domain 关联tenant查询)
//        Tenant tenant = tenantDao.findByDomainName(domainInfo.getDomainName());
//        if (null == tenant) {
//            throw new Exception("租户不存在");
//        }
//        //通过tenantId查询ssoApis库中的数据
//        List<Post> beans = postDao.findByTenantId(tenant.getId());
//        //将null赋为""
//        if (null != beans && beans.size() > 0) {
//            for (Post bean : beans) {
//                if (StringUtils.isBlank(bean.getParentCode())) {
//                    bean.setParentCode("");
//                }
//            }
//        }
//        //轮训比对标记(是否有主键id)
//        Map<DeptBean, String> result = new HashMap<>();

        //遍历拉取数据
        for (TreeBean treeBean : treeBeans) {
            if (!"builtin".equals(treeBean.getDataSource())) {
                //标记新增还是修改
                boolean flag = true;
                //赋值treeTypeId
                treeBean.setTreeType(treeTypeId);
                if (null != beans) {
                    //遍历数据库数据
                    for (TreeBean bean : beans) {
                        if (treeBean.getCode().equals(bean.getCode())) {


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
        }
        if (null != beans) {
            //查询数据库需要删除的数据
            for (TreeBean bean : beans) {

                if (!"builtin".equals(bean.getDataSource())||"pull".equals(bean.getDataSource())) {
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
