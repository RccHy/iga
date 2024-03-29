package com.qtgl.iga.dao.impl;

import com.qtgl.iga.AutoUpRunner;
import com.qtgl.iga.bean.NodeDto;
import com.qtgl.iga.bo.NodeRules;
import com.qtgl.iga.dao.NodeRulesDao;
import com.qtgl.iga.utils.MyBeanUtils;
import com.qtgl.iga.vo.NodeRulesVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

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
                nodeRules.setStatus(nodeDto.getStatus());
            }
        }
        // 修改时间
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

    //todo 查询规则 超级租户处理
    @Override
    public List<NodeRules> findNodeRules(Map<String, Object> arguments, String domain) {
        String sql = " SELECT " +
                " r.id, " +
                " r.node_id AS nodeId, " +
                " r.type AS type, " +
                " r.active AS active, " +
                " r.create_time AS createTime, " +
                " r.service_key AS serviceKey, " +
                " r.upstream_types_id AS upstreamTypesId, " +
                " r.inherit_id AS inheritId, " +
                " r.active_time AS activeTime, " +
                " r.update_time AS updateTime  " +
                " FROM " +
                " t_mgr_node_rules r, " +
                " t_mgr_node n  " +
                " WHERE " +
                " 1 = 1  " +
                " AND r.`status` = ?  " +
                " AND r.node_id = n.id  " +
                " AND n.domain =? ";
        //拼接sql
        StringBuffer stb = new StringBuffer(sql);
        //存入参数
        List<Object> param = new ArrayList<>();
        Integer status = (Integer) arguments.get("status");
        Integer type = (Integer) arguments.get("type");
        param.add(status);
        param.add(domain);
        if (null != type) {
            stb.append("AND r.type=? ");
            param.add(type);

        }
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


    public List<NodeRules> findNodeRulesByService(String serviceName, String domain, String synType) {
        String sql = "select tmnr.* " +
                "from t_mgr_upstream_types ut " +
                "left join t_mgr_upstream u on ut.upstream_id=u.id " +
                "left join t_mgr_node_rules tmnr on ut.id = tmnr.upstream_types_id " +
                " where graphql_url like ? " +
                "  and ut.active = 1 " +
                "  and u.active=1 " +
                "  and syn_way = 1 " +
                "   and domain in ? " +
                "  and syn_type = ? ";
        //存入参数
        List<Object> param = new ArrayList<>();
        param.add("bus://" + serviceName + "/%");
        param.add(Arrays.asList(domain, "localhost"));
        param.add(synType);
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, param);
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

        if (null != mapList && mapList.size() > 0) {
            ArrayList<NodeRules> list = new ArrayList<>();

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
        //  删除rule
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
    public List<NodeRules> findNodeRulesByServiceKey(String serviceKey, Integer status, Integer type) {
        List<NodeRules> nodeRules = new ArrayList<>();
        String sql = "select id,node_id as nodeId,type as type,active as active," +
                "create_time as createTime,service_key as serviceKey,upstream_types_id as upstreamTypesId,inherit_id as inheritId," +
                "active_time as activeTime,update_time as updateTime,sort,status from t_mgr_node_rules where 1 = 1 and service_key= ? and type=? and active=true  ";
        List<Object> param = new ArrayList<>();
        param.add(serviceKey);
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

    @Override
    public List<NodeRulesVo> findNodeRulesByUpStreamTypeIdsAndType(List<String> ids, String type, String domain, Integer status) {

        StringBuffer stb = new StringBuffer("SELECT " +
                " r.id, " +
                " r.node_id AS nodeId, " +
                " r.type AS type, " +
                " r.active AS active, " +
                " r.create_time AS createTime, " +
                " r.service_key AS serviceKey, " +
                " r.upstream_types_id AS upstreamTypesId, " +
                " r.inherit_id AS inheritId, " +
                " r.active_time AS activeTime, " +
                " r.update_time AS updateTime, " +
                " r.sort, " +
                " r.STATUS, " +
                "CASE " +
                " WHEN di1.upstream_id IS NOT NULL THEN  1" +
                " WHEN di2.node_rule_id IS NOT NULL THEN 2 " +
                " WHEN u.active = FALSE THEN 3" +
                " WHEN r.active = FALSE THEN 4 " +
                " ELSE 0  " +
                " END AS runningStatus  " +
                "FROM " +
                " t_mgr_node_rules r " +
                " JOIN t_mgr_node n ON r.node_id = n.id  " +
                " AND r.type = 1  " +
                " AND n.type = ?" +
                " AND r.STATUS = ?" +
                " AND r.upstream_types_id IN ( ");
        //存入参数
        List<Object> param = new ArrayList<>();
        param.add(type);
        param.add(status);
        for (String id : ids) {
            stb.append("?,");
            param.add(id);
        }
        stb.replace(stb.length() - 1, stb.length(), " ) ");
        stb.append(" and n.domain in (?");
        param.add(domain);
        if (StringUtils.isNotBlank(AutoUpRunner.superDomainId)) {
            stb.append(",? ");
            param.add(AutoUpRunner.superDomainId);
        }
        stb.append(" ) ");
        stb.append("JOIN t_mgr_upstream_types ut ON ( r.upstream_types_id = ut.id OR r.service_key = ut.id ) " +
                " JOIN t_mgr_upstream u ON ut.upstream_id = u.id " +
                "LEFT JOIN t_mgr_domain_ignore di1 ON di1.upstream_id = u.id " +
                "LEFT JOIN t_mgr_domain_ignore di2 ON di2.node_rule_id = r.id");
        stb.append(" order by r.sort");
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(stb.toString(), param.toArray());
        if (null != mapList && mapList.size() > 0) {
            List<NodeRulesVo> nodeRules = new ArrayList<>();
            for (Map<String, Object> map : mapList) {
                NodeRulesVo nodeRule = new NodeRulesVo();
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
    public List<NodeRules> findNodeRulesByDomain(String superDomainId, Integer status, String type) {
        //拼接sql
        StringBuffer stb = new StringBuffer("select r.id,r.node_id as nodeId,r.type as type,r.active as active," +
                "r.create_time as createTime,r.service_key as serviceKey,r.upstream_types_id as upstreamTypesId,r.inherit_id as inheritId," +
                "r.active_time as activeTime,r.update_time as updateTime,r.sort,r.status from t_mgr_node_rules r,t_mgr_node n where 1 = 1  and  r.node_id = n.id and n.type =? " +
                "and r.status= ? and n.domain=? ");
        //存入参数
        List<Object> param = new ArrayList<>();
        param.add(type);
        param.add(status);
        param.add(superDomainId);

        List<Map<String, Object>> mapList = jdbcIGA.queryForList(stb.toString(), param.toArray());
        if (null != mapList && mapList.size() > 0) {
            List<NodeRules> nodeRules = new ArrayList<>();
            for (Map<String, Object> map : mapList) {
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
    public List<NodeRulesVo> findNodeRulesByNodeIds(List<String> nodeIds,String domain) {
        StringBuffer stb = new StringBuffer("SELECT nr.id, " +
                " nr.node_id AS nodeId, " +
                " nr.type AS type, " +
                " nr.active AS active, " +
                " nr.create_time AS createTime, " +
                " nr.service_key AS serviceKey, " +
                " nr.upstream_types_id AS upstreamTypesId, " +
                " nr.inherit_id AS inheritId, " +
                " nr.active_time AS activeTime, " +
                " nr.update_time AS updateTime, " +
                " nr.sort, " +
                " nr.status," +
                "      CASE WHEN di1.upstream_id IS NOT NULL THEN 1" +
                "           WHEN di2.node_rule_id IS NOT NULL THEN 2" +
                "           WHEN u.active = FALSE THEN 3" +
                "           WHEN nr.active = FALSE THEN 4 " +
                "           ELSE 0 END AS runningStatus " +
                " FROM t_mgr_node_rules nr " +
                "         JOIN t_mgr_upstream_types ut ON (nr.upstream_types_id = ut.id or nr.service_key = ut.id  ) " +
                "         JOIN t_mgr_upstream u ON ut.upstream_id = u.id " +
                "         LEFT JOIN t_mgr_domain_ignore di1 ON di1.upstream_id = u.id and di1.domain=?" +
                "         LEFT JOIN t_mgr_domain_ignore di2 ON di2.node_rule_id = nr.id and di2.domain=?" +
                " where nr.node_id in ( ");
        //存入参数
        List<Object> param = new ArrayList<>();
        param.add(domain);
        param.add(domain);
        for (String id : nodeIds) {
            stb.append("?,");
            param.add(id);
        }
        stb.replace(stb.length() - 1, stb.length(), " ) ");
        stb.append(" order by nr.sort");
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(stb.toString(), param.toArray());
        if (!CollectionUtils.isEmpty(mapList)) {
            List<NodeRulesVo> nodeRulesVos = new ArrayList<>();
            for (Map<String, Object> map : mapList) {
                NodeRulesVo nodeRulesVo = new NodeRulesVo();
                try {
                    MyBeanUtils.populate(nodeRulesVo, map);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
                nodeRulesVos.add(nodeRulesVo);
            }
            return nodeRulesVos;
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
