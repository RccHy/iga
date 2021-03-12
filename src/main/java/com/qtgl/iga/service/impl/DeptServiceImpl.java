package com.qtgl.iga.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.DeptBean;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.dao.*;
import com.qtgl.iga.service.DeptService;
import com.qtgl.iga.utils.TreeEnum;
import com.qtgl.iga.utils.TreeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class DeptServiceImpl implements DeptService {


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
        Map<String, DeptBean>  mainTreeMap = new ConcurrentHashMap<>();
        JSONObject root = new JSONObject();
        root.put("code", "root");
        root.put("name", "root");
        root.put("parentCode", "");

        List<DomainInfo> domainInfos = domainInfoDao.findAll();
        for (DomainInfo domain : domainInfos) {
            nodeRules(domain, null, mainTreeMap, 1);

        }
    }

    /**
     * @param domain
     * @param nodeCode
     * @throws Exception
     */
    private Map nodeRules(DomainInfo domain, String nodeCode, Map<String, DeptBean> mainTree, int a) throws Exception {
        String hrJson = "[\n" +
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

        //获取根节点的规则
        Node node = nodeDao.getByCode(domain.getId(), nodeCode);
        if (null == node) {
            return mainTree;
        }
        //获取节点的[拉取] 规则，来获取部门树
        List<NodeRules> nodeRules = rulesDao.getByNodeAndType(node.getId(), 1, true);
        for (NodeRules nodeRule : nodeRules) {
            //
            if (null == nodeRule.getUpstreamTypesId()) {
                throw new Exception("build dept tree error:node upstream type is null,id:" + nodeRule.getNodeId());
            }

            // 根据id 获取 UpstreamType
            UpstreamType upstreamType = upstreamTypeDao.findById(nodeRule.getUpstreamTypesId());
            // todo  请求graphql查询，获得部门树。目前写死变量
            // String graphqlUrl = upstreamType.getGraphqlUrl();


            //获得部门树
            JSONArray upstreamTree = new JSONArray();
            if (a == 1) {
                upstreamTree = JSONArray.parseArray(hrJson);
            }
            if (a == 2) {
                upstreamTree = JSONArray.parseArray(otherJson);
            }
            //对树进行 parent 分组
            Map<String, List<JSONObject>> upstreamTreeMap = TreeUtil.groupChildren(upstreamTree);
            //遍历树
            //验证树的合法性

            //

            List<String> treeRootCode = TreeUtil.getTreeRootCode(upstreamTree);
            // 树 运行  规则,
            List<NodeRulesRange> nodeRulesRanges = rangeDao.getByRulesId(nodeRule.getId());
            // 待合并的树
            JSONArray mergeTree = new JSONArray();
            for (NodeRulesRange nodeRulesRange : nodeRulesRanges) {
                //配置的节点
                String rangeNodeCode = nodeRulesRange.getNode();
                // 挂载规则
                if (0 == nodeRulesRange.getType()) {
                    // 包含根节点一起挂载，修改根节点个parentCode
                    if (0 == nodeRulesRange.getRange()) {
                        for (int i = 0; i < upstreamTree.size(); i++) {
                            JSONObject upstreamTreeNode = upstreamTree.getJSONObject(i);
                            // 找到tree中对应的节点，并修改其 父节点。并add 进待合并 mergeTree
                            if (rangeNodeCode.equals(upstreamTreeNode.getString(TreeEnum.CODE.getCode()))) {
                                upstreamTreeNode.put(TreeEnum.PARENTCODE.getCode(), nodeCode);
                                // 如果该树根节点只有1个，则可提前结束循环。并将整棵树add 进 待合并mergeTree
                                if (treeRootCode.size() == 1) {
                                    mergeTree.addAll(upstreamTree);
                                    break;
                                }
                                // 如果该树 根节点不止一个，则深度递归，将所有子节点add 进 待合并mergeTree
                                mergeTree.add(upstreamTreeNode);
                                buildMergeTree(upstreamTree,rangeNodeCode,mergeTree);
                            }
                        }
                    }
                    // 去除根节点开始挂载
                    if (1 == nodeRulesRange.getRange()) {
                        for (int i = 0; i < upstreamTree.size(); i++) {
                            JSONObject upstreamTreeNode = upstreamTree.getJSONObject(i);
                            if (rangeNodeCode.equals(upstreamTreeNode.getString(TreeEnum.PARENTCODE.getCode()))) {

                            }
                        }
                    }


                }

                //排除规则
                if (1 == nodeRulesRange.getType()) {
                    // 排除当前节点 以及 其子节点
                    if (0 == nodeRulesRange.getRange()) {
                        JSONArray jsonArray = upstreamTree;

                    }
                    // 仅排除当前节点子节点
                    if (1 == nodeRulesRange.getRange()) {

                    }
                }
            }

            /*========================规则运算完成=============================*/


            // 新增树的规则
            for (int i = 0; i < mergeTree.size(); i++) {
                JSONObject tree = mergeTree.getJSONObject(i);
                nodeRules(domain, tree.getString(TreeEnum.CODE.getCode()), mainTree, 2);
            }
        }
        return mainTree;
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
     * 深度递归 获得所有子节点
     * @param tree
     * @param nodeCode
     * @param mergeTree
     */
    public void buildMergeTree(JSONArray tree, String nodeCode, JSONArray mergeTree) {
        for (int i = 0; i < tree.size(); i++) {
            if(nodeCode.equals(tree.getJSONObject(i).getString(TreeEnum.PARENTCODE.getCode()))){
                mergeTree.add(tree.getJSONObject(i));
                buildMergeTree(tree,nodeCode,mergeTree);
            }
        }
    }

}
