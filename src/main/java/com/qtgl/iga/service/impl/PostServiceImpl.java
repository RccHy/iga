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
        ArrayList<TreeBean> treeBeans = new ArrayList<>();


        List<TreeBean> mainTreeBeans = new ArrayList<>();

        for (TreeBean rootBean : rootBeans) {
            calculationService.nodeRules(domain, null, rootBean.getCode(), mainTreeBeans, 0, TYPE, "system");
        }
//
        // 判断重复(code)
        calculationService.groupByCode(mainTreeBeans);

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
        beans = dataProcessing(collect, domain, "", beans, result, treeBeans);
        if (null != beans) {
            beans.addAll(treeBeans);
        }

        // 判断重复(code)
        calculationService.groupByCode(beans);

        saveToSso(result, tenant.getId(), logBeans);

        return result;

    }

    @Override
    public List<TreeBean> findPosts(Map<String, Object> arguments, DomainInfo domain) throws Exception {
        Integer status = nodeService.judgeEdit(arguments, domain, TYPE);
        if (status == null) {
            return null;
        }
        //获取默认数据
        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        if (null == tenant) {
            throw new Exception("租户不存在");
        }
        //  置空 mainTree
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

        //sso dept库的数据(通过domain 关联tenant查询)
        if (null == tenant) {
            throw new Exception("租户不存在");
        }

        //轮训比对标记(是否有主键id)
        Map<TreeBean, String> result = new HashMap<>();
        ArrayList<TreeBean> treeBeans = new ArrayList<>();

        List<TreeBean> mainTreeBeans = new ArrayList<>();

        for (TreeBean rootBean : rootBeans) {
            calculationService.nodeRules(domain, null, rootBean.getCode(), mainTreeBeans, status, TYPE, "system");
        }
        System.out.println("==");

        // 判断重复(code)
        calculationService.groupByCode(mainTreeBeans);


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
        beans = dataProcessing(mainTreeMap, domain, "", beans, result, treeBeans);

        if (null != beans) {
            beans.addAll(treeBeans);
        }
        // 判断重复(code)
        calculationService.groupByCode(beans);

//        saveToSso(result, tenant.getId());

        return beans;
    }

    /**
     * @param result
     * @param tenantId
     * @Description: 增量插入sso数据库
     * @return: void
     */
    private void saveToSso(Map<TreeBean, String> result, String tenantId, List<TreeBean> logBeans) throws Exception {
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
                //统计修改字段
                TreeBean newTreeBean = key.getKey();
                TreeBean oldTreeBean = logCollect.get(newTreeBean.getCode());
                oldTreeBean.setSource(newTreeBean.getSource());

                //根据upstreamTypeId查询fileds
                boolean flag = false;

                try {
                    List<UpstreamTypeField> fields =  DataBusUtil.typeFields.get(newTreeBean.getUpstreamTypeId());
                    if (null != fields && fields.size() > 0) {
                        for (UpstreamTypeField field : fields) {
                            String sourceField = field.getSourceField();
                            Object newValue = ClassCompareUtil.getGetMethod(newTreeBean, sourceField);
                            Object oldValue = ClassCompareUtil.getGetMethod(oldTreeBean, sourceField);
                            if (null == oldValue && null == newValue) {
                                continue;
                            }
                            if (null != oldValue && oldValue.equals(newValue)) {
                                continue;
                            }
                            flag = true;
                            ClassCompareUtil.setValue(oldTreeBean, oldTreeBean.getClass(), sourceField, oldValue.getClass(), newValue);
                            logger.info(newTreeBean.getCode() + "字段" + sourceField + "-----------" + oldValue + "-------------" + newValue);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (oldTreeBean.getDelMark().equals(1)) {
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


        Integer flag = postDao.renewData(insertList, updateList, deleteList, tenantId);
        if (null != flag && flag > 0) {


            logger.info("post插入" + insertList.size() + "条数据  {}", System.currentTimeMillis());


            logger.info("post更新" + updateList.size() + "条数据  {}", System.currentTimeMillis());


            logger.info("post删除" + deleteList.size() + "条数据  {}", System.currentTimeMillis());
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
//                        collect.put(treeBean.getCode(), treeBean);
                        insert.add(treeBean);
                        result.put(treeBean, "insert");
                    }
                } else {
//                    collect.put(treeBean.getCode(), treeBean);
                    insert.add(treeBean);
                    result.put(treeBean, "insert");
                }
            }
        }
        if (null != beans) {
            //查询数据库需要删除的数据
            for (TreeBean bean : beans) {

                if (!"builtin".equals(bean.getDataSource()) || "PULL".equals(bean.getDataSource())) {
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
