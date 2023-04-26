package com.qtgl.iga.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.*;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.service.*;
import com.qtgl.iga.task.TaskConfig;
import com.qtgl.iga.utils.ClassCompareUtil;
import com.qtgl.iga.utils.DataBusUtil;
import com.qtgl.iga.utils.TreeUtil;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.enums.TreeEnum;
import com.qtgl.iga.utils.exception.CustomException;
import com.qtgl.iga.vo.NodeRulesVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <FileName> NodeRulesCalculationServiceImpl
 * <Desc> 节点规则运算
 **/

@Service
//@Transactional
@Slf4j
public class NodeRulesCalculationServiceImpl {
    @Resource
    NodeRulesService rulesService;
    @Resource
    UpstreamService upstreamService;
    @Resource
    DataBusUtil dataBusUtil;
    @Resource
    UpstreamTypeService upstreamTypeService;
    @Resource
    NodeService nodeService;
    @Resource
    DeptTreeTypeService deptTreeTypeService;
    @Resource
    MonitorRulesService monitorRulesService;
    @Resource
    DeptServiceImpl deptService;
    @Resource
    IncrementalTaskService incrementalTaskService;
    @Resource
    ShadowCopyService shadowCopyService;
    //@Resource
    //UpstreamDeptDao upstreamDeptDao;
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
    public void excludeRules(String nodeCode, Map<String, TreeBean> mergeDept, Map<String, List<TreeBean>> childrenMap, List<NodeRulesRange> nodeRulesRanges, DomainInfo domain, DeptTreeType treeType, List<TreeBean> mainTree) {
        for (NodeRulesRange nodeRulesRange : nodeRulesRanges) {
            //配置的节点
            if (null == nodeRulesRange.getNode()) {
                throw new CustomException(ResultCode.FAILED, "配置节点为空");
            }
            String rangeNodeCode = nodeRulesRange.getNode();
            // 排除 规则
            if (1 == nodeRulesRange.getType()) {
                if (rangeNodeCode.contains("=")) {
                    if ("*".equals((rangeNodeCode.substring(1)).trim())) {
                        mergeDept.clear();
                    } else if (Pattern.matches("=Reg\\(\".*\"\\)", rangeNodeCode)) {
                        //符合函数表达式的所有节点
                        for (TreeBean treeBean : new ArrayList<>(mergeDept.values())) {
                            String reg = rangeNodeCode.substring(rangeNodeCode.indexOf("(") + 2, rangeNodeCode.length() - 2);
                            if (Pattern.matches(reg, treeBean.getCode())) {
                                mergeDept.remove(treeBean.getCode());
                            }
                        }
                    } else {

//                        非正则(逻辑表达式)
                        for (TreeBean treeBean : new ArrayList<>(mergeDept.values())) {
                            try {
                                SimpleBindings bindings = new SimpleBindings();
                                bindings.put("$code", treeBean.getCode());
                                String reg = rangeNodeCode.substring(1);
                                ScriptEngineManager sem = new ScriptEngineManager();
                                ScriptEngine engine = sem.getEngineByName("js");
                                final Object eval = engine.eval(reg, bindings);
                                if (null != mergeDept.get(eval)) {
                                    mergeDept.remove(treeBean.getCode());
                                }

                            } catch (ScriptException e) {
                                logger.error(" {} 节点 {}   规则{} 中的 排除规则{}表达式非法,请检查 ",
                                        null == treeType ? "" : treeType.getName(), ("".equals(nodeCode) ? "根节点" : nodeCode), treeBean.getRuleId(), nodeRulesRange.getId());

                                ArrayList<ErrorData> list = new ArrayList<>();
                                list.add(new ErrorData((null == treeType ? "" : treeType.getId()), treeBean.getRuleId(), nodeCode, nodeRulesRange.getId()));

                                throw new CustomException(ResultCode.ILLEGAL_EXCLUSION, list, mainTree, null == treeType ? "" : treeType.getName(), "".equals(nodeCode) ? "根节点" : nodeCode, rangeNodeCode);
                            }
                        }

                    }

                } else {
                    if (null != nodeRulesRange.getRange()) {
                        if (!mergeDept.containsKey(rangeNodeCode)) {
                            logger.error(" 规则{} 中的  code:{} 无法找到排除节点 ", nodeRulesRange.getNodeRulesId(), rangeNodeCode);
                            ArrayList<ErrorData> list = new ArrayList<>();
                            list.add(new ErrorData((null == treeType ? "" : treeType.getId()), nodeRulesRange.getNodeRulesId(), nodeCode, nodeRulesRange.getId()));
                            throw new CustomException(ResultCode.NO_EXCLUSION, list, mainTree, nodeCode, rangeNodeCode);
                        }
                        // 排除当前节点 以及 其子节点
                        if (0 == nodeRulesRange.getRange()) {

                            mergeDept.remove(rangeNodeCode);
                            TreeUtil.removeTree(rangeNodeCode, childrenMap, mergeDept);
                        }
                        // 仅排除当前节点子节点
                        if (1 == nodeRulesRange.getRange()) {
                            TreeUtil.removeTree(rangeNodeCode, childrenMap, mergeDept);
                        }
                    } else {
                        logger.error(" 规则{} 中的  code:{} 排除规则为空 ", nodeRulesRange.getNodeRulesId(), rangeNodeCode);
                        ArrayList<ErrorData> list = new ArrayList<>();
                        list.add(new ErrorData((null == treeType ? "" : treeType.getId()), nodeRulesRange.getNodeRulesId(), nodeCode, nodeRulesRange.getId()));

                        throw new CustomException(ResultCode.NULL_EXCLUSION, list, mainTree, nodeCode, rangeNodeCode);
                    }
                }
//                //   预留 支持通过表达式排除
//                if (2 == nodeRulesRange.getRange()) {
//
//                }
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
     */
    public void mountRules(String nodeCode, List<TreeBean> mainTree, Map<String, TreeBean> upstreamMap,
                           Map<String, List<TreeBean>> childrenMap, List<NodeRulesRange> nodeRulesRanges,
                           Map<String, TreeBean> mergeDeptMap, String source, DeptTreeType treeType) {
        for (NodeRulesRange nodeRulesRange : nodeRulesRanges) {
            //配置的节点
            String rangeNodeCode = nodeRulesRange.getNode();
            // 挂载规则
            if (0 == nodeRulesRange.getType()) {
                //表达式规则
                if (rangeNodeCode.contains("=")) {
                    if ("*".equals((rangeNodeCode.substring(1)).trim())) {
                        //全部节点
                        mergeDeptMap.putAll(upstreamMap);
                    } else if (Pattern.matches("=Reg\\(\".*\"\\)", rangeNodeCode)) {

                        //符合函数表达式的所有节点
                        for (TreeBean treeBean : new ArrayList<>(upstreamMap.values())) {
                            String reg = rangeNodeCode.substring(rangeNodeCode.indexOf("(") + 2, rangeNodeCode.length() - 2);
                            //rangeNodeCode.indexOf("\"")
                            if (Pattern.matches(reg, treeBean.getCode())) {
                                mergeDeptMap.put(treeBean.getCode(), treeBean);
                            }
                        }
                    } else {
//                        非正则(逻辑表达式)
                        for (TreeBean treeBean : new ArrayList<>(upstreamMap.values())) {
                            try {
                                SimpleBindings bindings = new SimpleBindings();
                                bindings.put("$code", treeBean.getCode());
                                String reg = rangeNodeCode.substring(1);
                                ScriptEngineManager sem = new ScriptEngineManager();
                                ScriptEngine engine = sem.getEngineByName("js");
                                final Object eval = engine.eval(reg, bindings);
                                if (null != upstreamMap.get(eval)) {
                                    mergeDeptMap.put(treeBean.getCode(), upstreamMap.get(eval));
                                }

                            } catch (ScriptException e) {
                                logger.error(" {} 节点 {}   规则{} 中的 挂载规则{}表达式非法,请检查 ",
                                        null == treeType ? "" : treeType.getName(), ("".equals(treeBean.getCode()) ? "根节点" : treeBean.getCode()), treeBean.getRuleId(), nodeRulesRange.getId());

                                ArrayList<ErrorData> list = new ArrayList<>();
                                list.add(new ErrorData((null == treeType ? "" : treeType.getId()), treeBean.getRuleId(), nodeCode, nodeRulesRange.getId()));

                                throw new CustomException(ResultCode.ILLEGAL_MOUNT, list, mainTree, null == treeType ? "" : treeType.getName(), "".equals(nodeCode) ? "根节点" : nodeCode, rangeNodeCode);
                            }
                        }

                    }

                } else {
                    if (!upstreamMap.containsKey(rangeNodeCode)) {
                        logger.error(" 节点 {}   规则{} 中的  code:{} 无法找到挂载节点 ", nodeCode, nodeRulesRange.getNodeRulesId(), rangeNodeCode);

                        ArrayList<ErrorData> list = new ArrayList<>();
                        list.add(new ErrorData((null == treeType ? "" : treeType.getId()), nodeRulesRange.getNodeRulesId(), nodeCode));
                        logger.error(" 节点 {}   规则{} 中的  code:{} 挂载规则非法 ", nodeCode, nodeRulesRange.getNodeRulesId(), rangeNodeCode);
                        throw new CustomException(ResultCode.UNMOUNT, list, mainTree, nodeCode, rangeNodeCode);
                    }
                    // 包含根节点一起挂载，修改根节点个parentCode
                    if (null != nodeRulesRange.getRange()) {
                        if (0 == nodeRulesRange.getRange()) {
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

                            //对该节点下所有子树同时进行挂载
                            mergeDeptTree(nodeRulesRange.getNode(), nodeCode, childrenMap, mergeDeptMap, source);
                        }
                    } else {
                        ArrayList<ErrorData> list = new ArrayList<>();
                        list.add(new ErrorData((null == treeType ? "" : treeType.getId()), nodeRulesRange.getNodeRulesId(), nodeCode));
                        logger.error(" 节点 {}   规则{} 中的  code:{} 挂载规则非法 ", nodeCode, nodeRulesRange.getNodeRulesId(), rangeNodeCode);
                        throw new CustomException(ResultCode.NO_MOUNT, list, mainTree, nodeCode, rangeNodeCode);

                    }
                }
            }
        }
    }


    /**
     * 监控规则
     * 如果删除的数据 > 监控规则限制 则触发一下检测逻辑：
     * A：本次异常原因和上次已忽略的异常原因相同。则不触发异常，继续更新数据
     * B：如上次无异常，则记录下异常信息
     *
     * @param domain      租户信息
     * @param lastTaskLog 上次日志信息
     * @param count       总数
     * @param delete      本次删除数据
     * @throws Exception
     */
    public void monitorRules(DomainInfo domain, TaskLog lastTaskLog, Integer count, List<Map.Entry<TreeBean, String>> delete, List<Map.Entry<TreeBean, String>> invalid, String type) throws Exception {
        List<Map.Entry<TreeBean, String>> all = new ArrayList<>();
        if (!CollectionUtils.isEmpty(delete)) {
            all.addAll(delete);
        }
        if (!CollectionUtils.isEmpty(invalid)) {
            all.addAll(invalid);
        }

        if (null != all && all.size() > 0) {
            // 获取 监控规则
            final List<MonitorRules> deptMonitorRules = monitorRulesService.findAll(domain.getId(), type);
            if ("dept".equals(type)) {
                type = "组织机构";
            } else {
                type = "岗位";
            }
            for (MonitorRules deptMonitorRule : deptMonitorRules) {
                SimpleBindings bindings = new SimpleBindings();
                bindings.put("$count", count);
                bindings.put("$result", all.size());
                String reg = deptMonitorRule.getRules();
                ScriptEngineManager sem = new ScriptEngineManager();
                ScriptEngine engine = sem.getEngineByName("js");
                Object eval = engine.eval(reg, bindings);
                if ((Boolean) eval) {
                    boolean flag = true;
                    // 如果上次日志状态 是【忽略】，则判断数据是否相同原因相同，相同则进行忽略
                    if (null != lastTaskLog && null != lastTaskLog.getStatus() && lastTaskLog.getStatus().equals("ignore")) {
                        JSONArray objects = JSONArray.parseArray(lastTaskLog.getData());
                        Map<String, JSONObject> map = TreeUtil.toMap(objects);
                        if (null != map) {
                            for (Map.Entry<TreeBean, String> treeBean : all) {
                                if (!map.containsKey(treeBean.getKey().getCode())) {
                                    flag = true;
                                    break;
                                }
                                flag = false;
                            }
                        }
                    }
                    if (flag) {
                        logger.error("{}删除数量{},超出监控设定", type, all.size());
                        List<TreeBean> treeBeanList = all.stream().map(Map.Entry::getKey).collect(Collectors.toList());
                        TaskConfig.errorData.put(domain.getId(), JSON.toJSONString(JSON.toJSON(treeBeanList)));
                        throw new CustomException(ResultCode.MONITOR_ERROR, null, treeBeanList, type, all.size() + "");
                    }
                }
            }
        }
    }


    /**
     * 人员身份 监控规则
     *
     * @param domain
     * @param lastTaskLog
     * @param count
     * @param delete
     * @throws Exception
     */
    public void monitorRules(DomainInfo domain, TaskLog lastTaskLog, Integer count, List delete, List invalid) throws Exception {
        List all = new ArrayList<>();
        if (!CollectionUtils.isEmpty(delete)) {
            all.addAll(delete);
        }
        if (!CollectionUtils.isEmpty(invalid)) {
            all.addAll(invalid);
        }
        if (null != all && all.size() > 0) {
            String type = "occupy";
            if (all.get(0).getClass().getSimpleName().equals("Person")) {
                type = "person";
            }
            // 获取 监控规则
            final List<MonitorRules> deptMonitorRules = monitorRulesService.findAll(domain.getId(), type);
            for (MonitorRules deptMonitorRule : deptMonitorRules) {
                SimpleBindings bindings = new SimpleBindings();
                bindings.put("$count", count);
                bindings.put("$result", all.size());
                String reg = deptMonitorRule.getRules();
                ScriptEngineManager sem = new ScriptEngineManager();
                ScriptEngine engine = sem.getEngineByName("js");
                Object eval = engine.eval(reg, bindings);
                if ((Boolean) eval) {
                    boolean flag = true;

                    // 如果上次日志状态 是【忽略】，则判断数据是否相同原因相同，相同则进行忽略
                    if (null != lastTaskLog && null != lastTaskLog.getStatus() && lastTaskLog.getStatus().equals("ignore")) {
                        JSONArray objects = JSONArray.parseArray(lastTaskLog.getData());
                        if (all.get(0).getClass().getSimpleName().equals("Person")) {
                            //type = "人员";
                            List<JSONObject> personList = objects.toJavaList(JSONObject.class);
                            Map<String, JSONObject> map = personList.stream().collect(Collectors.toMap(
                                    (code -> code.getString("openId")),
                                    (value -> value)));

                            for (Object t : all) {
                                Person person = (Person) t;
                                if (!map.containsKey(person.getOpenId())) {
                                    flag = true;
                                    break;
                                }
                                flag = false;
                            }
                        } else {
                            //type = "人员身份";
                            List<JSONObject> personList = objects.toJavaList(JSONObject.class);
                            Map<String, JSONObject> map = personList.stream().collect(Collectors.toMap(
                                    (code -> code.getString("personId") + ":" + code.getString("postCode") + ":" + code.getString("deptCode")),
                                    (value -> value)));

                            for (Object t : all) {
                                OccupyDto occupyDto = (OccupyDto) t;
                                if (!map.containsKey(occupyDto.getPersonId() + ":" + occupyDto.getPostCode() + ":" + occupyDto.getDeptCode())) {
                                    flag = true;
                                    break;
                                }
                                flag = false;
                            }
                        }
                    }
                    if (flag) {
                        logger.error("{}删除数量{},超出监控设定", type.equals("person") ? "人员" : "人员身份", all.size());
                        TaskConfig.errorData.put(domain.getId(), JSON.toJSONString(JSON.toJSON(all)));
                        throw new CustomException(ResultCode.MONITOR_ERROR, null, all, type.equals("person") ? "人员" : "人员身份", all.size() + "");

                    }
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
            if (!tree.containsKey(TreeEnum.CODE.getCode()) || !tree.containsKey(TreeEnum.NAME.getCode()) || !tree.containsKey(TreeEnum.PARENT_CODE.getCode())) {
                key = false;
                msg = tree.toJSONString();
                break;
            }
        }
        // 验证是否有根节点
        for (int i = 0; i < treeArray.size(); i++) {
            JSONObject tree = treeArray.getJSONObject(i);
            //如果有节点结构不符合规则
            if ("".equals(tree.getString(TreeEnum.PARENT_CODE.getCode()))) {

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
                              Map<String, TreeBean> mergeDeptMap, String source) {
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
     * @param domain          租户
     * @param treeType        组织机构树类型
     * @param nodeCode        节点
     * @param mainTree
     * @param status          状态(0:正式,1:编辑,2:历史)
     * @param type            来源类型 person,post,dept,occupy
     * @param dynamicCodes
     * @param ssoBeansMap     sso数据
     * @param dynamicAttrs    sso 扩展字段定义字段
     * @param valueMap        sso扩展字段值
     * @param valueUpdate     扩展字段修改结果集
     * @param valueInsert     扩展字段新增结果集
     * @param upstreamHashMap 需要忽略的权威源
     * @param result          最终结果集
     * @param nodesMap        node规则
     * @param currentTask     当前同步任务
     * @return
     * @throws Exception
     * @Description: 规则运算
     */
    public List<TreeBean> nodeRules(DomainInfo domain, DeptTreeType treeType, String nodeCode, List<TreeBean> mainTree, Integer status, String type, List<String> dynamicCodes, Map<String, TreeBean> ssoBeansMap,
                                    List<DynamicAttr> dynamicAttrs, Map<String, List<DynamicValue>> valueMap, List<DynamicValue> valueUpdate, List<DynamicValue> valueInsert, Map<String, UpstreamDto> upstreamHashMap,
                                    Map<TreeBean, String> result, Map<String, List<NodeDto>> nodesMap, TaskLog currentTask) throws Exception {
        //获取根节点的规则
        //List<Node> nodes = nodeDao.getByCode(domain.getId(), treeType, nodeCode, status, type);
        //获取组织机构信息
        //DeptTreeType treeType = deptTreeTypeDao.findByCode(deptTreeType, domain.getId());
        List<NodeDto> nodes;
        if (!CollectionUtils.isEmpty(nodesMap) && nodesMap.containsKey(nodeCode)) {
            nodes = nodesMap.get(nodeCode);
        } else {
            return mainTree;
        }
        if (!CollectionUtils.isEmpty(nodes)) {
            for (NodeDto node : nodes) {
                if (null == node) {
                    return mainTree;
                }
                Map<String, TreeBean> mergeDeptMap = null;
                String code = node.getNodeCode();
                logger.info("开始'{}'节点规则运算", code);
                //获取节点的[拉取] 规则，来获取部门树
                //List<NodeRules> nodeRules = rulesDao.getByNodeAndType(node.getId(), 1, null, status);
                List<NodeRulesVo> nodeRules = node.getNodeRules();

                //将主树进行 分组
                Map<String, TreeBean> mainTreeMap = mainTree.stream().collect(Collectors.toMap(TreeBean::getCode, deptBean -> deptBean));

                Collection<TreeBean> mainDept = mainTreeMap.values();
                Map<String, List<TreeBean>> mainTreeChildren = TreeUtil.groupChildren(new ArrayList<>(mainDept));

                if (null != nodeRules && nodeRules.size() > 0) {
                    // 过滤出继承下来的NodeRules
                    Map<String, NodeRules> inheritNodeRules = nodeRules.stream().filter(rules -> StringUtils.isNotEmpty(rules.getInheritId()))
                            .collect(Collectors.toMap(NodeRules::getId, v -> v));
                    // 遍历结束后 要对数据确权
                    for (NodeRulesVo nodeRule : nodeRules) {
                        if (1 != nodeRule.getType()) {
                            continue;
                        }
                        if (0 != nodeRule.getRunningStatus()) {
                            //todo 忽略提示
                            log.info("当前规则被忽略,跳过执行");
                            continue;
                        }
                        //是否有规则过滤非继承数据打标识
                        if (null != mainTreeMap && StringUtils.isBlank(nodeRule.getInheritId())) {
                            TreeBean treeBean = mainTreeMap.get(code);
                            if (null != treeBean) {
                                treeBean.setIsRuled(true);
                            }
                        }
                        logger.info("开始'{}'节点拉取规则，上游:{}", code, nodeRule.getUpstreamTypesId());
                        // 每次循环都能拿到一个部门树，并计算出需要挂载的内容
                        if (null == nodeRule.getUpstreamTypesId()) {
                            logger.error("对应拉取节点'{}'无权威源类型数据", code);
                            throw new CustomException(ResultCode.NO_UPSTREAM_TYPE, null, null, "", code);
                        }
                        if (StringUtils.isNotEmpty(nodeRule.getInheritId())) {
                            logger.info("对应拉取节点'{}'继承自父级{}，跳过计算", code, nodeRule.getInheritId());
                            continue;
                        }
                        // 根据id 获取 UpstreamType

                        //todo 不启用 不报错
                        UpstreamType upstreamType = upstreamTypeService.findById(nodeRule.getUpstreamTypesId());
                        if (null == upstreamType) {
                            logger.error("对应拉取节点规则'{}'无有效权威源类型数据", code);
                            throw new CustomException(ResultCode.NO_UPSTREAM_TYPE, null, null, "", code);
                        }
                        //获取来源
                        Upstream upstream = upstreamService.findById(upstreamType.getUpstreamId());
                        if (null == upstream) {
                            logger.error("对应拉取节点规则'{}'无权威源数据", code);
                            throw new CustomException(ResultCode.NO_UPSTREAM, null, null, "", code);
                        }
                        //获得部门树
                        JSONArray upstreamTree = new JSONArray();
                        //   请求graphql查询，获得部门树
                        LocalDateTime timestamp = LocalDateTime.now();
                        try {
                            upstreamTree = dataBusUtil.getDataByBus(upstreamType, domain.getDomainName());
                        } catch (CustomException e) {
                            //e.setData(mainTree);
                            //if (new Long("1085").equals(e.getCode())) {
                            //    throw new CustomException(ResultCode.INVOKE_URL_ERROR, "请求资源地址失败,请检查权威源:" + upstream.getAppName() + "(" + upstream.getAppCode() + ")" + "下的权威源类型:" + upstreamType.getDescription(), mainTree);
                            //} else {
                            //    throw e;
                            //}
                            if (new Long("1085").equals(e.getCode())) {
                                log.error("请求资源地址失败,请检查权威源:{}下的权威源类型:{},通过影子副本获取数据", upstream.getAppName() + "(" + upstream.getAppCode() + ")", upstreamType.getDescription());
                            }else if(new Long("1087").equals(e.getCode())) {
                                throw  e;
                            } else {
                                e.printStackTrace();
                                log.error("{}:获取上游数据失败:{} ,通过影子副本获取数据", type, e.getErrorMsg());
                            }
                            //通过影子副本获取数据
                            upstreamTree = shadowCopyService.findDataByUpstreamTypeAndType(upstreamType.getId(), type, upstreamType.getDomain());
                            if (CollectionUtils.isEmpty(upstreamTree)) {
                                throw new CustomException(ResultCode.SHADOW_GET_DATA_ERROR, null, null, type, upstreamType.getDescription());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            logger.error("{}{} 中的类型 【{}】 表达式异常, 通过影子副本获取数据", (null == treeType ? "" : treeType.getName() + "下"), ("".equals(nodeCode) ? "根节点" : nodeCode), upstreamType.getDescription());
                            //throw new CustomException(ResultCode.EXPRESSION_ERROR, null, null, null == treeType ? "" : treeType.getName(), "".equals(nodeCode) ? "根节点" : nodeCode, upstreamType.getDescription());
                            //通过影子副本获取数据
                            upstreamTree = shadowCopyService.findDataByUpstreamTypeAndType(upstreamType.getId(), type, upstreamType.getDomain());
                            if (CollectionUtils.isEmpty(upstreamTree)) {
                                throw new CustomException(ResultCode.SHADOW_GET_DATA_ERROR, null, null, type, upstreamType.getDescription());
                            }
                        }

                        //验证树的合法性
                        if (upstreamTree.size() <= 0) {
                            logger.info("节点'{}'数据源{}获取部门数据为空", code, upstreamType.getGraphqlUrl());
                            //return mainTree;
                            continue;
                        }
                        logger.info("节点'{}'数据获取完成", code);


                        List<TreeBean> upstreamDept = new ArrayList<>();
                        //遍历拉取的数据,标准化数据,以及赋值逻辑运算所需的值
                        for (Object o : upstreamTree) {
//                            JSONObject dept = (JSONObject) o;
                            JSONObject dept = JSON.parseObject(JSON.toJSONString(o));
                            //校验数据是否合法
                            if (null != dept.getInteger(TreeEnum.DEL_MARK.getCode()) && 1 != dept.getInteger(TreeEnum.DEL_MARK.getCode()) && 0 != dept.getInteger(TreeEnum.DEL_MARK.getCode())) {
                                logger.error(type + "是否有效字段不合法{}", dept.getInteger(TreeEnum.DEL_MARK.getCode()));
                                continue;
                            }
                            if (null != dept.getInteger(TreeEnum.ACTIVE.getCode()) && 1 != dept.getInteger(TreeEnum.ACTIVE.getCode()) && 0 != dept.getInteger(TreeEnum.ACTIVE.getCode())) {
                                logger.error(type + "是否删除字段不合法{}", dept.getInteger(TreeEnum.ACTIVE.getCode()));
                                continue;
                            }
                            if (null == dept.getString(TreeEnum.ACTIVE.getCode())) {
                                dept.put(TreeEnum.ACTIVE.getCode(), 1);
                            }
                            if (null == dept.getString(TreeEnum.DEL_MARK.getCode())) {
                                dept.put(TreeEnum.DEL_MARK.getCode(), 0);
                            }
                            if (null == dept.getString(TreeEnum.PARENT_CODE.getCode())) {
                                dept.put(TreeEnum.PARENT_CODE.getCode(), "");
                            }
                            if (null == dept.getString(TreeEnum.CREATE_TIME.getCode())) {
                                dept.put(TreeEnum.CREATE_TIME.getCode(), timestamp);
                            } else {
                                dept.put(TreeEnum.CREATE_TIME.getCode(), dept.getTimestamp(TreeEnum.CREATE_TIME.getCode()));
                            }
                            if (null == dept.getString(TreeEnum.UPDATE_TIME.getCode())) {
                                dept.put(TreeEnum.UPDATE_TIME.getCode(), timestamp);
                            } else {
                                dept.put(TreeEnum.UPDATE_TIME.getCode(), dept.getTimestamp(TreeEnum.UPDATE_TIME.getCode()));
                            }
                            if (null == dept.getString(TreeEnum.EN_NAME.getCode())) {
                                dept.put(TreeEnum.EN_NAME.getCode(), "");
                            }
                            if (null == dept.getString(TreeEnum.ABBREVIATION.getCode())) {
                                dept.put(TreeEnum.ABBREVIATION.getCode(), "");
                            }
                            if (null == dept.getString(TreeEnum.RELATION_TYPE.getCode())) {
                                dept.put(TreeEnum.RELATION_TYPE.getCode(), "");
                            }
                            if (null == dept.getString(TreeEnum.INDEPENDENT.getCode())) {
                                dept.put(TreeEnum.INDEPENDENT.getCode(), 0);
                            }
                            dept.put("upstreamTypeId", upstreamType.getId());
                            dept.put("treeType", null == treeType ? "" : treeType.getCode());
                            dept.put("ruleId", nodeRule.getId());
                            dept.put("color", upstream.getColor());
                            dept.put("isRuled", false);
                            dept.put("source", upstream.getAppName() + "(" + upstream.getAppCode() + ")");

                            if ("post".equals(type) && StringUtils.isBlank(dept.getString(TreeEnum.FORMAL.getCode()))) {
                                //如果是岗位  formal默认值赋值
                                dept.put("formal", false);
                            }
                            //处理扩展字段
                            ConcurrentHashMap<String, String> map = null;
                            if (!CollectionUtils.isEmpty(dynamicCodes)) {
                                map = new ConcurrentHashMap<>();
                                for (String dynamicCode : dynamicCodes) {
                                    if (dept.containsKey(dynamicCode)) {
                                        if (StringUtils.isNotBlank(dept.getString(dynamicCode))) {
                                            map.put(dynamicCode, dept.getString(dynamicCode));
                                        }
                                    }
                                }
                                logger.info("处理{}的上游扩展字段值为{}", dept, map);
                                dept.put("dynamic", map);
                            }
                            //逻辑处理字段,规则是否启用
                            dept.put("ruleStatus", nodeRule.getActive());
                            upstreamDept.add(dept.toJavaObject(TreeBean.class));
                        }

                        ////循环引用判断
                        //this.circularData(upstreamTree, status, mainTree,domain);
                        // 判断权威源拉取数据是否有重复性问题
                        this.groupByCode(upstreamDept, status, domain);


                        //判断上游是否给出时间戳
                        this.judgeTime(upstreamTree, timestamp);

                        //对树 json 转为 map
                        Map<String, TreeBean> upstreamMap = TreeUtil.toMap(upstreamDept);

                        //对树进行 parent 分组
                        Map<String, List<TreeBean>> childrenMap = TreeUtil.groupChildren(upstreamDept);
                        //查询 树 运行  规则,
                        //List<NodeRulesRange> nodeRulesRanges = rangeDao.getByRulesId(nodeRule.getId(), null);
                        List<NodeRulesRange> nodeRulesRanges = nodeRule.getNodeRulesRanges();
                        mergeDeptMap = new ConcurrentHashMap<>();
                        logger.info("节点'{}'开始运行挂载", code);
                        //获取并检测 需要挂载的树， add 进入 待合并的树集合 mergeDept
                        mountRules(nodeCode, mainTree, upstreamMap, childrenMap, nodeRulesRanges, mergeDeptMap, upstream.getAppName() + "(" + upstream.getAppCode() + ")", treeType);
                        //在挂载基础上进行排除
                        excludeRules(nodeCode, mergeDeptMap, childrenMap, nodeRulesRanges, domain, treeType, mainTree);
                        logger.info("节点'{}'开始运行排除", code);
                        // 对最终要挂载的树进行重命名
                        renameRules(mergeDeptMap, nodeRulesRanges, childrenMap, type);
                        logger.info("节点'{}'开始运行重命名", code);
                        logger.info("节点'{}'的规则运算完成：{}", nodeCode, mergeDeptMap);


                        logger.info("部门节点:{}的规则运算完成", nodeCode);

                        //判空
                        this.judgeData(mergeDeptMap);
                        //循环引用判断
                        this.circularData(mergeDeptMap, status, mainTree, domain);



            /*
                 和主树进行合并校验 (组织机构针对于同组织机构树做运算)
                1: 确认权威源， 根据源的排序，在合并时候，判断是否要修改同级别，同code 的节点来源。
                2： 如果子节点不继承父级规则，则由父级规则之前合并进来的子树先进行删除
             */
                        // 如果本次规则  权重 大于 继承 规则。  则 丢掉主树中 同code 同parent的节点
                        //非根节点计算继承规则
                        //if (!"".equals(nodeCode)) {
                        if (inheritNodeRules.size() > 0) {
                            for (Map.Entry<String, NodeRules> nodeRulesEntry : inheritNodeRules.entrySet()) {
                                // 当前权重大于 继承来源
                                if (nodeRule.getSort() < nodeRulesEntry.getValue().getSort()) {
                                    Map<String, TreeBean> mainTreeMap2 = new ConcurrentHashMap<>();
                                    mainTreeMap2.putAll(mainTreeMap);
                                    for (Map.Entry<String, TreeBean> deptEntry : mainTreeMap2.entrySet()) {
                                        String key = deptEntry.getKey();
                                        TreeBean value = deptEntry.getValue();
                                        if (mergeDeptMap.containsKey(key) &&
                                                mergeDeptMap.get(key).getParentCode().equals(value.getParentCode()) &&
                                                ((null == mergeDeptMap.get(key).getTreeType() ? "" : mergeDeptMap.get(key).getTreeType())
                                                        .equals(null == value.getTreeType() ? "" : value.getTreeType()))
                                        ) {

                                            //如果为BUILTIN数据移除merge中的数据
                                            if ("BUILTIN".equals(value.getDataSource())) {
                                                mergeDeptMap.remove(key);
                                            } else {
                                                //如果为API数据移除mainTree中的数据
                                                mainTreeMap.remove(key);
                                            }
                                        }
                                    }
                                } else {
                                    // 当前权重 小于 继承来源
                                    Map<String, TreeBean> mergeDeptMap2 = new ConcurrentHashMap<>();
                                    mergeDeptMap2.putAll(mergeDeptMap);
                                    for (Map.Entry<String, TreeBean> deptEntry : mergeDeptMap2.entrySet()) {
                                        String key = deptEntry.getKey();
                                        TreeBean value = deptEntry.getValue();

                                        if (mainTreeMap.containsKey(key) &&
                                                mainTreeMap.get(key).getParentCode().equals(value.getParentCode()) &&
                                                ((null == mergeDeptMap.get(key).getTreeType() ? "" : mergeDeptMap.get(key).getTreeType())
                                                        .equals(null == value.getTreeType() ? "" : value.getTreeType()))
                                        ) {
                                            //如果为API数据移除mainTree中的数据
                                            if ("API".equals(value.getDataSource()) || "ENTERPRISE".equals(value.getDataSource())) {
                                                mainTreeMap.remove(key);
                                            } else {
                                                //如果为BUILTIN数据移除merge中的数据
                                                mergeDeptMap.remove(key);
                                            }
                                        }
                                    }

                                }
                            }
                        } else {
                            // 完全没有继承
                            if (nodeRule.getSort() == 0) {
                                if (!"".equals(nodeCode)) {
                                    // 完全不继承 第一个数据源， 需处理掉 主树当前节点下所有的子集
                                    TreeUtil.removeTree(nodeCode, mainTreeChildren, mainTreeMap);
                                }
                                Map<String, TreeBean> mainTreeMap2 = new ConcurrentHashMap<>();
                                mainTreeMap2.putAll(mainTreeMap);
                                for (Map.Entry<String, TreeBean> deptEntry : mainTreeMap2.entrySet()) {
                                    String key = deptEntry.getKey();
                                    TreeBean value = deptEntry.getValue();
                                    if (mergeDeptMap.containsKey(key) &&
                                            mergeDeptMap.get(key).getParentCode().equals(value.getParentCode()) &&
                                            ((null == mergeDeptMap.get(key).getTreeType() ? "" : mergeDeptMap.get(key).getTreeType())
                                                    .equals(null == value.getTreeType() ? "" : value.getTreeType()))
                                    ) {

                                        //如果为BUILTIN数据移除merge中的数据
                                        if ("BUILTIN".equals(value.getDataSource())) {
                                            mergeDeptMap.remove(key);
                                        } else {
                                            //如果为API数据移除mainTree中的数据
                                            mainTreeMap.remove(key);
                                        }
                                    }
                                }
                            } else {
                                //完全不继承 非一个数据源， 直接去重 向主树合并
                                Map<String, TreeBean> mergeDeptMap2 = new ConcurrentHashMap<>();
                                mergeDeptMap2.putAll(mergeDeptMap);
                                for (Map.Entry<String, TreeBean> deptEntry : mergeDeptMap2.entrySet()) {
                                    String key = deptEntry.getKey();
                                    TreeBean value = deptEntry.getValue();
                                    if (mainTreeMap.containsKey(key) &&
                                            mainTreeMap.get(key).getParentCode().equals(value.getParentCode()) &&
                                            ((null == mergeDeptMap.get(key).getTreeType() ? "" : mergeDeptMap.get(key).getTreeType())
                                                    .equals(null == value.getTreeType() ? "" : value.getTreeType()))
                                    ) {
                                        //如果为BUILTIN数据移除merge中的数据
                                        if ("BUILTIN".equals(value.getDataSource())) {
                                            mergeDeptMap.remove(key);
                                        } else {
                                            //如果为API数据移除mainTree中的数据
                                            mainTreeMap.remove(key);
                                        }
                                    }
                                }

                            }

                        }
                        // }
                        //todo 判断当前权威源类型是否为增量处理
                        if (null != upstreamType.getIsIncremental() && upstreamType.getIsIncremental()) {
                            if (null != mergeDeptMap) {
                                Collection<TreeBean> values = mergeDeptMap.values();
                                IncrementalTask incrementalTask = null;
                                if (null != currentTask) {
                                    //处理增量日志
                                    List<TreeBean> collect1 = values.stream().sorted(Comparator.comparing(TreeBean::getUpdateTime).reversed()).collect(Collectors.toList());
                                    incrementalTask = new IncrementalTask();
                                    incrementalTask.setId(UUID.randomUUID().toString());
                                    incrementalTask.setMainTaskId(currentTask.getId());
                                    incrementalTask.setType(type);
                                    logger.info("类型:{},权威源类型:{},上游增量最大修改时间:{} -> {},当前时刻:{}", upstreamType.getSynType(), upstreamType.getId(), collect1.get(0).getUpdateTime(), collect1.get(0).getUpdateTime().toInstant(ZoneOffset.ofHours(+8)).toEpochMilli(), System.currentTimeMillis());
                                    long min = Math.min(collect1.get(0).getUpdateTime().toInstant(ZoneOffset.ofHours(+8)).toEpochMilli(), System.currentTimeMillis());
                                    incrementalTask.setTime(new Timestamp(min));
                                    incrementalTask.setUpstreamTypeId(collect1.get(0).getUpstreamTypeId());
                                    incrementalTaskService.save(incrementalTask, domain);

                                }
                                //增量对比处理内存中sso的数据
                                ssoBeansMap = incrementalDataProcessing(values, ssoBeansMap, dynamicAttrs, valueMap, valueUpdate, valueInsert, upstreamHashMap, incrementalTask, result);
                            }

                            // 将本次 add 进的 节点 进行 规则运算
                            for (Map.Entry<String, TreeBean> entry : mergeDeptMap.entrySet()) {
                                mainTree = nodeRules(domain, treeType, entry.getValue().getCode(), mainTree, status, type, dynamicCodes, ssoBeansMap, dynamicAttrs, valueMap, valueUpdate, valueInsert, upstreamHashMap, result, nodesMap, currentTask);
                            }

                            continue;
                        }
                        mainTree = new ArrayList<>(mainTreeMap.values());

                        if (null != mergeDeptMap) {
                            Collection<TreeBean> values = mergeDeptMap.values();

                            mainTree.addAll(new ArrayList<>(values));
                        }

                        //拼接到mainTree后校验总树是否有重复
                        this.groupByCode(mainTree, status, domain);
                        mainTreeMap = mainTree.stream().collect(Collectors.toMap(TreeBean::getCode, deptBean -> deptBean));
                        mainDept = mainTreeMap.values();
                        mainTreeChildren = TreeUtil.groupChildren(new ArrayList<>(mainDept));

                        // 将本次 add 进的 节点 进行 规则运算
                        for (Map.Entry<String, TreeBean> entry : mergeDeptMap.entrySet()) {
                            mainTree = nodeRules(domain, treeType, entry.getValue().getCode(), mainTree, status, type, dynamicCodes, ssoBeansMap, dynamicAttrs, valueMap, valueUpdate, valueInsert, upstreamHashMap, result, nodesMap, currentTask);
                        }



                        /*========================规则运算完成=============================*/
                    }
                }
                if (null != mergeDeptMap) {
                    //处理父节点
                    ArrayList<TreeBean> treeBeans = new ArrayList<>(mergeDeptMap.values());
                    if (null != treeBeans && treeBeans.size() > 0) {
                        for (TreeBean treeBean : treeBeans) {
                            if (!mergeDeptMap.containsKey(treeBean.getParentCode())) {
                                treeBean.setParentCode(nodeCode);
                                mergeDeptMap.put(treeBean.getCode(), treeBean);
                            }
                        }
                    }
                }


                if (null == nodeRules && (!"".equals(nodeCode))) {
                    TreeUtil.removeTree(code, mainTreeChildren, mainTreeMap);
                    mainTree = new ArrayList<>(mainTreeMap.values());
                }
            }
        }


        return mainTree;
    }

    public JSONArray judgeTime(JSONArray upstreamTree, LocalDateTime timestamp) {
        //判断上游是否给出创建时间和修改时间
        String str = upstreamTree.toString();
        boolean flag = str.contains("createTime");
        boolean upFlag = str.contains("updateTime");
        //未给出则手动赋值
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

    /**
     * @param mergeDeptMap
     * @param status
     * @param mainTree
     * @Description: 循环依赖判断
     * @return: void
     */
    public void circularData(Map<String, TreeBean> mergeDeptMap, Integer status, List<TreeBean> mainTree, DomainInfo domainInfo) {
        Collection<TreeBean> values = mergeDeptMap.values();
        ArrayList<TreeBean> mergeList = new ArrayList<>(values);
        for (TreeBean treeBean : mergeList) {
            for (TreeBean bean : mergeList) {
                if (treeBean.getCode().equals(bean.getParentCode()) && treeBean.getParentCode().equals(bean.getCode())) {

                    //存放异常信息的容器
                    ArrayList<ErrorData> list = new ArrayList<>();
                    //String deptTreeName = null;
                    //String treeBeanName = null;
                    //String treeBeanCode = null;
                    String treeName = null;
                    String treeCode = null;

                    //String deptTreeNameM = null;
                    //String treeBeanNameM = null;
                    //String treeBeanCodeM = null;
                    String treeNameM = null;
                    String treeCodeM = null;

                    DeptTreeType deptTreeType = deptTreeTypeService.findByCode(bean.getTreeType(), domainInfo.getId());

                    if (("API".equals(bean.getDataSource())) || ("BUILTIN".equals(bean.getDataSource())) || ("ENTERPRISE".equals(bean.getDataSource()))) {
                        //deptTreeName = (null == deptTreeType ? "" : deptTreeType.getName());
                        //treeBeanName = "".equals(bean.getCode()) ? "根节点" : bean.getName();
                        //treeBeanCode = bean.getCode();
                        treeName = bean.getName();
                        treeCode = bean.getCode();
                        list.add(new ErrorData((null == deptTreeType ? "" : deptTreeType.getId()), bean.getRuleId(), bean.getCode()));
                    } else {

                        NodeRules nodeRules = rulesService.findNodeRulesById(bean.getRuleId(), status);
                        List<Node> nodes = nodeService.findById(nodeRules.getNodeId());
                        //deptTreeName = (null == deptTreeType ? "" : deptTreeType.getName());
                        //treeBeanName = "".equals(nodes.get(0).getNodeCode()) ? "根节点" : mergeDeptMap.get(nodes.get(0).getNodeCode()).getName();
                        //treeBeanCode = nodes.get(0).getNodeCode();
                        treeName = bean.getName();
                        treeCode = bean.getCode();
                        list.add(new ErrorData((null == deptTreeType ? "" : deptTreeType.getId()), bean.getRuleId(), nodes.get(0).getNodeCode()));

                    }

                    if (null == treeBean.getTreeType()) {
                        treeBean.setTreeType("");
                    }
                    DeptTreeType deptTreeType2 = deptTreeTypeService.findByCode(treeBean.getTreeType(), domainInfo.getId());
                    if (("API".equals(treeBean.getDataSource())) || ("BUILTIN".equals(treeBean.getDataSource())) || ("ENTERPRISE".equals(treeBean.getDataSource()))) {
                        //deptTreeNameM = null == deptTreeType2 ? "" : deptTreeType2.getName();
                        //treeBeanNameM = "".equals(treeBean.getCode()) ? "根节点" : treeBean.getName();
                        //treeBeanCodeM = treeBean.getCode();
                        treeNameM = treeBean.getName();
                        treeCodeM = treeBean.getCode();
                        list.add(new ErrorData((null == deptTreeType2 ? "" : deptTreeType2.getId()), treeBean.getRuleId(), treeBean.getCode()));
                    } else {
                        NodeRules nodeRules2 = rulesService.findNodeRulesById(treeBean.getRuleId(), status);
                        List<Node> nodes2 = nodeService.findById(nodeRules2.getNodeId());
                        //deptTreeNameM = null == deptTreeType2 ? "" : deptTreeType2.getName();
                        //treeBeanNameM = "".equals(nodes2.get(0).getNodeCode()) ? "根节点" : mergeDeptMap.get(nodes2.get(0).getNodeCode()).getName();
                        //treeBeanCodeM = nodes2.get(0).getNodeCode();
                        treeNameM = treeBean.getName();
                        treeCodeM = treeBean.getCode();
                        list.add(new ErrorData((null == deptTreeType2 ? "" : deptTreeType2.getId()), treeBean.getRuleId(), nodes2.get(0).getNodeCode()));
                    }
                    logger.error(" {} 节点 {}   规则{} 中的 数据 {} code:{} 与 机构{} 节点 {}   规则{} 中的 数据 {} code:{} 循环依赖 ",
                            null == deptTreeType ? "" : deptTreeType.getName(), ("".equals(bean.getCode()) ? "根节点" : bean.getCode()), null == bean.getRuleId() ? " " : bean.getRuleId(), bean.getName(), bean.getCode(),

                            null == deptTreeType2 ? "" : deptTreeType2.getName(), ("".equals(treeBean.getCode()) ? "根节点" : treeBean.getCode()), null == bean.getRuleId() ? " " : bean.getRuleId(), treeBean.getName(), treeBean.getCode());
//                    throw new CustomException(ResultCode.CYCLE_ERROR, list, mainTree, deptTreeName, treeBeanName, treeBeanCode, treeName, treeCode
//                            , deptTreeNameM, treeBeanNameM, treeBeanCodeM, treeNameM, treeCodeM);
                    throw new CustomException(ResultCode.CYCLE_ERROR, list, mainTree, treeName, treeCode
                            , treeNameM, treeCodeM);

                }
            }
        }

    }

    public void circularData(JSONArray mergeDeptMap, Integer status, List<TreeBean> mainTree, DomainInfo domainInfo) {
        List<TreeBean> mergeList = JSON.parseArray(mergeDeptMap.toString(), TreeBean.class);
        Map<String, TreeBean> mergeMap = mergeList.stream().collect(Collectors.toMap((TreeBean::getCode), (dept -> dept)));

        for (TreeBean treeBean : mergeList) {
            for (TreeBean bean : mergeList) {
                if (treeBean.getCode().equals(bean.getParentCode()) && treeBean.getParentCode().equals(bean.getCode())) {

                    //存放异常信息的容器
                    ArrayList<ErrorData> list = new ArrayList<>();
//                    StringBuffer ex = null;

                    String deptTreeName = null;
                    String treeBeanName = null;
                    String treeBeanCode = null;
                    String treeName = null;
                    String treeCode = null;

                    String deptTreeNameM = null;
                    String treeBeanNameM = null;
                    String treeBeanCodeM = null;
                    String treeNameM = null;
                    String treeCodeM = null;

                    DeptTreeType deptTreeType = deptTreeTypeService.findByCode(bean.getTreeType(), domainInfo.getId());

                    if (("API".equals(bean.getDataSource())) || ("BUILTIN".equals(bean.getDataSource())) || ("ENTERPRISE".equals(bean.getDataSource()))) {
                        deptTreeName = (null == deptTreeType ? "" : deptTreeType.getName());
                        treeBeanName = "".equals(bean.getCode()) ? "根节点" : bean.getName();
                        treeBeanCode = bean.getCode();
                        treeName = bean.getName();
                        treeCode = bean.getCode();
//                        ex = new StringBuffer((null == deptTreeType ? "" : deptTreeType.getName()) + "节点 (" + ("".equals(bean.getCode()) ? "根节点" : bean.getName())
//                                + "(" + bean.getCode() + "))" + "中的数据" + bean.getName() + "(" + bean.getCode() + ")" + "与");
                        list.add(new ErrorData((null == deptTreeType ? "" : deptTreeType.getId()), bean.getRuleId(), bean.getCode()));
                    } else {

                        NodeRules nodeRules = rulesService.findNodeRulesById(bean.getRuleId(), status);
                        List<Node> nodes = nodeService.findById(nodeRules.getNodeId());
//                        ex = new StringBuffer((null == deptTreeType ? "" : deptTreeType.getName()) + "节点 (").append("".equals(nodes.get(0).getNodeCode()) ? "根节点" : mergeMap.get(nodes.get(0).getNodeCode()).getName())
//                                .append("(").append(nodes.get(0).getNodeCode()).append("))" + "中的数据").append(bean.getName()).append("(").append(bean.getCode()).append(")").append(" 与");
                        deptTreeName = (null == deptTreeType ? "" : deptTreeType.getName());
                        treeBeanName = "".equals(nodes.get(0).getNodeCode()) ? "根节点" : mergeMap.get(nodes.get(0).getNodeCode()).getName();
                        treeBeanCode = nodes.get(0).getNodeCode();
                        treeName = bean.getName();
                        treeCode = bean.getCode();
                        list.add(new ErrorData((null == deptTreeType ? "" : deptTreeType.getId()), bean.getRuleId(), nodes.get(0).getNodeCode()));

                    }

                    if (null == treeBean.getTreeType()) {
                        treeBean.setTreeType("");
                    }
                    DeptTreeType deptTreeType2 = deptTreeTypeService.findByCode(treeBean.getTreeType(), domainInfo.getId());
                    if (("API".equals(treeBean.getDataSource())) || ("BUILTIN".equals(treeBean.getDataSource())) || ("ENTERPRISE".equals(treeBean.getDataSource()))) {
//                        ex.append(null == deptTreeType2 ? "" : deptTreeType2.getName()).append("节点 (").append("".equals(treeBean.getCode()) ? "根节点" : treeBean.getName())
//                                .append("(").append(treeBean.getCode()).append("))").append("中的数据").append(treeBean.getName()).append("(").append(treeBean.getCode())
//                                .append(")").append("循环依赖");
                        deptTreeNameM = null == deptTreeType2 ? "" : deptTreeType2.getName();
                        treeBeanNameM = "".equals(treeBean.getCode()) ? "根节点" : treeBean.getName();
                        treeBeanCodeM = treeBean.getCode();
                        treeNameM = treeBean.getName();
                        treeCodeM = treeBean.getCode();
                        list.add(new ErrorData((null == deptTreeType2 ? "" : deptTreeType2.getId()), treeBean.getRuleId(), treeBean.getCode()));
                    } else {
                        NodeRules nodeRules2 = rulesService.findNodeRulesById(treeBean.getRuleId(), status);
                        List<Node> nodes2 = nodeService.findById(nodeRules2.getNodeId());
//                        ex.append(null == deptTreeType2 ? "" : deptTreeType2.getName()).append("节点 (").append("".equals(nodes2.get(0).getNodeCode()) ? "根节点" : mergeMap.get(nodes2.get(0).getNodeCode()).getName())
//                                .append("(").append(nodes2.get(0).getNodeCode()).append("))").append("中的数据").append(treeBean.getName()).append("(")
//                                .append(treeBean.getCode()).append(")").append("循环依赖");
                        deptTreeNameM = null == deptTreeType2 ? "" : deptTreeType2.getName();
                        treeBeanNameM = "".equals(nodes2.get(0).getNodeCode()) ? "根节点" : mergeMap.get(nodes2.get(0).getNodeCode()).getName();
                        treeBeanCodeM = nodes2.get(0).getNodeCode();
                        treeNameM = treeBean.getName();
                        treeCodeM = treeBean.getCode();
                        list.add(new ErrorData((null == deptTreeType2 ? "" : deptTreeType2.getId()), treeBean.getRuleId(), nodes2.get(0).getNodeCode()));
                    }
                    logger.error(" {} 节点 {}   规则{} 中的 数据 {} code:{} 与 机构{} 节点 {}   规则{} 中的 数据 {} code:{} 循环依赖 ",
                            null == deptTreeType ? "" : deptTreeType.getName(), ("".equals(bean.getCode()) ? "根节点" : bean.getCode()), null == bean.getRuleId() ? " " : bean.getRuleId(), bean.getName(), bean.getCode(),
                            null == deptTreeType2 ? "" : deptTreeType2.getName(), ("".equals(treeBean.getCode()) ? "根节点" : treeBean.getCode()), null == bean.getRuleId() ? " " : bean.getRuleId(), treeBean.getName(), treeBean.getCode());
//                    errorData.put(domain.getDomainName(), list);
//                    errorTree.put(domain.getDomainName(), mainTree);
//                    throw new Exception(ex.toString());

//                    throw new CustomException(ResultCode.CYCLE_ERROR, list, mainTree, deptTreeName, treeBeanName, treeBeanCode, treeName, treeCode
//                            , deptTreeNameM, treeBeanNameM, treeBeanCodeM, treeNameM, treeCodeM);
                    throw new CustomException(ResultCode.CYCLE_ERROR, list, mainTree, treeName, treeCode
                            , treeNameM, treeCodeM);


                }
            }
        }

    }

    /**
     * @param map
     * @Description: code, name 判空校验
     * @return: void
     */
    public void judgeData(Map<String, TreeBean> map) throws CustomException {
        Collection<TreeBean> values = map.values();
        ArrayList<TreeBean> mainList = new ArrayList<>(values);
        for (TreeBean treeBean : mainList) {
            if (StringUtils.isBlank(treeBean.getName()) || StringUtils.isBlank(treeBean.getCode())) {
                throw new CustomException(ResultCode.FAILED, "含非法数据,请检查");
            }
        }
    }

    /**
     * @param treeBeans
     * @param status
     * @param domainInfo
     * @Description: code重复性校验
     * @return: void
     */
    public void groupByCode(List<TreeBean> treeBeans, Integer status, DomainInfo domainInfo) {
        HashMap<String, List<TreeBean>> resultBeans = new HashMap<>();
        ArrayList<TreeBean> mergeList = new ArrayList<>();
        if (null != treeBeans && treeBeans.size() > 0) {
            for (TreeBean treeBean : treeBeans) {
                //API BUILTIN
                List<TreeBean> beans = resultBeans.get(treeBean.getCode());
                if (null != beans && beans.size() > 0) {
                    //存放异常信息的容器
                    ArrayList<ErrorData> list = new ArrayList<>();

                    String deptTreeName = null;
                    String treeBeanName = null;
                    String treeBeanCode = null;
                    String treeName = null;
                    String treeCode = null;

                    String deptTreeNameM = null;
                    String treeBeanNameM = null;
                    String treeBeanCodeM = null;
                    String treeNameM = null;
                    String treeCodeM = null;

                    mergeList.add(treeBean);

                    TreeBean treeBean1 = beans.get(0);
                    if (null == treeBean1.getTreeType()) {
                        treeBean1.setTreeType("");
                    }
                    DeptTreeType deptTreeType = deptTreeTypeService.findByCode(treeBean1.getTreeType(), domainInfo.getId());
                    if (("API".equals(treeBean1.getDataSource())) || ("BUILTIN".equals(treeBean1.getDataSource())) || ("ENTERPRISE".equals(treeBean1.getDataSource()))) {

                        //deptTreeName = (null == deptTreeType ? "" : deptTreeType.getName());
                        //treeBeanName = StringUtils.isBlank(treeBean1.getCode()) ? "根节点" : treeBean1.getName();
                        //treeBeanCode = treeBean1.getCode();
                        treeName = treeBean1.getName();
                        treeCode = treeBean1.getCode();
                        list.add(new ErrorData((null == deptTreeType ? "" : deptTreeType.getId()), treeBean1.getRuleId(), treeBean1.getCode()));

                    } else {

                        NodeRules nodeRules = rulesService.findNodeRulesById(treeBean1.getRuleId(), null);
                        List<Node> nodes = nodeService.findById(nodeRules.getNodeId());
                        //deptTreeName = (null == deptTreeType ? "" : deptTreeType.getName());
                        //treeBeanName = StringUtils.isBlank(nodes.get(0).getNodeCode()) ? "根节点" : resultBeans.get(nodes.get(0).getNodeCode()).get(0).getName();
                        //treeBeanCode = nodes.get(0).getNodeCode();
                        treeName = treeBean1.getName();
                        treeCode = treeBean1.getCode();
                        list.add(new ErrorData((null == deptTreeType ? "" : deptTreeType.getId()), treeBean1.getRuleId(), nodes.get(0).getNodeCode()));

                    }

                    if (null == treeBean.getTreeType()) {
                        treeBean.setTreeType("");
                    }
                    DeptTreeType deptTreeType2 = deptTreeTypeService.findByCode(treeBean.getTreeType(), domainInfo.getId());
                    if (("API".equals(treeBean.getDataSource())) || ("BUILTIN".equals(treeBean.getDataSource())) || ("ENTERPRISE".equals(treeBean.getDataSource()))) {
                        //deptTreeNameM = null == deptTreeType2 ? "" : deptTreeType2.getName();
                        //treeBeanNameM = StringUtils.isBlank(treeBean.getCode()) ? "根节点" : treeBean.getName();
                        //treeBeanCodeM = treeBean.getCode();
                        treeNameM = treeBean.getName();
                        treeCodeM = treeBean.getCode();
                        list.add(new ErrorData((null == deptTreeType2 ? "" : deptTreeType2.getId()), treeBean.getRuleId(), treeBean.getCode()));
                    } else {
                        NodeRules nodeRules2 = rulesService.findNodeRulesById(treeBean.getRuleId(), null);
                        List<Node> nodes2 = nodeService.findById(nodeRules2.getNodeId());
                        //deptTreeNameM = null == deptTreeType2 ? "" : deptTreeType2.getName();
                        //treeBeanNameM = StringUtils.isBlank(nodes2.get(0).getNodeCode())? "根节点" : resultBeans.get(nodes2.get(0).getNodeCode()).get(0).getName();
                        //treeBeanCodeM = nodes2.get(0).getNodeCode();
                        treeNameM = treeBean.getName();
                        treeCodeM = treeBean.getCode();
                        list.add(new ErrorData((null == deptTreeType2 ? "" : deptTreeType2.getId()), treeBean.getRuleId(), nodes2.get(0).getNodeCode()));
                    }

                    logger.error(" {} 节点 {}   规则{} 中的 数据 {} code:{} 与 机构{} 节点 {}   规则{} 中的 数据 {} code:{} 重复 ",
                            null == deptTreeType ? "" : deptTreeType.getName(), ("".equals(treeBean1.getCode()) ? "根节点" : treeBean1.getCode()), null == treeBean1.getRuleId() ? " " : treeBean1.getRuleId(), treeBean1.getName(), treeBean1.getCode(),
                            null == deptTreeType2 ? "" : deptTreeType2.getName(), ("".equals(treeBean.getCode()) ? "根节点" : treeBean.getCode()), null == treeBean1.getRuleId() ? " " : treeBean1.getRuleId(), treeBean.getName(), treeBean.getCode());
//
//                    throw new CustomException(ResultCode.REPEAT_ERROR, list, mergeList, deptTreeName, treeBeanName, treeBeanCode, treeName, treeCode
//                            , deptTreeNameM, treeBeanNameM, treeBeanCodeM, treeNameM, treeCodeM);
                    throw new CustomException(ResultCode.REPEAT_ERROR, list, mergeList, treeName, treeCode
                            , treeNameM, treeCodeM);
                }
                mergeList.add(treeBean);
                ArrayList<TreeBean> treeList = new ArrayList<>();
                treeList.add(treeBean);
                resultBeans.put(treeBean.getCode(), treeList);

            }
        }


    }
    //
    ///**
    // * @param upstreamTree
    // * @param id
    // * @Description: 将上游部门数据存入iga数据库
    // * @return: java.lang.Integer
    // */
    //@Transactional
    //Integer saveDataToDb(JSONArray upstreamTree, String id) {
    //    //上游数据已有时间戳的情况
    //    UpstreamDept upstreamDept = new UpstreamDept();
    //    upstreamDept.setDept(upstreamTree.toJSONString());
    //    upstreamDept.setUpstreamTypeId(id);
    //    UpstreamDept upstreamDepts = null;
    //    //查询数据库是否有数据
    //    UpstreamDept upstreamDeptDb = upstreamDeptDao.findUpstreamDeptByUpstreamId(id);
    //    if (null == upstreamDeptDb) {
    //        upstreamDepts = upstreamDeptDao.saveUpstreamDepts(upstreamDept);
    //    } else {
    //        upstreamDept.setId(upstreamDeptDb.getId());
    //        upstreamDept.setCreateTime(new Timestamp(System.currentTimeMillis()));
    //        upstreamDepts = upstreamDeptDao.updateUpstreamDepts(upstreamDept);
    //
    //    }
    //
    //    return null == upstreamDepts ? 0 : 1;
    //
    //}

    /**
     * 增量数据数据库内存对比
     *
     * @param values       上游增量数据
     * @param ssoCollect   sso数据库数据
     * @param dynamicAttrs 扩展字段映射
     * @param valueMap     sso数据库扩展字段值
     * @param valueUpdate  对比后需要修改的扩展字段容器
     * @param valueInsert  对比后需要新增的扩展字段容器
     * @param upstreamMap  判别权威源是否处于无效
     * @return
     */
    private Map<String, TreeBean> incrementalDataProcessing(Collection<TreeBean> values, Map<String, TreeBean> ssoCollect, List<DynamicAttr> dynamicAttrs, Map<String, List<DynamicValue>> valueMap, List<DynamicValue> valueUpdate, List<DynamicValue> valueInsert, Map<String, UpstreamDto> upstreamMap, IncrementalTask incrementalTask, Map<TreeBean, String> result) {

        LocalDateTime now = LocalDateTime.now();

        //Map<TreeBean, String> resultTemp = new HashMap<>();

        ArrayList<TreeBean> ssoBeans = new ArrayList<>(ssoCollect.values());
        //拉取的数据
        ArrayList<TreeBean> pullBeans = new ArrayList<>(values);
        // 处理扩展字段
        //扩展字段id与code对应map
        Map<String, String> attrMap = new ConcurrentHashMap<>();
        if (!CollectionUtils.isEmpty(dynamicAttrs)) {
            attrMap = dynamicAttrs.stream().collect(Collectors.toMap(DynamicAttr::getId, DynamicAttr::getCode));
        }

        //遍历拉取数据
        for (TreeBean pullBean : pullBeans) {
            //来自数据库的部分主树数据没有对应规则标识,默认有效
            if (null == pullBean.getRuleStatus()) {
                pullBean.setRuleStatus(true);
            }
            //标记新增标识
            boolean flag = true;

            if (null != ssoBeans) {
                //当前数据的来源规则是启用的再进行对比
                if (pullBean.getRuleStatus()) {
                    //遍历数据库数据
                    for (TreeBean ssoBean : ssoBeans) {
                        //来源数据规则是启用的再进行对比
                        if (pullBean.getCode().equals(ssoBean.getCode())) {
                            ssoBean.setIsRuled(pullBean.getIsRuled());
                            ssoBean.setColor(pullBean.getColor());
                            if (null != pullBean.getUpdateTime()) {
                                //修改
                                if (null == ssoBean.getUpdateTime() || pullBean.getUpdateTime().isAfter(ssoBean.getUpdateTime())) {


                                    //修改标识 1.source为API 则进行覆盖  2.source都为PULL则对比字段 有修改再标识为true
                                    boolean updateFlag = false;
                                    //删除恢复标识
                                    boolean delRecoverFlag = false;
                                    //del字段标识
                                    boolean delFlag = false;
                                    //失效标识
                                    boolean invalidFlag = false;
                                    //上游是否提供active字段
                                    boolean activeFlag = false;
                                    //是否处理扩展字段标识
                                    boolean dyFlag = true;

                                    //使用sso的对象,将需要修改的值赋值
                                    if ("API".equals(ssoBean.getDataSource())) {
                                        updateFlag = true;
                                    }
                                    //处理sso数据的active为null的情况
                                    if (null == ssoBean.getActive() || "".equals(ssoBean.getActive())) {
                                        ssoBean.setActive(1);
                                    }
                                    ssoBean.setDataSource("INC_PULL");
                                    ssoBean.setSource(pullBean.getSource());
                                    ssoBean.setUpdateTime(now);
                                    ssoBean.setTreeType(pullBean.getTreeType());
                                    ssoBean.setColor(pullBean.getColor());
                                    ssoBean.setIsRuled(pullBean.getIsRuled());
                                    ssoBean.setRuleStatus(pullBean.getRuleStatus());

                                    List<UpstreamTypeField> fields = null;
                                    if (null != pullBean.getUpstreamTypeId()) {
                                        fields = DataBusUtil.typeFields.get(pullBean.getUpstreamTypeId());
                                    }
                                    //获取对应权威源的映射字段
                                    if (null != fields && fields.size() > 0) {
                                        for (UpstreamTypeField field : fields) {
                                            String sourceField = field.getSourceField();

                                            Object newValue = ClassCompareUtil.getGetMethod(pullBean, sourceField);
                                            Object oldValue = ClassCompareUtil.getGetMethod(ssoBean, sourceField);
                                            //均为空 跳过
                                            if (null == oldValue && null == newValue) {
                                                continue;
                                            }
                                            //不为空且相同 跳过
                                            if (null != oldValue && oldValue.equals(newValue)) {
                                                continue;
                                            }
                                            //将修改表示改为true 标识数据需要修改
                                            updateFlag = true;
                                            //如果上游给出删除标记 则使用上游的   不给则不处理
                                            if ("delMark".equalsIgnoreCase(sourceField) && null != ssoBean.getDelMark() && null != pullBean.getDelMark() && (ssoBean.getDelMark() == 1) && (pullBean.getDelMark() == 0)) {
                                                //恢复标识
                                                delRecoverFlag = true;
                                                continue;
                                            }
                                            if ("delMark".equalsIgnoreCase(sourceField) && null != ssoBean.getDelMark() && null != pullBean.getDelMark() && (ssoBean.getDelMark() == 0) && (pullBean.getDelMark() == 1)) {
                                                //删除标识
                                                delFlag = true;
                                                continue;
                                            }
                                            if (sourceField.equalsIgnoreCase("active") && (Integer) oldValue == 1 && (Integer) newValue == 0) {
                                                invalidFlag = true;
                                            }
                                            if (sourceField.equalsIgnoreCase("active")) {
                                                activeFlag = true;
                                            }

                                            //将值更新到sso对象
                                            ClassCompareUtil.setValue(ssoBean, ssoBean.getClass(), sourceField, oldValue, newValue);
                                            logger.info("增量数据 信息更新{}:字段{}: {} -> {} ", pullBean.getCode(), sourceField, oldValue, newValue);
                                        }
                                    }
                                    //标识为恢复数据
                                    if (delRecoverFlag) {
                                        ssoBean.setDelMark(0);
                                        ssoCollect.put(ssoBean.getCode(), ssoBean);
                                        result.put(ssoBean, "recover");
                                        //resultTemp.put(ssoBean, "recover");
                                        //修改标记置为false
                                        updateFlag = false;
                                        logger.info("增量数据 信息{}从删除恢复", ssoBean.getCode());
                                    }
                                    //标识为删除的数据
                                    if (delFlag) {
                                        if ((null != ssoBean.getRuleStatus() && !ssoBean.getRuleStatus()) || (!CollectionUtils.isEmpty(upstreamMap) && upstreamMap.containsKey(ssoBean.getSource()))) {
                                            result.put(ssoBean, "obsolete");
                                            //resultTemp.put(ssoBean, "obsolete");
                                            logger.info("增量数据 对比后应删除{},但检测到对应权威源已无效或规则未启用,跳过该数据", ssoBean.getId());
                                        } else {
                                            //将数据放入删除集合
                                            ssoBean.setDelMark(1);
                                            ssoCollect.remove(ssoBean.getCode());
                                            result.put(ssoBean, "delete");
                                            //resultTemp.put(ssoBean, "delete");
                                            //修改标记置为false
                                            updateFlag = false;
                                            logger.info("增量数据 对比后需要删除{}", ssoBean.getId());
                                        }

                                    }


                                    //修改不为删除的数据
                                    if (updateFlag && ssoBean.getDelMark() != 1) {

                                        ssoBean.setUpdateTime(now);
                                        //失效
                                        if (invalidFlag) {
                                            if ((null != ssoBean.getRuleStatus() && !ssoBean.getRuleStatus()) || (!CollectionUtils.isEmpty(upstreamMap) && upstreamMap.containsKey(ssoBean.getSource()))) {
                                                result.put(ssoBean, "obsolete");
                                                //resultTemp.put(ssoBean, "obsolete");
                                                logger.info("增量数据 对比后应置为失效{},但检测到对应权威源已无效或规则未启用,跳过该数据", ssoBean.getId());
                                            } else {
                                                ssoBean.setActive(0);
                                                ssoCollect.put(ssoBean.getCode(), ssoBean);
                                                result.put(ssoBean, "invalid");
                                                //resultTemp.put(ssoBean, "invalid");
                                                logger.info("增量数据 对比后需要置为失效{}", ssoBean.getId());
                                            }
                                        } else {
                                            //将数据放入修改集合
                                            ssoCollect.put(ssoBean.getCode(), ssoBean);

                                            if (dyFlag) {
                                                //上游的扩展字段
                                                Map<String, String> dynamic = pullBean.getDynamic();
                                                List<DynamicValue> dyValuesFromSSO = null;
                                                //数据库的扩展字段
                                                if (!CollectionUtils.isEmpty(valueMap)) {
                                                    dyValuesFromSSO = valueMap.get(ssoBean.getId());
                                                }
                                                deptService.dynamicProcessing(valueUpdate, valueInsert, attrMap, ssoBean, dynamic, dyValuesFromSSO);
                                                dyFlag = false;
                                            }
                                        }
                                        logger.info("增量数据 对比后需要修改{}", ssoBean);

                                    }
                                    //上游未提供active并且sso与上游源该字段值不一致
                                    if (!activeFlag && (!ssoBean.getActive().equals(pullBean.getActive()))) {
                                        ssoBean.setUpdateTime(now);
                                        ssoBean.setActive(pullBean.getActive());
                                        //将数据放入修改集合
                                        ssoCollect.put(ssoBean.getCode(), ssoBean);
                                        logger.info("手动从失效中恢复{}", ssoBean);

                                        if (dyFlag) {
                                            //上游的扩展字段
                                            Map<String, String> dynamic = pullBean.getDynamic();
                                            List<DynamicValue> dyValuesFromSSO = null;
                                            //数据库的扩展字段
                                            if (!CollectionUtils.isEmpty(valueMap)) {
                                                dyValuesFromSSO = valueMap.get(ssoBean.getId());
                                            }
                                            deptService.dynamicProcessing(valueUpdate, valueInsert, attrMap, ssoBean, dynamic, dyValuesFromSSO);
                                            dyFlag = false;
                                        }
                                    }
                                    //上游未提供delMark并且sso与上游源该字段值不一致
                                    if (!delFlag && !delRecoverFlag && (!ssoBean.getDelMark().equals(pullBean.getDelMark()))) {
                                        ssoBean.setUpdateTime(now);
                                        ssoBean.setDelMark(pullBean.getDelMark());
                                        //将数据放入修改集合
                                        ssoCollect.put(ssoBean.getCode(), ssoBean);

                                        if (dyFlag) {
                                            //上游的扩展字段
                                            Map<String, String> dynamic = pullBean.getDynamic();
                                            List<DynamicValue> dyValuesFromSSO = null;
                                            //数据库的扩展字段
                                            if (!CollectionUtils.isEmpty(valueMap)) {
                                                dyValuesFromSSO = valueMap.get(ssoBean.getId());
                                            }
                                            deptService.dynamicProcessing(valueUpdate, valueInsert, attrMap, ssoBean, dynamic, dyValuesFromSSO);
                                            dyFlag = false;
                                        }

                                        logger.info("手动从删除中恢复{}", ssoBean);
                                    }

                                    //防止重复将数据放入
                                    if (!dyFlag) {
                                        ssoCollect.put(ssoBean.getCode(), ssoBean);
                                        result.put(ssoBean, "update");
                                        //resultTemp.put(ssoBean, "update");
                                    }

                                    //处理扩展字段对比     修改标识为false则认为主体字段没有差异
                                    if (!updateFlag && dyFlag) {
                                        //上游的扩展字段
                                        Map<String, String> dynamic = pullBean.getDynamic();
                                        List<DynamicValue> dyValuesFromSSO = null;
                                        //数据库的扩展字段
                                        if (!CollectionUtils.isEmpty(valueMap)) {
                                            dyValuesFromSSO = valueMap.get(ssoBean.getId());
                                        }
                                        Boolean valueFlag = deptService.dynamicProcessing(valueUpdate, valueInsert, attrMap, ssoBean, dynamic, dyValuesFromSSO);
                                        if (valueFlag) {
                                            result.put(ssoBean, "update");
                                            //resultTemp.put(ssoBean, "update");
                                        }


                                    }
                                }
                            }
                            flag = false;
                        }


                    }

                } else {
                    logger.info("增量数据{},对应规则未启用,本次跳过该数据", pullBean);
                }
                //没有相等的应该是新增(对比code没有对应的标识为新增)  并且当前数据的来源规则是启用的
                if (flag && pullBean.getRuleStatus()) {
                    //新增
                    pullBean.setDataSource("INC_PULL");
                    ssoCollect.put(pullBean.getCode(), pullBean);
                    result.put(pullBean, "insert");
                    //resultTemp.put(pullBean, "insert");
                }
            } else {
                //数据库数据为空的话且数据来源规则是启用的,则默认新增
                if (pullBean.getRuleStatus()) {
                    pullBean.setDataSource("INC_PULL");
                    ssoCollect.put(pullBean.getCode(), pullBean);
                    result.put(pullBean, "insert");
                    //resultTemp.put(pullBean, "insert");

                }
            }
        }
        ////处理单次数据
        //Map<String, List<Map.Entry<TreeBean, String>>> resultMap = resultTemp.entrySet().stream().collect(Collectors.groupingBy(Map.Entry::getValue));
        ////处理数据
        //Integer recoverDept = resultMap.containsKey("recover") ? resultMap.get("recover").size() : 0;
        //Integer insertDept = (resultMap.containsKey("insert") ? resultMap.get("insert").size() : 0) + recoverDept;
        //Integer deleteDept = resultMap.containsKey("delete") ? resultMap.get("delete").size() : 0;
        //Integer updateDept = (resultMap.containsKey("update") ? resultMap.get("update").size() : 0);
        //Integer invalidDept = resultMap.containsKey("invalid") ? resultMap.get("invalid").size() : 0;
        //String operationNo = insertDept + "/" + deleteDept + "/" + updateDept + "/" + invalidDept;
        //incrementalTask.setOperationNo(operationNo);
        //incrementalTaskService.update(incrementalTask);
        return ssoCollect;
    }

}
