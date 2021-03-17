package com.qtgl.iga.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.DeptBean;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.dao.*;
import com.qtgl.iga.service.DeptService;
import com.qtgl.iga.utils.TreeEnum;
import com.qtgl.iga.utils.TreeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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


    @Override
    public List<Dept> getAllDepts() {
        return deptDao.getAllDepts();
    }


    @Override
    public void buildDept() throws Exception {

        //
        Map<String, DeptBean> mainTreeMap = new ConcurrentHashMap<>();
        JSONObject root = new JSONObject();
        root.put("code", "root");
        root.put("name", "root");
        root.put("parentCode", "");

        List<DomainInfo> domainInfos = domainInfoDao.findAll();
        for (DomainInfo domain : domainInfos) {
            nodeRules(domain, null, mainTreeMap);
        }
        System.out.println("1");
    }

    /**
     * @param domain
     * @param nodeCode
     * @throws Exception
     */
    private Map nodeRules(DomainInfo domain, String nodeCode, Map<String, DeptBean> mainTree) throws Exception {
       /* String hrJson = "[\n" +
                "{code:\"001\",name:\"一级部门1\",parentCode:\"\"},\n" +
                "{code:\"002\",name:\"一级部门2\",parentCode:\"\"},\n" +
                "{code:\"003\",name:\"一级部门3\",parentCode:\"\"},\n" +
                "{code:\"0011\",name:\"二级部门1-1\",parentCode:\"001\"},\n" +
                "{code:\"0012\",name:\"二级部门1-2\",parentCode:\"001\"},\n" +
                "{code:\"0013\",name:\"二级部门1-2\",parentCode:\"001\"},\n" +
                "{code:\"0021\",name:\"二级部门2-1\",parentCode:\"002\"},\n" +
                "{code:\"0022\",name:\"二级部门2-1\",parentCode:\"002\"}\n" +
                "{code:\"00221\",name:\"三级部门2-1-1\",parentCode:\"0021\"}\n" +
                "]";
        String eduJson = "[\n" +
                "{code:\"004\",name:\"一级部门4\",parentCode:\"\"},\n" +
                "{code:\"005\",name:\"一级部门5\",parentCode:\"\"},\n" +
                "{code:\"0051\",name:\"二级部门5-1\",parentCode:\"005\"},\n" +
                "{code:\"00511\",name:\"三级部门5-1-1\",parentCode:\"0051\"},\n" +
                "]";

        String otherJson = "[\n" +
                "{code:\"0014\",name:\"二级部门1-4\",parentCode:\"abc\"},\n" +
                "{code:\"00141\",name:\"二级部门1-4-1\",parentCode:\"0014\"},\n" +
                "]";
*/

        //获取根节点的规则
        List<Node> nodes = nodeDao.getByCode(domain.getId(), nodeCode);
        for (Node node : nodes) {
            if (null == node) {
                return mainTree;
            }
            //获取节点的[拉取] 规则，来获取部门树
            List<NodeRules> nodeRules = rulesDao.getByNodeAndType(node.getId(), 1, true);
            Map<String, DeptBean> mergeDeptMap = new ConcurrentHashMap<>();
            for (NodeRules nodeRule : nodeRules) {
                //是否继承自 父级,是则直接跳过
                if (nodeRule.getInherit()) {
                    continue;
                }
                //
                if (null == nodeRule.getUpstreamTypesId()) {
                    throw new Exception("build dept tree error:node upstream type is null,id:" + nodeRule.getNodeId());
                }

                // 根据id 获取 UpstreamType
                UpstreamType upstreamType = upstreamTypeDao.findById(nodeRule.getUpstreamTypesId());
                // todo  请求graphql查询，获得部门树。目前写死变量
                String graphqlUrl = upstreamType.getGraphqlUrl();


                //获得部门树
                JSONArray upstreamTree = new JSONArray();
                /*if (a == 1) {
                    upstreamTree = JSONArray.parseArray(hrJson);
                }
                if (a == 2) {
                    upstreamTree = JSONArray.parseArray(otherJson);
                }*/

                //验证树的合法性
                if (upstreamTree.size() <= 0) {
                    logger.info("数据源获取部门数据为空{}", upstreamType.toString());
                    return mainTree;
                }
                //对树 json 转为 map
                Map<String, JSONObject> upstreamMap = TreeUtil.toMap(upstreamTree);
                //对树进行 parent 分组
                Map<String, List<JSONObject>> childrenMap = TreeUtil.groupChildren(upstreamTree);
                //查询 树 运行  规则,
                List<NodeRulesRange> nodeRulesRanges = rangeDao.getByRulesId(nodeRule.getId());
                //获取并检测 需要挂载的树， add 进入 待合并的树集合 mergeDept
                mountRules(nodeCode, mainTree, upstreamMap, childrenMap, nodeRulesRanges, mergeDeptMap);
                //在挂载基础上进行排除
                excludeRules(mergeDeptMap, childrenMap, nodeRulesRanges);
                // 对最终要挂载的树进行重命名
                renameRules(mergeDeptMap, nodeRulesRanges, childrenMap);
                logger.info("部门节点:{}的规则运算完成",nodeCode);
                /*========================规则运算完成=============================*/
            }
            // 和主树进行合并
            // todo 校验是否有重复，并确认权威数据来源
            mainTree.putAll(mergeDeptMap);


            // 将本次 add 进的 节点 进行 规则运算
            for (Map.Entry<String, DeptBean> entry : mergeDeptMap.entrySet()) {
                nodeRules(domain, entry.getKey(), mainTree);
            }
        }
        return mainTree;
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
                            Map<String, DeptBean> mergeDeptMap) throws Exception {
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
                    if (mainTree.containsKey(deptBean.getCode()) &&
                            (!mainTree.get(deptBean.getCode()).getParentCode().equals(deptBean.getParentCode()))) {
                        throw new Exception("挂载异常，节点中有同code不同parentCode节点：" + deptBean.getCode());
                    }
                    // mainTree.put(deptBean.getCode(), deptBean);
                    mergeDeptMap.put(deptBean.getCode(), deptBean);
                    //对该节点下所有子树同时进行挂载
                    mergeDeptTree(deptBean.getCode(), deptBean.getCode(), childrenMap, mainTree, mergeDeptMap);
                }
                // 去除根节点开始挂载
                if (1 == nodeRulesRange.getRange()) {
                    if (!upstreamMap.containsKey(rangeNodeCode)) {
                        throw new Exception("无法在树中找到挂载节点：" + rangeNodeCode);
                    }
                    //对该节点下所有子树同时进行挂载
                    mergeDeptTree(nodeRulesRange.getNode(), nodeCode, childrenMap, mainTree, mergeDeptMap);
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
     * @param mainTree
     * @throws Exception
     */
    public void mergeDeptTree(String code, String parentNode, Map<String, List<JSONObject>> childrenMap, Map<String, DeptBean> mainTree,
                              Map<String, DeptBean> mergeDept) throws Exception {
        List<JSONObject> children = childrenMap.get(code);
        if (null != children) {
            for (JSONObject treeJson : children) {
                String treeCode = treeJson.getString(TreeEnum.CODE.getCode());
                String treeParentCode = treeJson.getString(TreeEnum.CODE.getCode());
                if (mainTree.containsKey(treeCode) &&
                        (!mainTree.get(treeCode).getParentCode()
                                .equals(treeParentCode))) {
                    throw new Exception("挂载异常，节点中已有同code不同parentCode节点：" + treeCode);
                }
                treeJson.put(TreeEnum.PARENTCODE.getCode(), parentNode);
                mergeDept.put(treeCode, JSONObject.toJavaObject(treeJson, DeptBean.class));
                mergeDeptTree(treeCode, treeCode, childrenMap, mainTree, mergeDept);
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

}
