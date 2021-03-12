package com.qtgl.iga.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TreeUtil {


    /**
     * 获取树结构中哪些是根节点【】
     *
     * @param jsonArray
     * @return
     */
    public static List<String> getTreeRootCode(JSONArray jsonArray) throws Exception {
        List<JSONObject> treeList = jsonArray.toJavaList(JSONObject.class);

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

    public static Map<String, List<JSONObject>> groupChildren(JSONArray jsonArray)  {
        List<JSONObject> treeList = jsonArray.toJavaList(JSONObject.class);

        Map<String, List<JSONObject>> map = treeList.stream().
                collect(Collectors.groupingBy(jsonObject -> jsonObject.getString(TreeEnum.PARENTCODE.getCode())));
        return map;
    }


}
