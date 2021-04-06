package com.qtgl.iga.service.impl;


import com.qtgl.iga.bean.DeptBean;
import com.qtgl.iga.bo.Dept;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.Post;
import com.qtgl.iga.bo.Tenant;
import com.qtgl.iga.dao.PostDao;
import com.qtgl.iga.dao.TenantDao;
import com.qtgl.iga.service.PostService;
import javafx.geometry.Pos;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PostServiceImpl implements PostService {


    public static Logger logger = LoggerFactory.getLogger(PostServiceImpl.class);


    @Autowired
    DeptServiceImpl deptService;
    @Autowired
    PostDao postDao;
    @Autowired
    TenantDao tenantDao;

    @Value("${iga.hostname}")
    String hostname;


    @Override
    public List<DeptBean> findPosts(DomainInfo domain) throws Exception {
        //获取默认数据
        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        if (null == tenant) {
            throw new Exception("租户不存在");
        }
        //  置空mainTree
        List<DeptBean> rootBeans = postDao.findRootData(tenant.getId());
        for (DeptBean rootBean : rootBeans) {
            if (null == rootBean.getParentCode()) {
                rootBean.setParentCode("");
            }
        }

        //sso dept库的数据(通过domain 关联tenant查询)
        if (null == tenant) {
            throw new Exception("租户不存在");
        }
        //通过tenantId查询ssoApis库中的数据
        List<Post> beans = postDao.findByTenantId(tenant.getId());
        //将null赋为""
        if (null != beans && beans.size() > 0) {
            for (Post bean : beans) {
                if (StringUtils.isBlank(bean.getParentCode())) {
                    bean.setParentCode("");
                }
            }
        }
        //转化为map
        Map<String, DeptBean> rootMap = rootBeans.stream().collect(Collectors.toMap(DeptBean::getCode, deptBean -> deptBean));
        Map<String, DeptBean> mainTreeMap = rootBeans.stream().collect(Collectors.toMap(DeptBean::getCode, deptBean -> deptBean));
        // 将本次 add 进的 节点 进行 规则运算
        for (Map.Entry<String, DeptBean> entry : rootMap.entrySet()) {
            deptService.nodeRules(domain, null, entry.getKey(), mainTreeMap);
        }
        System.out.println("==");
        // Map<String, DeptBean> mainTreeMap = new HashMap<>();
        //deptService.nodeRules(domain, null, "", mainTreeMap);

        Collection<DeptBean> mainDept = mainTreeMap.values();
        ArrayList<DeptBean> mainList = new ArrayList<>(mainDept);

        // 判断重复(code)
        groupByCode(mainList);

        //同步到sso
        saveToSso(mainTreeMap, domain, "",beans);
        return new ArrayList<>(mainDept);
    }

    @Override
    public List<DeptBean> findDeptByDomainName(String domainName) {
//        获取tenantId
        Tenant byDomainName = tenantDao.findByDomainName(domainName);
        return postDao.findPostType(byDomainName.getId());

    }


    private void groupByCode(List<DeptBean> deptBeans) throws Exception {
        HashMap<String, Integer> map = new HashMap<>();
        HashMap<String, Integer> result = new HashMap<>();
        for (DeptBean deptBean : deptBeans) {
            Integer num = map.get(deptBean.getCode());
            num = (num == null ? 0 : num) + 1;
            map.put(deptBean.getCode(), num);
            if (num > 1) {
                result.put(deptBean.getCode(), num);
            }
        }

        if (result != null && result.size() > 0) {
            throw new Exception("有重复code" + result.toString());
        }
    }

    /**
     * @param mainTree
     * @Description: 处理数据并插入sso
     * @return: void
     */
    private void saveToSso(Map<String, DeptBean> mainTree, DomainInfo domainInfo, String treeTypeId,List<Post> beans) throws Exception {
        //拉取的数据
        Collection<DeptBean> mainDept = mainTree.values();
        ArrayList<DeptBean> deptBeans = new ArrayList<>(mainDept);
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
        //轮训比对标记(是否有主键id)
        Map<DeptBean, String> result = new HashMap<>();

        //遍历拉取数据
        for (DeptBean deptBean : deptBeans) {
            if (!"builtin".equals(deptBean.getDataSource())) {
                //标记新增还是修改
                boolean flag = true;
                //赋值treeTypeId
                deptBean.setTreeType(treeTypeId);
                if (null != beans) {
                    //遍历数据库数据
                    for (Post bean : beans) {
                        if (deptBean.getCode().equals(bean.getUserType())) {


                            if (null != deptBean.getCreateTime()) {
                                //修改
                                if (null == bean.getUpdateTime() || deptBean.getCreateTime().isAfter(bean.getUpdateTime())) {
                                    //新来的数据更实时
                                    result.put(deptBean, "update");
                                } else {
                                    result.put(deptBean, "obsolete");
                                }
                            } else {
                                result.put(deptBean, "obsolete");
                            }
                            flag = false;
                        }

                    }
                    //没有相等的应该是新增
                    if (flag) {
                        //新增
                        result.put(deptBean, "insert");
                    }
                } else {
                    result.put(deptBean, "insert");
                }
            }
        }
        if (null != beans) {
            //查询数据库需要删除的数据
            for (Post bean : beans) {

                if (!"builtin".equals(bean.getDataSource())) {
                    boolean flag = true;
                    for (DeptBean deptBean : result.keySet()) {
                        if (bean.getUserType().equals(deptBean.getCode())) {
                            flag = false;
                            break;
                        }
                    }
                    if (flag) {
                        DeptBean deptBean = new DeptBean();
                        deptBean.setCode(bean.getUserType());
                        result.put(deptBean, "delete");
                    }
                }
            }
        }

        Map<String, List<Map.Entry<DeptBean, String>>> collect = result.entrySet().stream().collect(Collectors.groupingBy(c -> c.getValue()));
        //  查询所有对比code
        List<Post> realBeans = postDao.findByTenantId(beans.get(0).getTenantId());
        Map<String, Post> collect1 = realBeans.stream().collect(Collectors.toMap(
                (Post::getUserType),
                (post -> post)));
        List<Map.Entry<DeptBean, String>> insert = collect.get("insert");
        //插入数据
        if (null != insert && insert.size() > 0) {
            ArrayList<DeptBean> list = new ArrayList<>();
            for (Map.Entry<DeptBean, String> key : insert) {
                Post post = collect1.get(key.getKey());
                if (null != post) {
                    throw new RuntimeException("code" + key.getKey() + "重复,插入失败");
                }
                list.add(key.getKey());
            }
            ArrayList<DeptBean> depts = postDao.saveDept(list, beans.get(0).getTenantId());
            if (null != depts && depts.size() > 0) {
                logger.info("插入" + list.size() + "条数据{}", depts.toString());
            } else {
                throw new Exception("插入失败");
            }
        }
        List<Map.Entry<DeptBean, String>> obsolete = collect.get("obsolete");
        if (null != obsolete && obsolete.size() > 0) {
            ArrayList<DeptBean> list = new ArrayList<>();
            for (Map.Entry<DeptBean, String> key : obsolete) {
                list.add(key.getKey());
            }
            logger.info("忽略" + list.size() + "条已过时数据{}", obsolete.toString());

        }
        List<Map.Entry<DeptBean, String>> update = collect.get("update");
        //修改数据
        if (null != update && update.size() > 0) {
            ArrayList<DeptBean> list = new ArrayList<>();
            for (Map.Entry<DeptBean, String> key : update) {
                list.add(key.getKey());
            }
            ArrayList<DeptBean> depts = postDao.updateDept(list, beans.get(0).getTenantId());
            if (null != depts && depts.size() > 0) {
                logger.info("更新" + list.size() + "条数据{}", depts.toString());
            } else {
                throw new Exception("更新失败");
            }
        }
        List<Map.Entry<DeptBean, String>> delete = collect.get("delete");
        //删除数据
        if (null != delete && delete.size() > 0) {
            ArrayList<DeptBean> list = new ArrayList<>();
            for (Map.Entry<DeptBean, String> key : delete) {
                list.add(key.getKey());
            }
            ArrayList<DeptBean> depts = postDao.deleteDept(list);
            if (null != depts && depts.size() > 0) {
                logger.info("删除" + list.size() + "条数据{}", depts.toString());
            } else {
                throw new Exception("删除失败");
            }
        }


    }


}
