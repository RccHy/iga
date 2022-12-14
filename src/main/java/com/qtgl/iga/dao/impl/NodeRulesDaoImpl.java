package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bean.NodeDto;
import com.qtgl.iga.bo.NodeRules;
import com.qtgl.iga.dao.NodeRulesDao;
import com.qtgl.iga.utils.MyBeanUtils;
import com.qtgl.iga.vo.NodeRulesVo;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
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
    public List<NodeRules> getByNodeAndType(String nodeId, Integer type, Boolean active, Integer status) {
        List<NodeRules> nodeRules = null;
        List<Object> para = new ArrayList<>();
        para.add(nodeId);
        //para.add(active);
        StringBuffer sql = new StringBuffer("select id, node_id as nodeId,type,active,inherit_id as inheritId," +
                " active_time as activeTime, create_time as createTime,  update_time as updateTime," +
                "service_key as serviceKey,upstream_types_id as upstreamTypesId,sort,status " +
                " from t_mgr_node_rules where node_id=?  ");
        if (null != active) {
            sql.append(" and active=? ");
            para.add(active);
        }
        if (null != type) {
            sql.append(" and type=? ");
            para.add(type);
        }
        if (null != status) {
            sql.append(" and status = ? ");
            para.add(status);
        }
        sql.append(" order by sort asc");
        List<Map<String, Object>> maps = jdbcIGA.queryForList(sql.toString(), para.toArray());
        if (null != maps && maps.size() > 0) {
            nodeRules = new ArrayList<>();
            for (Map<String, Object> map : maps) {
                NodeRules nodeRule = new NodeRules();
                try {
                    MyBeanUtils.populate(nodeRule, map);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }

                nodeRules.add(nodeRule);
            }
        }
        return nodeRules;
    }

    @Override
    public NodeDto saveNodeRules(NodeDto nodeDto) {

        String str = "insert into t_mgr_node_rules (id,node_id,type,active,active_time,create_time,update_time" +
                ",service_key,upstream_types_id,inherit_id,sort,status)" +
                "values(?,?,?,?,?,?,?,?,?,?,?,?)";
        if (null != nodeDto.getNodeRules() && nodeDto.getNodeRules().size() > 0) {
            for (NodeRules nodeRules : nodeDto.getNodeRules()) {
                if (null != nodeRules.getId()) {
                    nodeRules.setCreateTime(nodeDto.getCreateTime());
                    nodeRules.setUpdateTime(nodeDto.getUpdateTime());

                } else {
                    nodeRules.setId(UUID.randomUUID().toString().replace("-", ""));
                    nodeRules.setCreateTime(nodeDto.getCreateTime());
                    nodeRules.setUpdateTime(null);
                }
                nodeRules.setNodeId(nodeDto.getId());


            }
        }
        //todo ????????????
        List<NodeRulesVo> nodeRules = nodeDto.getNodeRules();
        int[] ints = jdbcIGA.batchUpdate(str, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                preparedStatement.setObject(1, nodeRules.get(i).getId());
                preparedStatement.setObject(2, nodeRules.get(i).getNodeId());
                preparedStatement.setObject(3, nodeRules.get(i).getType());
                preparedStatement.setObject(4, nodeRules.get(i).getActive());
                preparedStatement.setObject(5, null);
                preparedStatement.setObject(6, new Timestamp(nodeRules.get(i).getCreateTime()));
                preparedStatement.setObject(7, null);
                preparedStatement.setObject(8, nodeRules.get(i).getServiceKey());
                preparedStatement.setObject(9, nodeRules.get(i).getUpstreamTypesId());
                preparedStatement.setObject(10, nodeRules.get(i).getInheritId());
                preparedStatement.setObject(11, nodeRules.get(i).getSort());
                preparedStatement.setObject(12, nodeRules.get(i).getStatus());
            }

            @Override
            public int getBatchSize() {
                assert nodeRules != null;
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
                "active_time as activeTime,update_time as updateTime from t_mgr_node_rules where 1 = 1 and status =?";
        //??????sql
        StringBuffer stb = new StringBuffer(sql);
        //????????????
        List<Object> param = new ArrayList<>();
        Integer status = (Integer) arguments.get("status");

        param.add(status);
        dealData(arguments, stb, param);

        List<Map<String, Object>> mapList = jdbcIGA.queryForList(stb.toString(), param.toArray());

        ArrayList<NodeRules> list = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {
                NodeRules nodeRules = new NodeRules();
                try {
                    MyBeanUtils.populate(nodeRules, map);
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
                "update_time = ?,service_key = ?,upstream_types_id = ?,inherit_id= ? ,sort=?,status =? where id=?";
        nodeRules.setUpdateTime(System.currentTimeMillis());
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
            preparedStatement.setObject(11, nodeRules.getStatus());
            preparedStatement.setObject(12, nodeRules.getId());
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
    public List<NodeRulesVo> findNodeRulesByNodeId(String id, Integer status) {
        String sql = "select id,node_id as nodeId,type as type,active as active," +
                "create_time as createTime,service_key as serviceKey,upstream_types_id as upstreamTypesId,inherit_id as inheritId," +
                "active_time as activeTime,update_time as updateTime , sort ,status from t_mgr_node_rules where node_id =? ";
        List<Object> param = new ArrayList<>();
        param.add(id);
        if (null != status) {
            sql = sql + "and status =? ";
            param.add(status);
        }
        sql = sql + "order by sort asc";
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, param.toArray());
        ArrayList<NodeRulesVo> list = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {
                NodeRulesVo nodeRules = new NodeRulesVo();
                try {
                    MyBeanUtils.populate(nodeRules, map);
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
        String str = "insert into t_mgr_node_rules (id,node_id,type,active,active_time,create_time,update_time,service_key,upstream_types_id,inherit_id,sort,status) values(?,?,?,?,?,?,?,?,?,?,?,?)";
        nodeRules.setId(UUID.randomUUID().toString().replace("-", ""));
        nodeRules.setCreateTime(System.currentTimeMillis());
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
            preparedStatement.setObject(12, nodeRules.getStatus());
        }) > 0 ? nodeRules : null;


    }

    @Override
    public NodeRules findNodeRulesById(String id, Integer status) {
        String sql = "select id,node_id as nodeId,type as type,active as active," +
                "create_time as createTime,service_key as serviceKey,upstream_types_id as upstreamTypesId,inherit_id as inheritId," +
                "active_time as activeTime,update_time as updateTime,sort,status from t_mgr_node_rules where 1 = 1 and id= ?  ";
        List<Object> param = new ArrayList<>();
        param.add(id);
        if (null != status) {
            sql = sql + "and status =? ";
            param.add(status);
        }
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, param.toArray());
        ArrayList<NodeRules> list = new ArrayList<>();

        if (null != mapList && mapList.size() > 0) {
            try {
                for (Map<String, Object> map : mapList) {
                    NodeRules nodeRules = new NodeRules();
                    MyBeanUtils.populate(nodeRules, map);
                    list.add(nodeRules);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }

            return list.get(0);
        }
        return null;
    }

    @Override
    public Integer deleteNodeRulesById(String id) {
        Object[] params = new Object[2];
        params[0] = id;
        params[1] = id;
        //  ??????rule
        String sql = "delete from t_mgr_node_rules where  id = ? or inherit_id= ?";

        return jdbcIGA.update(sql, params);
    }

    @Override
    public List<NodeRules> findNodeRulesByUpStreamTypeId(String id, Integer status) {
        List<NodeRules> nodeRules = new ArrayList<>();
        String sql = "select id,node_id as nodeId,type as type,active as active," +
                "create_time as createTime,service_key as serviceKey,upstream_types_id as upstreamTypesId,inherit_id as inheritId," +
                "active_time as activeTime,update_time as updateTime,sort,status from t_mgr_node_rules where 1 = 1 and upstream_types_id= ?  ";
        List<Object> param = new ArrayList<>();
        param.add(id);
        if (null != status) {
            sql = sql + "and status =? ";
            param.add(status);
        } else {
            sql = sql + "and status !=? ";
            param.add(2);
        }
        List<Map<String, Object>> maps = jdbcIGA.queryForList(sql, param.toArray());
        if (null != maps && maps.size() > 0) {
            for (Map<String, Object> map : maps) {
                NodeRules nodeRule = new NodeRules();
                try {
                    MyBeanUtils.populate(nodeRule, map);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
                nodeRules.add(nodeRule);
            }
            return nodeRules;
        }
        return null;
    }

    @Override
    public Integer makeNodeRulesToHistory(String id, Integer status) {
        String str = "update t_mgr_node_rules set  status= ? " +
                "where id = ?  ";
        int update = jdbcIGA.update(str, preparedStatement -> {

            preparedStatement.setObject(1, status);
            preparedStatement.setObject(2, id);
        });
        return update;
    }

    @Override
    public List<NodeRules> findNodeRulesByServiceKey(String id, Integer status, Integer type) {
        List<NodeRules> nodeRules = new ArrayList<>();
        String sql = "select id,node_id as nodeId,type as type,active as active," +
                "create_time as createTime,service_key as serviceKey,upstream_types_id as upstreamTypesId,inherit_id as inheritId," +
                "active_time as activeTime,update_time as updateTime,sort,status from t_mgr_node_rules where 1 = 1 and service_key= ? and type=? and active=true  ";
        List<Object> param = new ArrayList<>();
        param.add(id);
        param.add(type);
        if (null != status) {
            sql = sql + "and status =? ";
            param.add(status);
        } else {
            sql = sql + "and status !=? ";
            param.add(2);
        }
        List<Map<String, Object>> maps = jdbcIGA.queryForList(sql, param.toArray());
        if (null != maps && maps.size() > 0) {
            for (Map<String, Object> map : maps) {
                NodeRules nodeRule = new NodeRules();
                try {
                    MyBeanUtils.populate(nodeRule, map);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
                nodeRules.add(nodeRule);
            }
            return nodeRules;
        }
        return null;
    }

    @Override
    public List<NodeRulesVo> findPullNodeRulesByNodeId(String id, Integer status) {
        String sql = "select id,node_id as nodeId,type as type,active as active," +
                "create_time as createTime,service_key as serviceKey,upstream_types_id as upstreamTypesId,inherit_id as inheritId," +
                "active_time as activeTime,update_time as updateTime , sort ,status from t_mgr_node_rules where node_id =? and type= 1 ";
        List<Object> param = new ArrayList<>();
        param.add(id);
        if (null != status) {
            sql = sql + "and status =? ";
            param.add(status);
        }
        sql = sql + "order by sort asc";
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, param.toArray());
        ArrayList<NodeRulesVo> list = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {
                NodeRulesVo nodeRules = new NodeRulesVo();
                try {
                    MyBeanUtils.populate(nodeRules, map);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
                list.add(nodeRules);
            }
            return list;
        }
        return null;
    }


    private void dealData(Map<String, Object> arguments, StringBuffer stb, List<Object> param) {
        Iterator<Map.Entry<String, Object>> it = arguments.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            if ("id".equals(entry.getKey())) {
                stb.append("and id= ? ");
                param.add(entry.getValue());
            }

        }
    }

}
