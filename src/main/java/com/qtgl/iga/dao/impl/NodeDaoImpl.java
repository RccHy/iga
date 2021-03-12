package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.Node;
import com.qtgl.iga.dao.NodeDao;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
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
    public Node getByCode(String domain, String nodeCode) {
        try {
            Node node = new Node();
            String sql = "select id,inherit,manual," +
                    "node_code as nodeCode," +
                    "create_time as createTime,update_time as updateTime,domain" +
                    " from t_mgr_node where domain=? and node_code=?";
            Map<String, Object> map = null;
            if (null == nodeCode) {
                sql = sql.replace("node_code=?", "node_code is null or node_code=\"\"");
                map = jdbcIGA.queryForMap(sql, domain);
            } else {
                map = jdbcIGA.queryForMap(sql, domain, nodeCode);
            }
            BeanUtils.populate(node, map);
            return node;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (EmptyResultDataAccessException e) {
            e.printStackTrace();

        }
        return null;
    }


}
