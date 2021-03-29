package com.qtgl.iga.service.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.DeptBean;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.dao.*;
import com.qtgl.iga.service.DeptService;
import com.qtgl.iga.service.UserTypeService;
import com.qtgl.iga.utils.DataBusUtil;
import com.qtgl.iga.utils.TreeEnum;
import com.qtgl.iga.utils.TreeUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserTypeServiceImpl implements UserTypeService {


    public static Logger logger = LoggerFactory.getLogger(UserTypeServiceImpl.class);


   @Autowired
   DeptServiceImpl deptService;
   @Autowired
   UserTypeDao userTypeDao;
   @Autowired
   TenantDao tenantDao;

    @Value("${iga.hostname}")
    String hostname;


    @Override
    public List<DeptBean> findUserType( DomainInfo domain) throws Exception {
        //获取默认数据
        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        if (null == tenant) {
            throw new Exception("租户不存在");
        }
        List<DeptBean> rootBeans = userTypeDao.findRootData(tenant.getId());
        //转化为map
        Map<String, DeptBean> mainTreeMap = rootBeans.stream().collect(Collectors.toMap(DeptBean::getCode, deptBean -> deptBean));
        deptService.nodeRules(domain, "", "", mainTreeMap);
        Collection<DeptBean> mainDept = mainTreeMap.values();
        ArrayList<DeptBean> mainList = new ArrayList<>(mainDept);

        // 判断重复(code)
        groupByCode(mainList);

        //同步到sso
        saveToSso(mainTreeMap, domain,"");
        return new ArrayList<>(mainDept);
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
    private void saveToSso(Map<String, DeptBean> mainTree, DomainInfo domainInfo, String treeTypeId) throws Exception {
        //拉取的数据
        Collection<DeptBean> mainDept = mainTree.values();
        ArrayList<DeptBean> deptBeans = new ArrayList<>(mainDept);
        //sso dept库的数据(通过domain 关联tenant查询)
        Tenant tenant = tenantDao.findByDomainName(domainInfo.getDomainName());
        if (null == tenant) {
            throw new Exception("租户不存在");
        }
        //通过tenantId查询ssoApis库中的数据
        List<UserType> beans = userTypeDao.findByTenantId(tenant.getId());
        //将null赋为""
        if(null!=beans && beans.size()>0){
            for (UserType bean : beans) {
                if (StringUtils.isBlank(bean.getParentCode())) {
                    bean.setParentCode("");
                }
            }
        }
        //轮训比对标记(是否有主键id)
        Map<DeptBean, String> result = new HashMap<>();

        //遍历拉取数据
        for (DeptBean deptBean : deptBeans) {
            if(!"builtin".equals(deptBean.getDataSource())){
                //标记新增还是修改
                boolean flag = true;
                //赋值treeTypeId
                deptBean.setTreeType(treeTypeId);
                if (null != beans) {
                    //遍历数据库数据
                    for (UserType bean : beans) {
                        if (deptBean.getCode().equals(bean.getUserType())) {
                            if (null != deptBean.getCreateTime()) {
                                //修改
                                if (null == bean.getUpdateTime() || deptBean.getCreateTime().after(Timestamp.valueOf(bean.getUpdateTime()))) {
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
            for (UserType bean : beans) {
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

        Map<String, List<Map.Entry<DeptBean, String>>> collect = result.entrySet().stream().collect(Collectors.groupingBy(c -> c.getValue()));

        List<Map.Entry<DeptBean, String>> insert = collect.get("insert");
        //插入数据
        if (null != insert && insert.size() > 0) {
            ArrayList<DeptBean> list = new ArrayList<>();
            for (Map.Entry<DeptBean, String> key : insert) {
                list.add(key.getKey());
            }
            ArrayList<DeptBean> depts = userTypeDao.saveDept(list, tenant.getId());
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
            ArrayList<DeptBean> depts = userTypeDao.updateDept(list, tenant.getId());
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
            ArrayList<DeptBean> depts = userTypeDao.deleteDept(list);
            if (null != depts && depts.size() > 0) {
                logger.info("删除" + list.size() + "条数据{}", depts.toString());
            } else {
                throw new Exception("删除失败");
            }
        }


    }




}
