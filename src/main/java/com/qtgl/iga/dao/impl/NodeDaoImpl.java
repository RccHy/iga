package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.Node;
import com.qtgl.iga.dao.NodeDao;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Repository
public class NodeDaoImpl implements NodeDao {

    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;


    @Override
    public Node save(Node node) {
        return null;
    }

    @Override
    public List<Node> getByCode(String domain, String nodeCode) {
        List<Node> nodes = new ArrayList<>();

        String sql = "select id,inherit,manual," +
                "node_code as nodeCode," +
                "create_time as createTime,update_time as updateTime,domain" +
                " from t_mgr_node where domain=? and node_code=?";
        List<Map<String, Object>> mapList = new ArrayList<>();
        if (null == nodeCode) {
            sql = sql.replace("node_code=?", "node_code is null or node_code=\"\"");
            mapList = jdbcIGA.queryForList(sql, domain);
        } else {
            mapList = jdbcIGA.queryForList(sql, domain, nodeCode);
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


}
