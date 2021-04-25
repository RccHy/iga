package com.qtgl.iga.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.dao.*;
import com.qtgl.iga.utils.DataBusUtil;
import com.qtgl.iga.utils.TreeEnum;
import com.qtgl.iga.utils.TreeUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * <FileName> NodeRulesCalculationServiceImpl
 * <Desc> 节点规则运算
 **/

@Service
//@Transactional
public class NodeRulesCalculationServiceImpl {
    @Autowired
    NodeRulesDao rulesDao;
    @Autowired
    UpstreamDao upstreamDao;
    @Autowired
    DataBusUtil dataBusUtil;
    @Autowired
    UpstreamTypeDao upstreamTypeDao;
    @Autowired
    NodeDao nodeDao;
    @Autowired
    UpstreamDeptDao upstreamDeptDao;
    @Autowired
    NodeRulesRangeDao rangeDao;

    public static Logger logger = LoggerFactory.getLogger(NodeRulesCalculationServiceImpl.class);
    //岗位重命名数据
    public static ConcurrentHashMap<String, String> postRename;
    //部门重命名数据
    public static ConcurrentHashMap<String, String> deptRename;


    /**
     * 重命名规则
     *
     * @param mergeDeptMap
     * @param nodeRulesRanges
     * @param childrenMap
     */
    public void renameRules(Map<String, TreeBean> mergeDeptMap, List<NodeRulesRange> nodeRulesRanges, Map<String, List<TreeBean>> childrenMap, String type) {
        for (NodeRulesRange nodeRulesRange : nodeRulesRanges) {
            // 重命名
            if (2 == nodeRulesRange.getType()) {
                String rename = nodeRulesRange.getRename();
                for (TreeBean treeBean : mergeDeptMap.values()) {
                    if (rename.contains("${code}")) {
                        String oldCode = treeBean.getCode();
                        String newCode = rename.replace("${code}", treeBean.getCode());
                        if ("post".equals(type)) {
                            postRename.put(oldCode, newCode);
                        }
                        if ("dept".equals(type)) {
                            deptRename.put(oldCode, newCode);
                        }
                        treeBean.setCode(newCode);
                        // 如果当前节点有子集，则同时修改子集的parentCode指向. 而原本就为"" 的顶级部门不应修改
                        if (childrenMap.containsKey(oldCode)) {
                            //String newParentCode = rename.replace("${code}", deptBean.getParentCode());
                            childrenMap.get(oldCode).forEach(deptBean1 -> {
                                deptBean1.setParentCode(newCode);
                            });
                        }
                    }
                    if (rename.contains("${name}")) {
                        String newName = rename.replace("${name}", treeBean.getName());
                        if ("post".equals(type)) {
                            postRename.put(treeBean.getName(), newName);
                        }
                        if ("dept".equals(type)) {
                            deptRename.put(treeBean.getName(), newName);
                        }
                        treeBean.setName(newName);
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
    public void excludeRules(Map<String, TreeBean> mergeDept, Map<String, List<TreeBean>> childrenMap, List<NodeRulesRange> nodeRulesRanges) throws Exception {
        for (NodeRulesRange nodeRulesRange : nodeRulesRanges) {
            //配置的节点
            if (null == nodeRulesRange.getNode()) {
                throw new Exception("配置节点为空");
            }
            String rangeNodeCode = nodeRulesRange.getNode();
            // 排除 规则
            if (1 == nodeRulesRange.getType()) {
                // 排除当前节点 以及 其子节点
                if (0 == nodeRulesRange.getRange()) {
                    if (!mergeDept.containsKey(rangeNodeCode)) {
                        throw new Exception("无法在挂载树中找到排除节点：" + rangeNodeCode);
                    }
                    mergeDept.remove(rangeNodeCode);
                    TreeUtil.removeTree(rangeNodeCode, childrenMap, mergeDept);
                }
                // 仅排除当前节点子节点
                if (1 == nodeRulesRange.getRange()) {
                    if (!mergeDept.containsKey(rangeNodeCode)) {
                        throw new Exception("无法在树中找到排除节点：" + rangeNodeCode);
                    }
                    TreeUtil.removeTree(rangeNodeCode, childrenMap, mergeDept);
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
    public void mountRules(String nodeCode, Map<String, TreeBean> mainTree, Map<String, TreeBean> upstreamMap,
                           Map<String, List<TreeBean>> childrenMap, List<NodeRulesRange> nodeRulesRanges,
                           Map<String, TreeBean> mergeDeptMap, String source) throws Exception {
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
                    TreeBean treeBean = upstreamMap.get(rangeNodeCode);
                    treeBean.setParentCode(nodeCode);
                    /*if (mainTree.containsKey(deptBean.getCode()) &&
                            (!mainTree.get(deptBean.getCode()).getParentCode().equals(deptBean.getParentCode()))) {
                        throw new Exception("挂载异常，节点中有同code不同parentCode节点：" + deptBean.getCode());
                    }*/
                    // mainTree.put(deptBean.getCode(), deptBean);
                    treeBean.setSource(source);
                    mergeDeptMap.put(treeBean.getCode(), treeBean);
                    //对该节点下所有子树同时进行挂载
                    mergeDeptTree(treeBean.getCode(), treeBean.getCode(), childrenMap, mergeDeptMap, source);
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
    public void mergeDeptTree(String code, String parentNode, Map<String, List<TreeBean>> childrenMap,
                              Map<String, TreeBean> mergeDeptMap, String source) throws Exception {
        List<TreeBean> children = childrenMap.get(code);
        if (null != children) {
            for (TreeBean treeJson : children) {
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

    /**
     * @param domain       租户
     * @param deptTreeType 组织机构树类型
     * @param nodeCode     节点
     * @param mainTree
     * @param status       状态(0:正式,1:编辑,2:历史)
     * @param type         来源类型 person,post,dept,occupy
     * @param operator     操作:task定时任务,system:系统操作
     * @Description: 规则运算
     * @return: java.util.Map<java.lang.String, com.qtgl.iga.bean.TreeBean>
     */
    public List<TreeBean> nodeRules(DomainInfo domain, String deptTreeType, String nodeCode, List<TreeBean> mainTree, Integer status, String type, String operator) throws Exception {
        //获取根节点的规则
        List<Node> nodes = nodeDao.getByCode(domain.getId(), deptTreeType, nodeCode, status, type);
        for (Node node : nodes) {
            if (null == node) {
                return mainTree;
            }
            String code = node.getNodeCode();
            logger.info("开始'{}'节点规则运算", code);
            //获取节点的[拉取] 规则，来获取部门树
            List<NodeRules> nodeRules = rulesDao.getByNodeAndType(node.getId(), 1, true, status);

            //将主树进行 分组
            Map<String, TreeBean> mainTreeMap = mainTree.stream().collect(Collectors.toMap(TreeBean::getCode, deptBean -> deptBean));

            final Collection<TreeBean> mainDept = mainTreeMap.values();
            final Map<String, List<TreeBean>> mainTreeChildren = TreeUtil.groupChildren(new ArrayList<>(mainDept));


            if (null != nodeRules && nodeRules.size() > 0) {
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
                    upstreamTree = dataBusUtil.getDataByBus(upstreamType, domain.getDomainName());
//                if (null == dataByBus || null == dataByBus.get("data")) {
//                    throw new Exception("数据获取失败");
//                }
//                if (upstreamType.getIsPage()) {
//                    //JSONArray dpetArray = JSONObject.parseObject(dataByBus.get("data").toString()).getJSONObject(TYPE).getJSONArray("edges");
//                    Map dataMap = (Map) dataByBus.get("data");
//                    Map deptMap = (Map) dataMap.get(TYPE);
//                    JSONArray deptArray = (JSONArray) JSONArray.toJSON(deptMap.get("edges"));
//                    for (Object deptOb : deptArray) {
//                        JSONObject nodeJson = (JSONObject) deptOb;
//                        upstreamTree.add(nodeJson.getJSONObject("node"));
//                    }
//                } else {
//                    upstreamTree = JSONObject.parseObject(dataByBus.get("data").toString()).getJSONArray(TYPE);
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
                    List<TreeBean> upstreamDept = new ArrayList<>();
                    //
                    for (Object o : upstreamTree) {
                        JSONObject dept = (JSONObject) o;
                        if (null == dept.getString(TreeEnum.PARENTCODE.getCode())) {
                            dept.put(TreeEnum.PARENTCODE.getCode(), "");
                        }
                        if (null == dept.getString(TreeEnum.CREATETIME.getCode())) {
                            dept.put(TreeEnum.CREATETIME.getCode(), timestamp);
                        }
                        if (null == dept.getString(TreeEnum.UPDATETIME.getCode())) {
                            dept.put(TreeEnum.UPDATETIME.getCode(), timestamp);
                        }
                        dept.put("upstreamTypeId", upstreamType.getId());
                        upstreamDept.add(dept.toJavaObject(TreeBean.class));

                    }
//                fileds.put(upstreamType,upstreamDept);
                    if ("task".equals(operator)) {
                        //   待优化
                        Integer flag = saveDataToDb(upstreamTree, upstreamType.getId());
                        if (flag <= 0) {
                            throw new Exception("数据插入 iga 失败");
                        }
                        logger.error("节点'{}'数据入库完成", code);
                    }
                    //对树 json 转为 map
                    Map<String, TreeBean> upstreamMap = TreeUtil.toMap(upstreamDept);

                    //对树进行 parent 分组
                    Map<String, List<TreeBean>> childrenMap = TreeUtil.groupChildren(upstreamDept);
                    //查询 树 运行  规则,
                    List<NodeRulesRange> nodeRulesRanges = rangeDao.getByRulesId(nodeRule.getId(), null);
                    Map<String, TreeBean> mergeDeptMap = new ConcurrentHashMap<>();
                    logger.error("节点'{}'开始运行挂载", code);
                    //获取并检测 需要挂载的树， add 进入 待合并的树集合 mergeDept
                    mountRules(nodeCode, mainTreeMap, upstreamMap, childrenMap, nodeRulesRanges, mergeDeptMap, upstream.getAppName() + "(" + upstream.getAppCode() + ")");
                    //在挂载基础上进行排除
                    excludeRules(mergeDeptMap, childrenMap, nodeRulesRanges);
                    logger.error("节点'{}'开始运行排除", code);
                    // 对最终要挂载的树进行重命名
                    renameRules(mergeDeptMap, nodeRulesRanges, childrenMap, type);
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
                                Map<String, TreeBean> mainTreeMap2 = new ConcurrentHashMap<>();
                                mainTreeMap2.putAll(mainTreeMap);
                                for (Map.Entry<String, TreeBean> deptEntry : mainTreeMap2.entrySet()) {
                                    String key = deptEntry.getKey();
                                    TreeBean value = deptEntry.getValue();
                                    if (mainTreeMap.containsKey(key) &&
                                            mergeDeptMap.get(key).getParentCode().equals(value.getParentCode())
                                    ) {
                                        mainTreeMap.remove(key);
                                    }
                                }
                            } else {
                                // 当前权重 小于 继承来源
                                Map<String, TreeBean> mergeDeptMap2 = new ConcurrentHashMap<>();
                                mergeDeptMap2.putAll(mergeDeptMap);
                                for (Map.Entry<String, TreeBean> deptEntry : mergeDeptMap2.entrySet()) {
                                    String key = deptEntry.getKey();
                                    TreeBean value = deptEntry.getValue();
                                    if (mainTreeMap.containsKey(key) && mainTreeMap.get(key).getParentCode().equals(value.getParentCode())) {
                                        mergeDeptMap.remove(key);
                                    }
                                }

                            }
                        }
                    } else {
                        // 完全没有继承
                        if (nodeRule.getSort() == 0) {
                            // 完全不继承 第一个数据源， 需处理掉 主树当前节点下所有的子集
                            TreeUtil.removeMainTree(nodeCode, mainTreeChildren, mainTreeMap);
                        } else {
                            //完全不继承 非一个数据源， 直接去重 向主树合并
                            Map<String, TreeBean> mergeDeptMap2 = new ConcurrentHashMap<>();
                            mergeDeptMap2.putAll(mergeDeptMap);
                            for (Map.Entry<String, TreeBean> deptEntry : mergeDeptMap2.entrySet()) {
                                String key = deptEntry.getKey();
                                TreeBean value = deptEntry.getValue();
                                if (mainTreeMap.containsKey(key) && mainTreeMap.get(key).getParentCode().equals(value.getParentCode())) {
                                    mergeDeptMap.remove(key);
                                }
                            }

                        }

                    }

                    if (null != mergeDeptMap) {
                        Collection<TreeBean> values = mergeDeptMap.values();

                        mainTree.addAll(new ArrayList<>(values));
                    }
                    // 将本次 add 进的 节点 进行 规则运算
                    for (Map.Entry<String, TreeBean> entry : mergeDeptMap.entrySet()) {
                        mainTree = nodeRules(domain, deptTreeType, entry.getValue().getCode(), mainTree, status, type, operator);
                    }

                    /*========================规则运算完成=============================*/
                }
            } else {
                TreeUtil.removeTree(code, mainTreeChildren, mainTreeMap);
                mainTree = new ArrayList<>(mainTreeMap.values());
            }
        }

        return mainTree;
    }

    public JSONArray judgeTime(JSONArray upstreamTree, LocalDateTime timestamp) {
        String str = upstreamTree.toString();
        boolean flag = str.contains("createTime");
        boolean upFlag = str.contains("updateTime");
        if (!flag) {

            List<TreeBean> mainList = JSON.parseArray(str, TreeBean.class);
            for (TreeBean treeBean : mainList) {
                treeBean.setCreateTime(timestamp);
                if (!upFlag) {
                    treeBean.setUpdateTime(timestamp);
                }
            }
            return JSONArray.parseArray(JSON.toJSONString(mainList));
        } else {
            return upstreamTree;
        }
    }

    public void circularData(Map<String, TreeBean> mergeDeptMap) throws Exception {
        Collection<TreeBean> values = mergeDeptMap.values();
        ArrayList<TreeBean> mainList = new ArrayList<>(values);
        for (TreeBean treeBean : mainList) {
            for (TreeBean bean : mainList) {
                if (treeBean.getCode().equals(bean.getParentCode()) && treeBean.getParentCode().equals(bean.getCode())) {
                    logger.error("节点循环依赖,请检查{}{}", treeBean, bean);
                    throw new Exception(bean.toString() + "与" + treeBean.toString() + "节点循环依赖,请检查");
                }
            }
        }

    }

    public void circularData(JSONArray mergeDeptMap) throws Exception {
        List<TreeBean> mainList = JSON.parseArray(mergeDeptMap.toString(), TreeBean.class);
        for (TreeBean treeBean : mainList) {
            for (TreeBean bean : mainList) {
                if (treeBean.getCode().equals(bean.getParentCode()) && treeBean.getParentCode().equals(bean.getCode())) {
                    logger.error("节点循环依赖,请检查{},{}", treeBean, bean);
                    throw new Exception(treeBean.toString() + "与" + bean.toString() + "节点循环依赖,请检查处理");
                }
            }
        }

    }


    public void judgeData(Map<String, TreeBean> map) throws Exception {
        Collection<TreeBean> values = map.values();
        ArrayList<TreeBean> mainList = new ArrayList<>(values);
        for (TreeBean treeBean : mainList) {
            if (StringUtils.isBlank(treeBean.getName()) || StringUtils.isBlank(treeBean.getCode())) {
                throw new Exception("含非法数据,请检查");
            }
        }
    }

    public void groupByCode(List<TreeBean> treeBeans) throws Exception {
        HashMap<String, Integer> map = new HashMap<>();
        HashMap<String, Integer> result = new HashMap<>();
        if (null != treeBeans && treeBeans.size() > 0) {
            for (TreeBean treeBean : treeBeans) {
                Integer num = map.get(treeBean.getCode());
                num = (num == null ? 0 : num) + 1;
                map.put(treeBean.getCode(), num);
                if (num > 1) {
                    result.put(treeBean.getName(), num);
                }
            }
        }

        if (result != null && result.size() > 0) {
            throw new Exception("有重复code" + result.toString());
        }
    }

    /**
     * @param upstreamTree
     * @param id
     * @Description: 将上游部门数据存入iga数据库
     * @return: java.lang.Integer
     */
    @Transactional
    Integer saveDataToDb(JSONArray upstreamTree, String id) {
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
}
