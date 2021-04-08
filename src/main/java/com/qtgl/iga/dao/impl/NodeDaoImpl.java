package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bean.NodeDto;
import com.qtgl.iga.bo.Node;
import com.qtgl.iga.dao.NodeDao;
import com.qtgl.iga.utils.FilterCodeEnum;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.util.*;


@Repository
public class NodeDaoImpl implements NodeDao {

    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;


    @Override
    public NodeDto save(NodeDto node) {

        String sql = "insert into t_mgr_node  (id,manual,node_code,create_time,update_time,domain,dept_tree_type,status)values(?,?,?,?,?,?,?,?)";
        //生成主键和时间
        if (null == node.getId()) {
            String id = UUID.randomUUID().toString().replace("-", "");
            node.setId(id);
        }
        node.setCreateTime(null == node.getCreateTime() ? System.currentTimeMillis() : node.getCreateTime());
        int update = jdbcIGA.update(sql, preparedStatement -> {
            preparedStatement.setObject(1, node.getId());
            preparedStatement.setObject(2, node.getManual());
            preparedStatement.setObject(3, node.getNodeCode());
            preparedStatement.setObject(4, new Timestamp(node.getCreateTime()));
            preparedStatement.setObject(5, null == node.getUpdateTime() ? null : new Timestamp(node.getUpdateTime()));
            preparedStatement.setObject(6, node.getDomain());
            preparedStatement.setObject(7, node.getDeptTreeType());
            preparedStatement.setObject(8, node.getStatus());

        });
        return update > 0 ? node : null;
    }

    @Override
    public List<Node> getByCode(String domain, String deptTreeType, String nodeCode, Integer status) {
        List<Node> nodes = new ArrayList<>();

        String sql = "select id,manual," +
                "node_code as nodeCode," +
                "create_time as createTime,update_time as updateTime,domain,dept_tree_type as deptTreeType,status" +
                " from t_mgr_node where domain=? and dept_tree_type=? and node_code=?  and status=?";
        List<Map<String, Object>> mapList = new ArrayList<>();
        if (null == nodeCode) {
            sql = sql.replace("node_code=?", "node_code is null or node_code=\"\"");
            mapList = jdbcIGA.queryForList(sql, domain, deptTreeType, status);
        }

        if (null == deptTreeType) {
            sql = sql.replace("dept_tree_type=? ", " (dept_tree_type is null or dept_tree_type=\"\") ");
            mapList = jdbcIGA.queryForList(sql, domain, nodeCode, status);
        }
        if (null != deptTreeType && null != nodeCode) {
            mapList = jdbcIGA.queryForList(sql, domain, deptTreeType, nodeCode, status);

        }
        for (Map<String, Object> map : mapList) {
            Node node = new Node();
            try {
                BeanUtils.populate(node, map);
                nodes.add(node);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return nodes;
    }

    @Override
    public Integer deleteNode(Map<String, Object> arguments, String domain) {
        Object[] params = new Object[2];
        params[0] = arguments.get("id");
        params[1] = domain;
        String sql = "delete from t_mgr_node where  id = ? and domain = ? ";

        return jdbcIGA.update(sql, params);
    }

    @Override
    public List<Node> findNodes(Map<String, Object> arguments, String domain) {
        ArrayList<Node> nodes = new ArrayList<>();
        Integer status = (Integer) arguments.get("status");
        String sql = "select id,manual," +
                "node_code as nodeCode," +
                "create_time as createTime,update_time as updateTime,domain,dept_tree_type as deptTreeType,status" +
                " from t_mgr_node where domain= ?   ";
        //拼接sql
        StringBuffer stb = new StringBuffer(sql);
        //存入参数
        List<Object> param = new ArrayList<>();
        param.add(domain);
        if (null != status) {
            stb.append(" and status =?  ");
            param.add(status);
        }
        dealData(arguments, stb, param);
//        getChild(arguments,param,stb);
        System.out.println(stb.toString());
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(stb.toString(), param.toArray());

        for (Map<String, Object> map : mapList) {
            try {
                Node node = new Node();
                BeanUtils.populate(node, map);
                nodes.add(node);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return nodes;
    }

    @Override
    public List<Node> findByTreeTypeId(String id, Integer status) {
        ArrayList<Node> nodes = new ArrayList<>();

        String sql = "select id,manual," +
                "node_code as nodeCode," +
                "create_time as createTime,update_time as updateTime,domain,dept_tree_type as deptTreeType,status" +
                " from t_mgr_node where dept_tree_type= ? and status=?";
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, id, status);

        return getNodes(nodes, mapList);
    }

    @Override
    public List<Node> findNodesPlus(Map<String, Object> arguments, String id) {
        ArrayList<Node> nodes = new ArrayList<>();

        List<String> codes = (List<String>) arguments.get("codes");
        String treeType = (String) arguments.get("treeType");
        Integer status = (Integer) arguments.get("status");
        String sql = "select id,manual," +
                "node_code as nodeCode," +
                "create_time as createTime,update_time as updateTime,domain,dept_tree_type as deptTreeType, status" +
                " from t_mgr_node where domain= ? and status=?  ";
        List<Object> param = new ArrayList<>();
        param.add(id);
        param.add(status);
        if (StringUtils.isNotBlank(treeType)) {
            sql = sql + " and dept_tree_type =?  ";
            param.add(treeType);
        } else {
            sql = sql + " and (dept_tree_type is null or dept_tree_type= \"\")";
        }
        sql = sql + "and node_code in (";

        sql = handleSql(sql, codes, param);
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, param.toArray());

        return getNodes(nodes, mapList);
    }

    private List<Node> getNodes(ArrayList<Node> nodes, List<Map<String, Object>> mapList) {
        for (Map<String, Object> map : mapList) {
            try {
                Node node = new Node();
                BeanUtils.populate(node, map);
                nodes.add(node);


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return nodes;

    }

    private String handleSql(String sql, List<String> codes, List<Object> param) {
        StringBuffer stb = new StringBuffer(sql);
        for (String code : codes) {
            stb.append(" ?,");
            param.add(code);
        }
        String s = stb.toString();
        s = s.substring(0, s.length() - 1) + ")";
        return s;
    }


    private void dealData(Map<String, Object> arguments, StringBuffer stb, List<Object> param) {
        Iterator<Map.Entry<String, Object>> it = arguments.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            if ("id".equals(entry.getKey())) {
                stb.append("and id= ? ");
                param.add(entry.getValue());
            }
            if ("filter".equals(entry.getKey())) {
                HashMap<String, Object> map = (HashMap<String, Object>) entry.getValue();
                for (Map.Entry<String, Object> str : map.entrySet()) {
                    if (str.getKey().equals("deptTreeType")) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "=")) {
                                stb.append("and dept_tree_type " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ? ");
                                param.add(soe.getValue());
                            }
                        }


                    }
                    if (str.getKey().equals("nodeCode")) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "=")) {
                                stb.append("and node_code " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ? ");
                                param.add(soe.getValue());
                            }
                        }


                    }


                }

            }

//            System.out.println("key = " + entry.getKey() + ", value = " + entry.getValue());
        }
    }


}
