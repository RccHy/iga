package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.NodeRules;
import com.qtgl.iga.dao.NodeRulesDao;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;


import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Repository
public class NodeRulesDaoImpl implements NodeRulesDao {

    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;


    @Override
    public List<NodeRules> getByNodeAndType(String nodeId, Integer type, Boolean active) {
        List<NodeRules> nodeRules = new ArrayList<>();
        List<Object> para = new ArrayList<>();
        para.add(nodeId);
        para.add(active);
        StringBuffer sql = new StringBuffer("select id, node_id as nodeId,type,active," +
                " active_time as activeTime, create_time as createTime,  update_time as updateTime," +
                "service_key as serviceKey,upstream_types_id as upstreamTypesId,sort " +
                " from t_mgr_node_rules where node_id=? and active=? ");
        if (null != type) {
            sql.append(" and type=? ");
            para.add(type);
        }
        sql.append(" order by sort asc");
        List<Map<String, Object>> maps = jdbcIGA.queryForList(sql.toString(), para.toArray());
        for (Map<String, Object> map : maps) {
            NodeRules nodeRule = new NodeRules();
            try {
                BeanUtils.populate(nodeRule, map);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            nodeRules.add(nodeRule);
        }
        return nodeRules;
    }
}
