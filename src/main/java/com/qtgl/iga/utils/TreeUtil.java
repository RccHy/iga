package com.qtgl.iga.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.TreeBean;

import java.util.*;
import java.util.stream.Collectors;

public class TreeUtil<T> {


    /**
     * 获取树结构中哪些是根节点【】
     *
     * @param map
     * @return
     */
    public static List<String> getTreeRootCode(Map<String, JSONObject> map) throws Exception {

        Collection<JSONObject> valueCollection = map.values();
        List<JSONObject> treeList = new ArrayList<JSONObject>(valueCollection);

        List<String> code = treeList.stream().
                map(jsonObject -> jsonObject.getString(TreeEnum.CODE.getCode())).collect(Collectors.toList());

       /* List<String> parentCode = treeList.stream().
                map(jsonObject -> jsonObject.getString(DeptEnum.PARENTCODE.getCode())).collect(Collectors.toList());
*/
        List<String> rootCode = treeList.stream()
                .filter(json -> !code.contains(json.getString(TreeEnum.PARENTCODE.getCode())))
                .map(jsonObject -> jsonObject.getString(TreeEnum.CODE.getCode()))
                .collect(Collectors.toList());


        return rootCode;
    }

    public static Map<String, List<JSONObject>> groupChildren(JSONArray jsonArray) {
        List<JSONObject> treeList = jsonArray.toJavaList(JSONObject.class);
        Map<String, List<JSONObject>> map = treeList.stream().
                collect(Collectors.groupingBy(jsonObject -> jsonObject.getString(TreeEnum.PARENTCODE.getCode())));
        return map;
    }

    public static Map<String, List<TreeBean>> groupChildren(List<TreeBean> treeBeans) {
        Map<String, List<TreeBean>> map = treeBeans.stream().
                collect(Collectors.groupingBy(TreeBean::getParentCode));
        return map;
    }

    public static Map<String, JSONObject> toMap(JSONArray jsonArray) {
        if (null != jsonArray) {
            List<JSONObject> treeList = jsonArray.toJavaList(JSONObject.class);

            Map<String, JSONObject> map = treeList.stream()
                    .collect(Collectors.toMap(
                            (code -> code.getString(TreeEnum.CODE.getCode())),
                            (value -> value)));
            return map;
        }
        return null;
    }

    public static Map<String, TreeBean> toMap(List<TreeBean> treeBeans) {

        Map<String, TreeBean> map = treeBeans.stream()
                .collect(Collectors.toMap(
                        (dept -> dept.getCode()),
                        (dept -> dept)));
        return map;
    }

    public static void removeMainTree(String code, Map<String, List<TreeBean>> childrenMap, Map<String, TreeBean> mainTree) {
        List<TreeBean> children = childrenMap.get(code);
        if (null != children) {
            for (TreeBean dept : children) {
                if (null != dept.getDataSource()) {
                    if (!dept.getDataSource().equalsIgnoreCase("BUILTIN")) {
                        mainTree.remove(dept.getCode());
                        removeMainTree(dept.getCode(), childrenMap, mainTree);
                    }
                }
            }
        }
    }

    public static void removeTree(String code, Map<String, List<TreeBean>> childrenMap, Map<String, TreeBean> mergeDept) {
        List<TreeBean> children = childrenMap.get(code);
        if (null != children) {
            for (TreeBean deptJson : children) {
                if (("API".equals(deptJson.getDataSource())) || ("BUILTIN".equals(deptJson.getDataSource()))) {
                    continue;
                } else {
                    mergeDept.remove(deptJson.getCode());
                }
                removeTree(deptJson.getCode(), childrenMap, mergeDept);
            }
        }
    }


}
