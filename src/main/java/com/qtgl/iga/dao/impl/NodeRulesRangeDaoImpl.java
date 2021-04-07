package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bean.NodeDto;
import com.qtgl.iga.bo.NodeRulesRange;
import com.qtgl.iga.dao.NodeRulesRangeDao;
import com.qtgl.iga.vo.NodeRulesVo;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;


@Repository
public class NodeRulesRangeDaoImpl implements NodeRulesRangeDao {


    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;

    @Override
    public List<NodeRulesRange> getByRulesId(String rulesId,Integer status) {
        try {
            List<NodeRulesRange> nodeRulesRanges = new ArrayList<>();
            List<Map<String, Object>> maps = jdbcIGA.queryForList("select " +
                            "id,node_rules_id as nodeRulesId,`type`,`rename`,node,`range`,create_time as createTime,status " +
                            "from t_mgr_node_rules_range " +
                            "where node_rules_id=? and status =? " +
                            "order by `type` desc",
                    rulesId,status);
            for (Map<String, Object> map : maps) {
                NodeRulesRange nodeRulesRange = new NodeRulesRange();
                BeanUtils.populate(nodeRulesRange, map);
                nodeRulesRanges.add(nodeRulesRange);
            }
            return nodeRulesRanges;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public NodeDto saveNodeRuleRange(NodeDto nodeDto) {
        //(id,node_rules_id,type,node,range,create_time,rename,update_time,status)
        String str = "insert into t_mgr_node_rules_range  values (?,?,?,?,?,?,?,?,?)";
        boolean contains = false;
        for (NodeRulesVo nodeRule : nodeDto.getNodeRules()) {
            List<NodeRulesRange> nodeRulesRanges = nodeRule.getNodeRulesRanges();
            for (NodeRulesRange nodeRulesRange : nodeRulesRanges) {
                if (null != nodeRulesRange.getId()) {
                    nodeRulesRange.setUpdateTime(nodeDto.getUpdateTime());
                } else {
                    nodeRulesRange.setId(UUID.randomUUID().toString().replace("-", ""));
                    nodeRulesRange.setCreateTime(nodeDto.getCreateTime());
                    nodeRulesRange.setUpdateTime(null);
                }
                nodeRulesRange.setNodeRulesId(nodeRule.getId());

            }
            int[] ints = jdbcIGA.batchUpdate(str, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                    preparedStatement.setObject(1, nodeRulesRanges.get(i).getId());
                    preparedStatement.setObject(2, nodeRulesRanges.get(i).getNodeRulesId());
                    preparedStatement.setObject(3, nodeRulesRanges.get(i).getType());
                    preparedStatement.setObject(4, nodeRulesRanges.get(i).getNode());
                    preparedStatement.setObject(5, nodeRulesRanges.get(i).getRange());
                    preparedStatement.setObject(6, null == nodeRulesRanges.get(i).getCreateTime() ? null : new Timestamp(nodeRulesRanges.get(i).getCreateTime()));
                    preparedStatement.setObject(7, nodeRulesRanges.get(i).getRename());
                    preparedStatement.setObject(8, null == nodeRulesRanges.get(i).getUpdateTime() ? null : new Timestamp(nodeRulesRanges.get(i).getUpdateTime()));
                    preparedStatement.setObject(9, nodeRulesRanges.get(i).getStatus());

                }

                @Override
                public int getBatchSize() {
                    return nodeRulesRanges.size();
                }
            });
            contains = contains || Arrays.toString(ints).contains("-1");
        }


        return contains ? null : nodeDto;
    }

    @Override
    public Integer deleteNodeRulesRangeByRuleId(String id) {
        Object[] params = new Object[1];
        params[0] = id;

        String sql = "delete from t_mgr_node_rules_range where  node_rules_id = ? ";

        return jdbcIGA.update(sql, params);
    }

    @Override
    public NodeRulesRange updateRulesRange(NodeRulesRange rulesRange) {
        String sql = "update t_mgr_node_rules_range  set node_rules_id = ?,type = ?,node = ?,range = ?,create_time = ?," +
                "rename = ?,update_time = ? ,status =? where id=?";
        Timestamp date = new Timestamp(System.currentTimeMillis());
        int update = jdbcIGA.update(sql, preparedStatement -> {
            preparedStatement.setObject(1, rulesRange.getNodeRulesId());
            preparedStatement.setObject(2, rulesRange.getType());
            preparedStatement.setObject(3, rulesRange.getNode());
            preparedStatement.setObject(4, rulesRange.getRange());
            preparedStatement.setObject(5, rulesRange.getCreateTime());
            preparedStatement.setObject(6, rulesRange.getRename());
            preparedStatement.setObject(7, date);
            preparedStatement.setObject(8, rulesRange.getStatus());
            preparedStatement.setObject(9, rulesRange.getId());
        });
        return update > 0 ? rulesRange : null;
    }

    @Override
    public NodeRulesRange saveNodeRuleRange(NodeRulesRange rulesRange) {
        if (null == rulesRange.getId()) {
            rulesRange.setId(UUID.randomUUID().toString().replace("-", ""));
        }
        //(id,node_rules_id,type,node,range,create_time,rename,update_time,status)
        String str = "insert into t_mgr_node_rules_range values(?,?,?,?,?,?,?,?,?)";
        rulesRange.setCreateTime(System.currentTimeMillis());
        return jdbcIGA.update(str, preparedStatement -> {
            preparedStatement.setObject(1, rulesRange.getId());
            preparedStatement.setObject(2, rulesRange.getNodeRulesId());
            preparedStatement.setObject(3, rulesRange.getType());
            preparedStatement.setObject(4, rulesRange.getNode());
            preparedStatement.setObject(5, rulesRange.getRange());
            preparedStatement.setObject(6, new Timestamp(rulesRange.getCreateTime()));
            preparedStatement.setObject(7, rulesRange.getRename());
            preparedStatement.setObject(8, null);
            preparedStatement.setObject(9, rulesRange.getStatus());
        }) > 0 ? rulesRange : null;

    }


}
