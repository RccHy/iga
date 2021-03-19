package com.qtgl.iga.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.DeptBean;

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

    public static  Map<String, List<DeptBean>> groupChildren(List<DeptBean> deptBeans) {
        Map<String, List<DeptBean>> map = deptBeans.stream().
                collect(Collectors.groupingBy(DeptBean::getParentCode));
        return map;
    }

    public static Map<String, JSONObject> toMap(JSONArray jsonArray) {
        List<JSONObject> treeList = jsonArray.toJavaList(JSONObject.class);

        Map<String, JSONObject> map = treeList.stream()
                .collect(Collectors.toMap(
                        (code -> code.getString(TreeEnum.CODE.getCode())),
                        (value -> value)));


        return map;
    }


}
