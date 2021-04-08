package com.qtgl.iga.service.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.DeptBean;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.dao.*;
import com.qtgl.iga.service.DeptService;
import com.qtgl.iga.utils.DataBusUtil;
import com.qtgl.iga.utils.TreeEnum;
import com.qtgl.iga.utils.TreeUtil;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
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
    NodeRulesDao rulesDao;
    @Autowired
    NodeRulesRangeDao rangeDao;
    @Autowired
    UpstreamTypeDao upstreamTypeDao;
    @Autowired
    UpstreamDao upstreamDao;
    @Autowired
    DataBusUtil dataBusUtil;
    @Autowired
    DeptTreeTypeDao deptTreeTypeDao;
    @Autowired
    UpstreamDeptDao upstreamDeptDao;
    @Autowired
    TenantDao tenantDao;

    @Value("${iga.hostname}")
    String hostname;


    @Override
    public List<DeptBean> findDept(Map<String, Object> arguments, DomainInfo domain) throws Exception {
        List<DeptBean> depts = this.buildDeptByDomain(domain);
        return depts;

//        Map<String, DeptBean> mainTreeMap = new ConcurrentHashMap<>();
//
//        nodeRules(domain, (String) arguments.get("treeType"), "", mainTreeMap);
//        Collection<DeptBean> mainDept = mainTreeMap.values();
//        ArrayList<DeptBean> mainList = new ArrayList<>(mainDept);
//
//        // 判断重复(code)
//        groupByCode(mainList);
//
//        //同步到sso
//        saveToSso(mainTreeMap, domain, (String) arguments.get("treeType"));
    }

    @Override
    public List<DeptBean> findDeptByDomainName(String domainName, String treeType,Integer delMark) {
        Tenant byDomainName = tenantDao.findByDomainName(domainName);
        List<DeptBean> byTenantId = deptDao.findByTenantId(byDomainName.getId(), treeType,delMark);

        return byTenantId;
    }


    @SneakyThrows
    @Override
    public List<DeptBean> buildDeptByDomain(DomainInfo domain) {
      /*  logger.info("hostname{}", hostname);
        //
        Map<String, List<DeptBean>> mainTreeMapGroupType = new ConcurrentHashMap<>();
        List<DomainInfo> domainInfos = domainInfoDao.findAll();

        for (DomainInfo domain : domainInfos) {*/


        // todo 获取sso整树
        //sso dept库的数据(通过domain 关联tenant查询)
        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        if (null == tenant) {
            throw new Exception("租户不存在");
        }
        //通过tenantId查询ssoApis库中的数据
        List<DeptBean> beans = deptDao.findByTenantId(tenant.getId(), null,null);
        if (null != beans && beans.size() > 0) {
            //将null赋为""
            for (DeptBean bean : beans) {
                if (null == bean.getParentCode()) {
                    bean.setParentCode("");
                }
            }
        }
        //轮训比对标记(是否有主键id)
        Map<DeptBean, String> result = new HashMap<>();
        // 获取租户下开启的部门类型
        List<DeptTreeType> deptTreeTypes = deptTreeTypeDao.findAll(new HashMap<>(), domain.getId());
        for (DeptTreeType deptType : deptTreeTypes) {
            Map<String, DeptBean> mainTreeMap = new ConcurrentHashMap<>();
            //todo id 改为code
            nodeRules(domain, deptType.getCode(), "", mainTreeMap,0);
            //  数据合法性
            Collection<DeptBean> mainDept = mainTreeMap.values();
            ArrayList<DeptBean> mainList = new ArrayList<>(mainDept);

            // 判断重复(code)
            groupByCode(mainList);

            //同步到sso

            beans = saveToSso(mainTreeMap, domain, deptType.getCode(), beans, result);
            //      mainTreeMapGroupType.put(deptType.getCode(), new ArrayList<>(mainDept));
        }
        //}
        groupByCode(beans);
        saveToSso(result, tenant.getId());

        return beans;
    }

    private void saveToSso(Map<DeptBean, String> result, String tenantId) throws Exception {
        //插入数据
        Map<String, List<Map.Entry<DeptBean, String>>> collect = result.entrySet().stream().collect(Collectors.groupingBy(c -> c.getValue()));
        List<Map.Entry<DeptBean, String>> insert = collect.get("insert");
        if (null != insert && insert.size() > 0) {
            ArrayList<DeptBean> list = new ArrayList<>();
            for (Map.Entry<DeptBean, String> key : insert) {

                list.add(key.getKey());
            }
            ArrayList<DeptBean> depts = deptDao.saveDept(list, tenantId);
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
            ArrayList<DeptBean> depts = deptDao.updateDept(list, tenantId);
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
            ArrayList<DeptBean> depts = deptDao.deleteDept(list);
            if (null != depts && depts.size() > 0) {
                logger.info("删除" + list.size() + "条数据{}", depts.toString());
            } else {
                throw new Exception("删除失败");
            }
        }
    }

    /**
     * @param domain
     * @param nodeCode
     * @throws Exception
     */
    public Map<String, DeptBean> nodeRules(DomainInfo domain, String deptTreeType, String nodeCode, Map<String, DeptBean> mainTree,Integer status) throws Exception {
        //获取根节点的规则
        List<Node> nodes = nodeDao.getByCode(domain.getId(), deptTreeType, nodeCode,status);
        for (Node node : nodes) {
            if (null == node) {
                return mainTree;
            }
            String code = node.getNodeCode();
            logger.info("开始'{}'节点规则运算", code);
            //获取节点的[拉取] 规则，来获取部门树
            List<NodeRules> nodeRules = rulesDao.getByNodeAndType(node.getId(), 1, true,status);

            //将主树进行 分组
            final Collection<DeptBean> mainDept = mainTree.values();
            final Map<String, List<DeptBean>> mainTreeChildren = TreeUtil.groupChildren(new ArrayList<>(mainDept));


            // 过滤出继承下来的NodeRules
            Map<String, NodeRules> inheritNodeRules = nodeRules.stream().filter(rules -> StringUtils.isNotEmpty(rules.getInheritId()))
                    .collect(Collectors.toMap(NodeRules::getId, v -> v));
            // 遍历结束后 要对数据确权
            for (NodeRules nodeRule : nodeRules) {
                logger.info("开始'{}'节点拉取规则，上游:{}", code, nodeRule.getUpstreamTypesId());
                // 每次循环都能拿到一个部门树，并计算出需要挂载的内容
                //
                if (null == nodeRule.getUpstreamTypesId()) {
                    logger.error("对应拉取节点'{}'无上有源类型数据", code);
                    throw new Exception("对应拉取节点'" + code + "'无上有源类型数据");
                }
                if (StringUtils.isNotEmpty(nodeRule.getInheritId())) {
                    logger.info("对应拉取节点'{}'继承自父级{}，跳过计算", code, nodeRule.getInheritId());
                    continue;
                }
                // 根据id 获取 UpstreamType
                UpstreamType upstreamType = upstreamTypeDao.findById(nodeRule.getUpstreamTypesId());
                //获取来源
                Upstream upstream = upstreamDao.findById(upstreamType.getUpstreamId());
                //获得部门树
                JSONArray upstreamTree = new JSONArray();
                //   请求graphql查询，获得部门树
                LocalDateTime timestamp = LocalDateTime.now();
                upstreamTree = dataBusUtil.getDataByBus(upstreamType);
//                if (null == dataByBus || null == dataByBus.get("data")) {
//                    throw new Exception("数据获取失败");
//                }
//                if (upstreamType.getIsPage()) {
//                    //JSONArray dpetArray = JSONObject.parseObject(dataByBus.get("data").toString()).getJSONObject("dept").getJSONArray("edges");
//                    Map dataMap = (Map) dataByBus.get("data");
//                    Map deptMap = (Map) dataMap.get("dept");
//                    JSONArray deptArray = (JSONArray) JSONArray.toJSON(deptMap.get("edges"));
//                    for (Object deptOb : deptArray) {
//                        JSONObject nodeJson = (JSONObject) deptOb;
//                        upstreamTree.add(nodeJson.getJSONObject("node"));
//                    }
//                } else {
//                    upstreamTree = JSONObject.parseObject(dataByBus.get("data").toString()).getJSONArray("dept");
//                }
                //验证树的合法性
                if (upstreamTree.size() <= 0) {
                    logger.info("节点'{}'数据源{}获取部门数据为空", code, upstreamType.getGraphqlUrl());
                    return mainTree;
                }
                logger.error("节点'{}'数据获取完成", code);
                //循环引用判断
                this.circularData(upstreamTree);

                //判断上游是否给出时间戳
                upstreamTree = this.judgeTime(upstreamTree, timestamp);


                /////////////////
                List<DeptBean> upstreamDept = new ArrayList<>();
                //
                for (Object o : upstreamTree) {
                    JSONObject dept = (JSONObject) o;
                    if (null == dept.getString(TreeEnum.PARENTCODE.getCode())) {
                        dept.put(TreeEnum.PARENTCODE.getCode(), "");
                    }
                    upstreamDept.add(dept.toJavaObject(DeptBean.class));

                }
                // todo 待优化
                Integer flag = saveDataToDb(upstreamTree, upstreamType.getId());
                if (!(flag > 0)) {
                    throw new Exception("数据插入 iga 失败");
                }
                logger.error("节点'{}'数据入库完成", code);
                //对树 json 转为 map
                Map<String, DeptBean> upstreamMap = TreeUtil.toMap(upstreamDept);

                //对树进行 parent 分组
                Map<String, List<DeptBean>> childrenMap = TreeUtil.groupChildren(upstreamDept);
                //查询 树 运行  规则,
                List<NodeRulesRange> nodeRulesRanges = rangeDao.getByRulesId(nodeRule.getId(),null);
                Map<String, DeptBean> mergeDeptMap = new ConcurrentHashMap<>();
                logger.error("节点'{}'开始运行挂载", code);
                //获取并检测 需要挂载的树， add 进入 待合并的树集合 mergeDept
                mountRules(nodeCode, mainTree, upstreamMap, childrenMap, nodeRulesRanges, mergeDeptMap, upstream.getAppCode());
                //在挂载基础上进行排除
                excludeRules(mergeDeptMap, childrenMap, nodeRulesRanges);
                logger.error("节点'{}'开始运行排除", code);
                // 对最终要挂载的树进行重命名
                renameRules(mergeDeptMap, nodeRulesRanges, childrenMap);
                logger.error("节点'{}'开始运行重命名", code);
                logger.info("节点'{}'的规则运算完成：{}", nodeCode, mergeDeptMap);


                logger.info("部门节点:{}的规则运算完成", nodeCode);

                //判空
                this.judgeData(mergeDeptMap);

                //循环引用判断
                this.circularData(mergeDeptMap);


            /*
                 和主树进行合并校验
                1: 确认权威源， 根据源的排序，在合并时候，判断是否要修改同级别，同code 的节点来源。
                2： 如果子节点不继承父级规则，则由父级规则之前合并进来的子树先进行删除
             */
                // 如果本次规则  权重 大于 继承 规则。  则 丢掉主树中 同code 同parent的节点
                if (inheritNodeRules.size() > 0) {
                    for (Map.Entry<String, NodeRules> nodeRulesEntry : inheritNodeRules.entrySet()) {
                        // 当前权重大于 继承来源
                        if (nodeRule.getSort() < nodeRulesEntry.getValue().getSort()) {
                            Map<String, DeptBean> mainTreeMap2 = new ConcurrentHashMap<>();
                            mainTreeMap2.putAll(mainTree);
                            for (Map.Entry<String, DeptBean> deptEntry : mainTreeMap2.entrySet()) {
                                String key = deptEntry.getKey();
                                DeptBean value = deptEntry.getValue();
                                if (mainTree.containsKey(key) &&
                                        mainTree.get(key).getParentCode().equals(value.getParentCode())
                                ) {
                                    mainTree.remove(key);
                                }
                            }
                        } else {
                            // 当前权重 小于 继承来源
                            Map<String, DeptBean> mergeDeptMap2 = new ConcurrentHashMap<>();
                            mergeDeptMap2.putAll(mergeDeptMap);
                            for (Map.Entry<String, DeptBean> deptEntry : mergeDeptMap2.entrySet()) {
                                String key = deptEntry.getKey();
                                DeptBean value = deptEntry.getValue();
                                if (mainTree.containsKey(key) && mainTree.get(key).getParentCode().equals(value.getParentCode())) {
                                    mergeDeptMap.remove(key);
                                }
                            }

                        }
                    }
                } else {
                    // 完全没有继承
                    if (nodeRule.getSort() == 0) {
                        // 完全不继承 第一个数据源， 需处理掉 主树当前节点下所有的子集
                        removeMainTree(nodeCode, mainTreeChildren, mainTree);
                    } else {
                        //完全不继承 非一个数据源， 直接去重 向主树合并
                        Map<String, DeptBean> mergeDeptMap2 = new ConcurrentHashMap<>();
                        mergeDeptMap2.putAll(mergeDeptMap);
                        for (Map.Entry<String, DeptBean> deptEntry : mergeDeptMap2.entrySet()) {
                            String key = deptEntry.getKey();
                            DeptBean value = deptEntry.getValue();
                            if (mainTree.containsKey(key) && mainTree.get(key).getParentCode().equals(value.getParentCode())) {
                                mergeDeptMap.remove(key);
                            }
                        }

                    }

                }


                mainTree.putAll(mergeDeptMap);
                // 将本次 add 进的 节点 进行 规则运算
                for (Map.Entry<String, DeptBean> entry : mergeDeptMap.entrySet()) {
                    nodeRules(domain, deptTreeType, entry.getKey(), mainTree,status);
                }

                /*========================规则运算完成=============================*/
            }
        }

        return mainTree;
    }

    private JSONArray judgeTime(JSONArray upstreamTree, LocalDateTime timestamp) {
        String str = upstreamTree.toString();
        boolean flag = str.contains("createTime");
        if (!flag) {

            List<DeptBean> mainList = JSON.parseArray(str, DeptBean.class);
            for (DeptBean deptBean : mainList) {
                deptBean.setCreateTime(timestamp);
            }
            return JSONArray.parseArray(JSON.toJSONString(mainList));
        } else {
            return upstreamTree;
        }
    }

    private void circularData(Map<String, DeptBean> mergeDeptMap) throws Exception {
        Collection<DeptBean> values = mergeDeptMap.values();
        ArrayList<DeptBean> mainList = new ArrayList<>(values);
        for (DeptBean deptBean : mainList) {
            for (DeptBean bean : mainList) {
                if (deptBean.getCode().equals(bean.getParentCode()) && deptBean.getParentCode().equals(bean.getCode())) {
                    logger.error("节点循环依赖,请检查{}{}", deptBean, bean);
                    throw new Exception(bean.toString() + "与" + deptBean.toString() + "节点循环依赖,请检查");
                }
            }
        }

    }

    private void circularData(JSONArray mergeDeptMap) throws Exception {
        List<DeptBean> mainList = JSON.parseArray(mergeDeptMap.toString(), DeptBean.class);
        for (DeptBean deptBean : mainList) {
            for (DeptBean bean : mainList) {
                if (deptBean.getCode().equals(bean.getParentCode()) && deptBean.getParentCode().equals(bean.getCode())) {
                    logger.error("节点循环依赖,请检查{},{}", deptBean, bean);
                    throw new Exception(deptBean.toString() + "与" + bean.toString() + "节点循环依赖,请检查处理");
                }
            }
        }

    }


    private void judgeData(Map<String, DeptBean> map) throws Exception {
        Collection<DeptBean> values = map.values();
        ArrayList<DeptBean> mainList = new ArrayList<>(values);
        for (DeptBean deptBean : mainList) {
            if (StringUtils.isBlank(deptBean.getName()) || StringUtils.isBlank(deptBean.getCode())) {
                throw new Exception("含非法数据,请检查");
            }
        }
    }

    private void groupByCode(List<DeptBean> deptBeans) throws Exception {
        HashMap<String, Integer> map = new HashMap<>();
        HashMap<String, Integer> result = new HashMap<>();
        if (null != deptBeans && deptBeans.size() > 0) {
            for (DeptBean deptBean : deptBeans) {
                Integer num = map.get(deptBean.getCode());
                num = (num == null ? 0 : num) + 1;
                map.put(deptBean.getCode(), num);
                if (num > 1) {
                    result.put(deptBean.getCode(), num);
                }
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
    private List<DeptBean> saveToSso(Map<String, DeptBean> mainTree, DomainInfo domainInfo, String treeTypeId, List<DeptBean> beans, Map<DeptBean, String> result) throws Exception {
        Map<String, DeptBean> collect = new HashMap<>();
        if (null != beans && beans.size() > 0) {
            collect = beans.stream().collect(Collectors.toMap((DeptBean::getCode), (dept -> dept)));
        }

        //拉取的数据
        Collection<DeptBean> mainDept = mainTree.values();
        ArrayList<DeptBean> deptBeans = new ArrayList<>(mainDept);


        //遍历拉取数据
        for (DeptBean deptBean : deptBeans) {
            //标记新增还是修改
            boolean flag = true;
            //赋值treeTypeId
            deptBean.setTreeType(treeTypeId);
            if (null != beans) {
                //遍历数据库数据
                for (DeptBean bean : beans) {
                    if (deptBean.getCode().equals(bean.getCode())) {
                        //
                        if (deptBean.getTreeType().equals(bean.getTreeType())) {
                            if (null != deptBean.getCreateTime()) {
                                //修改
                                if (null == bean.getCreateTime() || deptBean.getCreateTime().isAfter(bean.getCreateTime())) {
                                    //新来的数据更实时
                                    collect.put(deptBean.getCode(), deptBean);
                                    result.put(deptBean, "update");
                                } else {
                                    result.put(deptBean, "obsolete");
                                }
                            } else {
                                result.put(deptBean, "obsolete");
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
                    collect.put(deptBean.getCode(), deptBean);
                    result.put(deptBean, "insert");
                }
            } else {
                collect.put(deptBean.getCode(), deptBean);
                result.put(deptBean, "insert");
            }
        }
        if (null != beans) {
            //查询数据库需要删除的数据
            for (DeptBean bean : beans) {
                if (null != bean.getTreeType() && bean.getTreeType().equals(treeTypeId)) {
                    boolean flag = true;
                    for (DeptBean deptBean : result.keySet()) {
                        if (bean.getCode().equals(deptBean.getCode())) {
                            flag = false;
                            break;
                        }
                    }
                    if (flag) {
                        DeptBean deptBean = new DeptBean();
                        deptBean.setCode(bean.getCode());
                        collect.remove(deptBean.getCode());
                        result.put(deptBean, "delete");
                    }
                }
            }
        }


        Collection<DeptBean> values = collect.values();
        return new ArrayList<>(values);


    }

    /**
     * @param upstreamTree
     * @param id
     * @Description: 将上游部门数据存入iga数据库
     * @return: java.lang.Integer
     */
    private Integer saveDataToDb(JSONArray upstreamTree, String id) {
        //上游数据已有时间戳的情况
        UpstreamDept upstreamDept = new UpstreamDept();
        upstreamDept.setDept(upstreamTree.toJSONString());
        upstreamDept.setUpstreamTypeId(id);
        UpstreamDept upstreamDepts = null;
        //查询数据库是否有数据
        UpstreamDept upstreamDeptDb = upstreamDeptDao.findUpstreamDeptByUpstreamId(id);
        if (null == upstreamDeptDb) {
            upstreamDepts = upstreamDeptDao.saveUpstreamDepts(upstreamDept);
        } else {
            upstreamDept.setId(upstreamDeptDb.getId());
            upstreamDept.setCreateTime(new Timestamp(System.currentTimeMillis()));
            upstreamDepts = upstreamDeptDao.updateUpstreamDepts(upstreamDept);

        }

        return null == upstreamDepts ? 0 : 1;

    }


    /**
     * 重命名规则
     *
     * @param mergeDeptMap
     * @param nodeRulesRanges
     * @param childrenMap
     */
    private void renameRules(Map<String, DeptBean> mergeDeptMap, List<NodeRulesRange> nodeRulesRanges, Map<String, List<DeptBean>> childrenMap) {
        for (NodeRulesRange nodeRulesRange : nodeRulesRanges) {
            // 重命名
            if (2 == nodeRulesRange.getType()) {
                String rename = nodeRulesRange.getRename();
                for (DeptBean deptBean : mergeDeptMap.values()) {
                    if (rename.contains("${code}")) {
                        String newCode = rename.replace("${code}", deptBean.getCode());
                        deptBean.setCode(newCode);
                        // 如果当前节点有子集，则同时修改子集的parentCode指向
                        if (childrenMap.containsKey(deptBean.getParentCode())) {
                            String newParentCode = rename.replace("${code}", deptBean.getParentCode());
                            childrenMap.get(deptBean.getParentCode()).forEach(deptBean1 -> {
                                deptBean1.setParentCode(newParentCode);
                            });
                        }
                    }
                    if (rename.contains("${name}")) {
                        String newName = rename.replace("${name}", deptBean.getName());
                        deptBean.setName(newName);
                    }
                }
            }
        }
    }


    /**
     * 排除节点运算
     * 计算 所有排除规则后，将 源树 剔除节点后成一颗待合并待树，再将树进行循环加入主树
     *
     * @param childrenMap
     * @param nodeRulesRanges
     * @throws Exception
     */
    private void excludeRules(Map<String, DeptBean> mergeDept, Map<String, List<DeptBean>> childrenMap, List<NodeRulesRange> nodeRulesRanges) throws Exception {
        for (NodeRulesRange nodeRulesRange : nodeRulesRanges) {
            //配置的节点
            String rangeNodeCode = nodeRulesRange.getNode();
            // 排除 规则
            if (1 == nodeRulesRange.getType()) {
                // 排除当前节点 以及 其子节点
                if (0 == nodeRulesRange.getRange()) {
                    if (!mergeDept.containsKey(rangeNodeCode)) {
                        throw new Exception("无法在挂载树中找到排除节点：" + rangeNodeCode);
                    }
                    mergeDept.remove(rangeNodeCode);
                    removeTree(rangeNodeCode, childrenMap, mergeDept);
                }
                // 仅排除当前节点子节点
                if (1 == nodeRulesRange.getRange()) {
                    if (!mergeDept.containsKey(rangeNodeCode)) {
                        throw new Exception("无法在树中找到排除节点：" + rangeNodeCode);
                    }
                    removeTree(rangeNodeCode, childrenMap, mergeDept);
                }
                // todo  预留 支持通过表达式排除
                if (2 == nodeRulesRange.getRange()) {

                }
            }
        }

    }

    /**
     * 挂载规则
     *
     * @param nodeCode
     * @param mainTree
     * @param upstreamMap
     * @param childrenMap
     * @param nodeRulesRanges
     * @param mergeDeptMap
     * @throws Exception
     */
    private void mountRules(String nodeCode, Map<String, DeptBean> mainTree, Map<String, DeptBean> upstreamMap,
                            Map<String, List<DeptBean>> childrenMap, List<NodeRulesRange> nodeRulesRanges,
                            Map<String, DeptBean> mergeDeptMap, String source) throws Exception {
        for (NodeRulesRange nodeRulesRange : nodeRulesRanges) {
            //配置的节点
            String rangeNodeCode = nodeRulesRange.getNode();
            // 挂载规则
            if (0 == nodeRulesRange.getType()) {
                // 包含根节点一起挂载，修改根节点个parentCode
                if (0 == nodeRulesRange.getRange()) {
                    if (!upstreamMap.containsKey(rangeNodeCode)) {
                        throw new Exception("无法在树中找到挂载节点：" + rangeNodeCode);
                    }
                    DeptBean deptBean = upstreamMap.get(rangeNodeCode);
                    deptBean.setParentCode(nodeCode);
                    /*if (mainTree.containsKey(deptBean.getCode()) &&
                            (!mainTree.get(deptBean.getCode()).getParentCode().equals(deptBean.getParentCode()))) {
                        throw new Exception("挂载异常，节点中有同code不同parentCode节点：" + deptBean.getCode());
                    }*/
                    // mainTree.put(deptBean.getCode(), deptBean);
                    deptBean.setSource(source);
                    mergeDeptMap.put(deptBean.getCode(), deptBean);
                    //对该节点下所有子树同时进行挂载
                    mergeDeptTree(deptBean.getCode(), deptBean.getCode(), childrenMap, mergeDeptMap, source);
                }
                // 去除根节点开始挂载
                if (1 == nodeRulesRange.getRange()) {
                    if (!upstreamMap.containsKey(rangeNodeCode)) {
                        throw new Exception("无法在树中找到挂载节点：" + rangeNodeCode);
                    }
                    //对该节点下所有子树同时进行挂载
                    mergeDeptTree(nodeRulesRange.getNode(), nodeCode, childrenMap, mergeDeptMap, source);
                }
            }
        }
    }


    /**
     * @param treeArray 需要挂载的树
     * @param nodeCode  被挂载的节点
     * @return
     */
    public JSONObject checkTree(JSONArray treeArray, String nodeCode) {
        JSONObject error = new JSONObject();
        String msg = "";
        boolean key = true;
        Boolean root = true;
        for (int i = 0; i < treeArray.size(); i++) {
            JSONObject tree = treeArray.getJSONObject(i);
            //如果有节点结构不符合规则
            if (!tree.containsKey(TreeEnum.CODE.getCode()) || !tree.containsKey(TreeEnum.NAME.getCode()) || !tree.containsKey(TreeEnum.PARENTCODE.getCode())) {
                key = false;
                msg = tree.toJSONString();
                break;
            }
        }
        // 验证是否有根节点
        for (int i = 0; i < treeArray.size(); i++) {
            JSONObject tree = treeArray.getJSONObject(i);
            //如果有节点结构不符合规则
            if ("".equals(tree.getString(TreeEnum.PARENTCODE.getCode()))) {

            }
        }
        if (!key) {
            error.put("key error", msg);
            return error;
        }
        return null;

    }


    /**
     * 深度递归，合并所有子节点
     *
     * @param code        节点code
     * @param parentNode  在 去除根节点挂载情况下， parentNode要特殊指向定义规则的节点。t_mgr_node
     * @param childrenMap
     * @throws Exception
     */
    public void mergeDeptTree(String code, String parentNode, Map<String, List<DeptBean>> childrenMap,
                              Map<String, DeptBean> mergeDeptMap, String source) throws Exception {
        List<DeptBean> children = childrenMap.get(code);
        if (null != children) {
            for (DeptBean treeJson : children) {
                String treeCode = treeJson.getCode();
                String treeParentCode = treeJson.getParentCode();
               /* if (mainTree.containsKey(treeCode) &&
                        (!mainTree.get(treeCode).getParentCode()
                                .equals(treeParentCode))) {
                    throw new Exception("挂载异常，节点中已有同code不同parentCode节点：" + treeCode);
                }*/
                treeJson.setParentCode(parentNode);
                treeJson.setSource(source);
                mergeDeptMap.put(treeCode, treeJson);
                mergeDeptTree(treeCode, treeCode, childrenMap, mergeDeptMap, source);
            }
        }
    }


    public void removeTree(String code, Map<String, List<DeptBean>> childrenMap, Map<String, DeptBean> mergeDept) {
        List<DeptBean> children = childrenMap.get(code);
        if (null != children) {
            for (DeptBean deptJson : children) {
                mergeDept.remove(deptJson.getCode());
                removeTree(deptJson.getCode(), childrenMap, mergeDept);
            }
        }
    }


    public void removeMainTree(String code, Map<String, List<DeptBean>> childrenMap, Map<String, DeptBean> mainTree) {
        List<DeptBean> children = childrenMap.get(code);
        if (null != children) {
            for (DeptBean dept : children) {
                if (!dept.getDataSource().equals("builtin")) {
                    mainTree.remove(dept.getCode());
                    removeMainTree(dept.getCode(), childrenMap, mainTree);
                }
            }
        }
    }

}
