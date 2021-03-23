package com.qtgl.iga.service.impl;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.DeptBean;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.dao.*;

import com.qtgl.iga.service.DeptService;
import com.qtgl.iga.utils.DataBusUtil;
import com.qtgl.iga.utils.TreeEnum;
import com.qtgl.iga.utils.TreeUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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


    @Override
    public List<DeptBean> findDept(Map<String, Object> arguments, DomainInfo domain) throws Exception {

        Map<String, DeptBean> mainTreeMap = new ConcurrentHashMap<>();

        nodeRules(domain, (String) arguments.get("treeType"), "", mainTreeMap);
        Collection<DeptBean> mainDept = mainTreeMap.values();

        return new ArrayList<>(mainDept);
    }

    public void saveDepts(){};


    @Override
    public void buildDept() throws Exception {

        //
        Map<String, List<DeptBean>> mainTreeMapGroupType = new ConcurrentHashMap<>();
        List<DomainInfo> domainInfos = domainInfoDao.findAll();

        for (DomainInfo domain : domainInfos) {
            // 获取租户下开启的部门类型
            List<DeptTreeType> deptTreeTypes = deptTreeTypeDao.findAll(new HashMap<>(), domain.getId());
            for (DeptTreeType deptType : deptTreeTypes) {
                Map<String, DeptBean> mainTreeMap = new ConcurrentHashMap<>();

                nodeRules(domain, deptType.getId(), "", mainTreeMap);
                Collection<DeptBean> mainDept = mainTreeMap.values();
                mainTreeMapGroupType.put(deptType.getCode(), new ArrayList<>(mainDept));
            }
        }
        System.out.println("1");

        // 将数据进行缓存


        // todo 异步处理  将 部门数据增量更新至sso-api库，并将数据
       // CompletableFuture<Void> future = CompletableFuture.runAsync(() -> saveDepts());




    }

    /**
     * @param domain
     * @param nodeCode
     * @throws Exception
     */
    private Map<String, DeptBean> nodeRules(DomainInfo domain, String deptTreeType, String nodeCode, Map<String, DeptBean> mainTree) throws Exception {
        //获取根节点的规则
        List<Node> nodes = nodeDao.getByCode(domain.getId(), deptTreeType, nodeCode);
        for (Node node : nodes) {
            if (null == node) {
                return mainTree;
            }
            //获取节点的[拉取] 规则，来获取部门树
            List<NodeRules> nodeRules = rulesDao.getByNodeAndType(node.getId(), 1, true);

            //将主树进行 分组
            final Collection<DeptBean> mainDept = mainTree.values();
            final Map<String, List<DeptBean>> mainTreeChildren = TreeUtil.groupChildren(new ArrayList<>(mainDept));


            // 过滤出继承下来的NodeRules
            Map<String, NodeRules> inheritNodeRules = nodeRules.stream().filter(rules -> StringUtils.isNotEmpty(rules.getInheritId()))
                    .collect(Collectors.toMap(k -> k.getId(), v -> v));
            // 遍历结束后 要对数据确权
            for (NodeRules nodeRule : nodeRules) {
                // 每次循环都能拿到一个部门树，并计算出需要挂载的内容
                //
                if (null == nodeRule.getUpstreamTypesId()) {
                    throw new Exception("build dept tree error:node upstream type is null,id:" + nodeRule.getNodeId());
                }

                // 根据id 获取 UpstreamType
                UpstreamType upstreamType = upstreamTypeDao.findById(nodeRule.getUpstreamTypesId());
                //获取来源
                Upstream upstream = upstreamDao.findById(upstreamType.getUpstreamId());

                if (StringUtils.isNotEmpty(nodeRule.getInheritId())) {
                    continue;
                }
                //获得部门树
                JSONArray upstreamTree = new JSONArray();
                //   请求graphql查询，获得部门树
                Map dataByBus = (Map) dataBusUtil.getDataByBus(upstreamType);
                if (upstreamType.getIsPage()) {
                    //JSONArray dpetArray = JSONObject.parseObject(dataByBus.get("data").toString()).getJSONObject("dept").getJSONArray("edges");
                    Map dataMap = (Map) dataByBus.get("data");
                    Map deptMap = (Map) dataMap.get("dept");
                    JSONArray dpetArray = (JSONArray) JSONArray.toJSON(deptMap.get("edges"));
                    for (Object deptOb : dpetArray) {
                        JSONObject nodeJson = (JSONObject) deptOb;
                        upstreamTree.add(nodeJson.getJSONObject("node"));
                    }
                } else {
                    upstreamTree = JSONObject.parseObject(dataByBus.get("data").toString()).getJSONArray("dept");
                }
                //验证树的合法性
                if (upstreamTree.size() <= 0) {
                    logger.info("数据源获取部门数据为空{}", upstreamType.toString());
                    return mainTree;
                }


                // todo  upstreamTree 不能有重复 code


                ///////////
                for (Object o : upstreamTree) {
                    JSONObject dept = (JSONObject) o;
                    if (null == dept.getString(TreeEnum.PARENTCODE.getCode()))
                        dept.put(TreeEnum.PARENTCODE.getCode(), "");

                }

                Integer flag = saveDataToDb(upstreamTree, upstreamType.getId());
                if (!(flag > 0)) {
                    throw new Exception("数据插入 iga 失败");
                }
                //对树 json 转为 map
                Map<String, JSONObject> upstreamMap = TreeUtil.toMap(upstreamTree);

                //对树进行 parent 分组
                Map<String, List<JSONObject>> childrenMap = TreeUtil.groupChildren(upstreamTree);
                //查询 树 运行  规则,
                List<NodeRulesRange> nodeRulesRanges = rangeDao.getByRulesId(nodeRule.getId());
                Map<String, DeptBean> mergeDeptMap = new ConcurrentHashMap<>();
                //获取并检测 需要挂载的树， add 进入 待合并的树集合 mergeDept
                mountRules(nodeCode, mainTree, upstreamMap, childrenMap, nodeRulesRanges, mergeDeptMap, upstream.getAppCode());
                //在挂载基础上进行排除
                excludeRules(mergeDeptMap, childrenMap, nodeRulesRanges);
                // 对最终要挂载的树进行重命名
                renameRules(mergeDeptMap, nodeRulesRanges, childrenMap);
                logger.info("部门节点:{}的规则运算完成", nodeCode);
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
                                if (mainTree.containsKey(key) &&
                                        (mainTree.get(key).getParentCode() == value.getParentCode() ||
                                                mainTree.get(key).getParentCode().equals(value.getParentCode()))
                                ) {
                                    mergeDeptMap.remove(key);
                                }
                            }

                        }
                    }
                } else {
                    // 完全没有继承
                    if (nodeRule.getSort() == 1) {
                        // 完全不继承 第一个数据源， 需处理掉 主树当前节点下所有的子集
                        removeMainTree(nodeCode, mainTreeChildren, mainTree);
                    } else {
                        //完全不继承 非一个数据源， 直接去重 向主树合并
                        Map<String, DeptBean> mergeDeptMap2 = new ConcurrentHashMap<>();
                        mergeDeptMap2.putAll(mergeDeptMap);
                        for (Map.Entry<String, DeptBean> deptEntry : mergeDeptMap2.entrySet()) {
                            String key = deptEntry.getKey();
                            DeptBean value = deptEntry.getValue();
                            if (mainTree.containsKey(key) &&
                                    (mainTree.get(key).getParentCode() == value.getParentCode() ||
                                            mainTree.get(key).getParentCode().equals(value.getParentCode()))
                            ) {
                                mergeDeptMap.remove(key);
                            }
                        }

                    }

                }


                mainTree.putAll(mergeDeptMap);
                // 将本次 add 进的 节点 进行 规则运算
                for (Map.Entry<String, DeptBean> entry : mergeDeptMap.entrySet()) {
                    nodeRules(domain, deptTreeType, entry.getKey(), mainTree);
                }

                /*========================规则运算完成=============================*/
            }
        }
        // todo 数据合法性
//        同步到sso
        saveToSso(mainTree);
        return mainTree;
    }

    /**
     * @Description: 处理数据并插入sso
     * @param mainTree
     * @return: void
     */
    private void saveToSso(Map<String, DeptBean> mainTree) {
        Collection<DeptBean> mainDept = mainTree.values();
        ArrayList<DeptBean> deptBeans = new ArrayList<>(mainDept);

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
            upstreamDepts = upstreamDeptDao.updateUpstreamDepts(upstreamDept);
            upstreamDepts.setId(upstreamDeptDb.getId());
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
    private void renameRules(Map<String, DeptBean> mergeDeptMap, List<NodeRulesRange> nodeRulesRanges, Map<String, List<JSONObject>> childrenMap) {
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
                            deptBean.setParentCode(newParentCode);
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
    private void excludeRules(Map<String, DeptBean> mergeDept, Map<String, List<JSONObject>> childrenMap, List<NodeRulesRange> nodeRulesRanges) throws Exception {
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
    private void mountRules(String nodeCode, Map<String, DeptBean> mainTree, Map<String, JSONObject> upstreamMap,
                            Map<String, List<JSONObject>> childrenMap, List<NodeRulesRange> nodeRulesRanges,
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
                    DeptBean deptBean = JSONObject.toJavaObject(upstreamMap.get(rangeNodeCode), DeptBean.class);
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
        Boolean key = true;
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
    public void mergeDeptTree(String code, String parentNode, Map<String, List<JSONObject>> childrenMap,
                              Map<String, DeptBean> mergeDeptMap, String source) throws Exception {
        List<JSONObject> children = childrenMap.get(code);
        if (null != children) {
            for (JSONObject treeJson : children) {
                String treeCode = treeJson.getString(TreeEnum.CODE.getCode());
                String treeParentCode = treeJson.getString(TreeEnum.CODE.getCode());
               /* if (mainTree.containsKey(treeCode) &&
                        (!mainTree.get(treeCode).getParentCode()
                                .equals(treeParentCode))) {
                    throw new Exception("挂载异常，节点中已有同code不同parentCode节点：" + treeCode);
                }*/
                DeptBean deptBean = JSONObject.toJavaObject(treeJson, DeptBean.class);
                deptBean.setParentCode(parentNode);
                deptBean.setSource(source);
                mergeDeptMap.put(treeCode, deptBean);
                mergeDeptTree(treeCode, treeCode, childrenMap, mergeDeptMap, source);
            }
        }
    }


    public void removeTree(String code, Map<String, List<JSONObject>> childrenMap, Map<String, DeptBean> mergeDept) {
        List<JSONObject> children = childrenMap.get(code);
        if (null != children) {
            for (JSONObject deptJson : children) {
                mergeDept.remove(deptJson.getString(TreeEnum.CODE.getCode()));
                removeTree(deptJson.getString(TreeEnum.CODE.getCode()), childrenMap, mergeDept);
            }
        }
    }


    public void removeMainTree(String code, Map<String, List<DeptBean>> childrenMap, Map<String, DeptBean> mainTree) {
        List<DeptBean> children = childrenMap.get(code);
        if (null != children) {
            for (DeptBean dept : children) {
                mainTree.remove(dept.getCode());
                removeMainTree(dept.getCode(), childrenMap, mainTree);
            }
        }
    }

}
