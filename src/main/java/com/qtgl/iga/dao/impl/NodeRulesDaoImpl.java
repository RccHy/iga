package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bean.NodeDto;
import com.qtgl.iga.bo.NodeRules;
import com.qtgl.iga.bo.UpstreamTypeField;
import com.qtgl.iga.dao.NodeRulesDao;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;


import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;


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
        StringBuffer sql = new StringBuffer("select id, node_id as nodeId,type,active,inherit," +
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

    @Override
    public NodeDto saveNodeRules(NodeDto nodeDto) {

        String str = "insert into t_mgr_node_rules values(?,?,?,?,?,?,?,?,?,?,?)";
        for (NodeRules nodeRules : nodeDto.getNodeRules()) {
            nodeRules.setId(UUID.randomUUID().toString().replace("-", ""));
            nodeRules.setNodeId(nodeDto.getId());
            nodeRules.setActiveTime(new Date().getTime());
            nodeRules.setCreateTime(new Date().getTime());
            nodeRules.setUpdateTime(null);
        }
        List<NodeRules> nodeRules = nodeDto.getNodeRules();
        int[] ints = jdbcIGA.batchUpdate(str, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                preparedStatement.setObject(1, nodeRules.get(i).getId());
                preparedStatement.setObject(2, nodeRules.get(i).getNodeId());
                preparedStatement.setObject(3, nodeRules.get(i).getType());
                preparedStatement.setObject(4, nodeRules.get(i).isActive());
                preparedStatement.setObject(5, nodeRules.get(i).getActiveTime());
                preparedStatement.setObject(6, nodeRules.get(i).getCreateTime());
                preparedStatement.setObject(7, null);
                preparedStatement.setObject(8, nodeRules.get(i).getServiceKey());
                preparedStatement.setObject(9, nodeRules.get(i).getUpstreamTypesId());
                preparedStatement.setObject(10, nodeRules.get(i).getInherit());
                preparedStatement.setObject(11, nodeRules.get(i).getSort());
            }

            @Override
            public int getBatchSize() {
                return nodeRules.size();
            }
        });
        Boolean contains = ints.toString().contains("-1");
        return contains? null : nodeDto;
    }
}
