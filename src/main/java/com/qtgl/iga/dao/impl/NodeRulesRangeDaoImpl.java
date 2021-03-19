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
    public List<NodeRulesRange> getByRulesId(String rulesId) {
        try {
            List<NodeRulesRange> nodeRulesRanges = new ArrayList<>();
            List<Map<String, Object>> maps = jdbcIGA.queryForList("select " +
                            "id,node_rules_id as nodeRulesId,`type`,`rename`,node,`range`,create_time as createTime " +
                            "from t_mgr_node_rules_range " +
                            "where node_rules_id=? " +
                            "order by `type` desc",
                    rulesId);
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
        String str = "insert into t_mgr_node_rules_range values(?,?,?,?,?,?,?)";
        boolean contains = false;
        for (NodeRulesVo nodeRule : nodeDto.getNodeRules()) {
            List<NodeRulesRange> nodeRulesRanges = nodeRule.getNodeRulesRanges();
            for (NodeRulesRange nodeRulesRange : nodeRulesRanges) {
                nodeRulesRange.setId(UUID.randomUUID().toString().replace("-", ""));
                nodeRulesRange.setNodeRulesId(nodeRule.getId());
                nodeRulesRange.setCreateTime(new Date().getTime());
            }
            int[] ints = jdbcIGA.batchUpdate(str, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                    preparedStatement.setObject(1, nodeRulesRanges.get(i).getId());
                    preparedStatement.setObject(2, nodeRulesRanges.get(i).getNodeRulesId());
                    preparedStatement.setObject(3, nodeRulesRanges.get(i).getType());
                    preparedStatement.setObject(4, nodeRulesRanges.get(i).getNode());
                    preparedStatement.setObject(5, nodeRulesRanges.get(i).getRange());
                    preparedStatement.setObject(6, new Timestamp(nodeRulesRanges.get(i).getCreateTime()));
                    preparedStatement.setObject(7, nodeRulesRanges.get(i).getRename());
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
    public Integer deleteNodeRulesRange(String id) {
        Object[] params = new Object[1];
        params[0] = id;

        String sql = "delete from t_mgr_node_rules_range where  node_rules_id = ? ";

        return jdbcIGA.update(sql, params);
    }
}
