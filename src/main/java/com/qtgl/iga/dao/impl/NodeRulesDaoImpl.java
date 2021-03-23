package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bean.NodeDto;
import com.qtgl.iga.bo.NodeRules;
import com.qtgl.iga.dao.NodeRulesDao;
import com.qtgl.iga.vo.NodeRulesVo;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Repository;


import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;


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
        StringBuffer sql = new StringBuffer("select id, node_id as nodeId,type,active,inherit_id as inheritId," +
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
            } catch (IllegalAccessException | InvocationTargetException e) {
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
            if (null != nodeRules.getId()) {
                nodeRules.setCreateTime(nodeDto.getCreateTime());
                nodeRules.setUpdateTime(nodeDto.getUpdateTime());

            } else {
                nodeRules.setId(UUID.randomUUID().toString().replace("-", ""));
                nodeRules.setCreateTime(new Date().getTime());
                nodeRules.setUpdateTime(null);
            }
            nodeRules.setNodeId(nodeDto.getId());


        }
        List<NodeRulesVo> nodeRules = nodeDto.getNodeRules();
        int[] ints = jdbcIGA.batchUpdate(str, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                preparedStatement.setObject(1, nodeRules.get(i).getId());
                preparedStatement.setObject(2, nodeRules.get(i).getNodeId());
                preparedStatement.setObject(3, nodeRules.get(i).getType());
                preparedStatement.setObject(4, nodeRules.get(i).getActive());
                preparedStatement.setObject(5, null == nodeRules.get(i).getActiveTime() ? null : new Timestamp(nodeRules.get(i).getActiveTime()));
                preparedStatement.setObject(6, new Timestamp(nodeRules.get(i).getCreateTime()));
                preparedStatement.setObject(7, null == nodeRules.get(i).getUpdateTime() ? null : new Timestamp(nodeRules.get(i).getUpdateTime()));
                preparedStatement.setObject(8, nodeRules.get(i).getServiceKey());
                preparedStatement.setObject(9, nodeRules.get(i).getUpstreamTypesId());
                preparedStatement.setObject(10, nodeRules.get(i).getInheritId());
                preparedStatement.setObject(11, nodeRules.get(i).getSort());
            }

            @Override
            public int getBatchSize() {
                return nodeRules.size();
            }
        });
        boolean contains = Arrays.toString(ints).contains("-1");
        return contains ? null : nodeDto;
    }

    @Override
    public List<NodeRules> findNodeRules(Map<String, Object> arguments) {
        String sql = "select id,node_id as nodeId,type as type,active as active," +
                "create_time as createTime,service_key as serviceKey,upstream_types_id as upstreamTypesId,inherit_id as inheritId," +
                "active_time as activeTime,update_time as updateTime from t_mgr_node_rules where 1 = 1 ";
        //拼接sql
        StringBuffer stb = new StringBuffer(sql);
        //存入参数
        List<Object> param = new ArrayList<>();

        dealData(arguments, stb, param);

        List<Map<String, Object>> mapList = jdbcIGA.queryForList(stb.toString(), param.toArray());

        ArrayList<NodeRules> list = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {
                NodeRules nodeRules = new NodeRules();
                try {
                    BeanUtils.populate(nodeRules, map);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
                list.add(nodeRules);
            }
            return list;
        }
        return null;
    }

    @Override
    public NodeRules updateRules(NodeRules nodeRules) {

        String sql = "update t_mgr_node_rules  set node_id = ?,type = ?,active = ?,active_time = ?,create_time = ?," +
                "update_time = ?,service_key = ?,upstream_types_id = ?,inherit_id= ? ,sort=? where id=?";
        nodeRules.setUpdateTime(new Date().getTime());
        int update = jdbcIGA.update(sql, preparedStatement -> {
            preparedStatement.setObject(1, nodeRules.getNodeId());
            preparedStatement.setObject(2, nodeRules.getType());
            preparedStatement.setObject(3, nodeRules.getActive());
            preparedStatement.setObject(4, nodeRules.getActiveTime());
            preparedStatement.setObject(5, nodeRules.getCreateTime());
            preparedStatement.setObject(6, new Timestamp(nodeRules.getUpdateTime()));
            preparedStatement.setObject(7, nodeRules.getServiceKey());
            preparedStatement.setObject(8, nodeRules.getUpstreamTypesId());
            preparedStatement.setObject(9, nodeRules.getInheritId());
            preparedStatement.setObject(10, nodeRules.getSort());
            preparedStatement.setObject(11, nodeRules.getId());
        });
        return update > 0 ? nodeRules : null;
    }

    @Override
    public Integer deleteNodeRules(String id) {
        Object[] params = new Object[1];
        params[0] = id;
        String sql = "delete from t_mgr_node_rules where  node_id = ? ";

        return jdbcIGA.update(sql, params);
    }

    @Override
    public List<NodeRulesVo> findNodeRulesByNodeId(String id) {
        String sql = "select id,node_id as nodeId,type as type,active as active," +
                "create_time as createTime,service_key as serviceKey,upstream_types_id as upstreamTypesId,inherit_id as inheritId," +
                "active_time as activeTime,update_time as updateTime from t_mgr_node_rules where node_id =? ";

        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, id);
        ArrayList<NodeRulesVo> list = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {
                NodeRulesVo nodeRules = new NodeRulesVo();
                try {
                    BeanUtils.populate(nodeRules, map);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
                list.add(nodeRules);
            }
            return list;
        }
        return null;
    }

    @Override
    public NodeRulesVo saveNodeRules(NodeRulesVo nodeRules) {
        String str = "insert into t_mgr_node_rules values(?,?,?,?,?,?,?,?,?,?,?)";
        nodeRules.setId(UUID.randomUUID().toString().replace("-", ""));
        nodeRules.setCreateTime(new Date().getTime());
        nodeRules.setUpdateTime(null);
        return jdbcIGA.update(str, preparedStatement -> {
            preparedStatement.setObject(1, nodeRules.getId());
            preparedStatement.setObject(2, nodeRules.getNodeId());
            preparedStatement.setObject(3, nodeRules.getType());
            preparedStatement.setObject(4, nodeRules.getActive());
            preparedStatement.setObject(5, null);
            preparedStatement.setObject(6, new Timestamp(nodeRules.getCreateTime()));
            preparedStatement.setObject(7, null);
            preparedStatement.setObject(8, nodeRules.getServiceKey());
            preparedStatement.setObject(9, nodeRules.getUpstreamTypesId());
            preparedStatement.setObject(10, nodeRules.getInheritId());
            preparedStatement.setObject(11, nodeRules.getSort());
        }) > 0 ? nodeRules : null;


    }

    @Override
    public NodeRules findNodeRulesById(String id) {
        String sql = "select id,node_id as nodeId,type as type,active as active," +
                "create_time as createTime,service_key as serviceKey,upstream_types_id as upstreamTypesId,inherit_id as inheritId," +
                "active_time as activeTime,update_time as updateTime from t_mgr_node_rules where 1 = 1 and id= ? ";
        Map<String, Object> mapList = jdbcIGA.queryForMap(sql, id);
        NodeRules nodeRules = new NodeRules();

        if (null != mapList) {
            try {
                BeanUtils.populate(nodeRules, mapList);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }

            return nodeRules;
        }
        return null;
    }

    @Override
    public Integer deleteNodeRulesById(String id) {
        Object[] params = new Object[1];
        params[0] = id;
        String sql = "delete from t_mgr_node_rules where  id = ? ";

        return jdbcIGA.update(sql, params);
    }


    private void dealData(Map<String, Object> arguments, StringBuffer stb, List<Object> param) {
        Iterator<Map.Entry<String, Object>> it = arguments.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            if (entry.getKey().equals("id")) {
                stb.append("and id= ? ");
                param.add(entry.getValue());
            }


        }
    }

}
