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

        String sql = "insert into t_mgr_node  values(?,?,?,?,?,?,?)";
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

        });
        return update > 0 ? node : null;
    }

    @Override
    public List<Node> getByCode(String domain, String deptTreeType, String nodeCode) {
        List<Node> nodes = new ArrayList<>();

        String sql = "select id,manual," +
                "node_code as nodeCode," +
                "create_time as createTime,update_time as updateTime,domain,dept_tree_type as deptTreeType" +
                " from t_mgr_node where domain=? and dept_tree_type=? and node_code=?";
        List<Map<String, Object>> mapList = new ArrayList<>();
        if (null == nodeCode) {
            sql = sql.replace("node_code=?", "node_code is null or node_code=\"\"");
            mapList = jdbcIGA.queryForList(sql, domain, deptTreeType);
        }

        if (null == deptTreeType) {
            sql = sql.replace("dept_tree_type=? ", " (dept_tree_type is null or dept_tree_type=\"\") ");
            mapList = jdbcIGA.queryForList(sql, domain, nodeCode);
        }
        if (null != deptTreeType && null != nodeCode) {
            mapList = jdbcIGA.queryForList(sql, domain, deptTreeType, nodeCode);

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
        String sql = "delete from t_mgr_node where  id = ? and domain =?";

        return jdbcIGA.update(sql, params);
    }

    @Override
    public List<Node> findNodes(Map<String, Object> arguments, String domain) {
        ArrayList<Node> nodes = new ArrayList<>();

        String sql = "select id,manual," +
                "node_code as nodeCode," +
                "create_time as createTime,update_time as updateTime,domain,dept_tree_type as deptTreeType" +
                " from t_mgr_node where domain= ? ";
        //拼接sql
        StringBuffer stb = new StringBuffer(sql);
        //存入参数
        List<Object> param = new ArrayList<>();
        param.add(domain);
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
    public List<Node> findByTreeTypeId(String id) {
        ArrayList<Node> nodes = new ArrayList<>();

        String sql = "select id,manual," +
                "node_code as nodeCode," +
                "create_time as createTime,update_time as updateTime,domain,dept_tree_type as deptTreeType" +
                " from t_mgr_node where dept_tree_type= ? ";
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql,id);

        for (Map<String, Object> map : mapList) {
            try {
                Node node = new Node();
                BeanUtils.populate(node, map);
                nodes.add(node);
                return nodes;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }


    private void dealData(Map<String, Object> arguments, StringBuffer stb, List<Object> param) {
        Iterator<Map.Entry<String, Object>> it = arguments.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            if (entry.getKey().equals("id")) {
                stb.append("and id= ? ");
                param.add(entry.getValue());
            }
            if (entry.getKey().equals("filter")) {
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
