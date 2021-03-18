package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bean.NodeDto;
import com.qtgl.iga.bo.NodeRules;
import com.qtgl.iga.bo.NodeRulesRange;
import com.qtgl.iga.dao.NodeRulesRangeDao;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
//        String str = "insert into t_mgr_node_rules_range values(?,?,?,?,?,?,?)";
//        for (NodeRulesRange nodeRulesRange : nodeDto.getNodeRulesRanges()) {
//            nodeRulesRange.setId(UUID.randomUUID().toString().replace("-", ""));
//            nodeRulesRange.setNodeRulesId(nodeDto.getNodeRules().get(0).get);
//            nodeRulesRange.setActiveTime(new Date().getTime());
//            nodeRulesRange.setCreateTime(new Date().getTime());
//            nodeRulesRange.setUpdateTime(null);
//        }
//        List<NodeRules> nodeRules = nodeDto.getNodeRules();
//        int[] ints = jdbcIGA.batchUpdate(str, new BatchPreparedStatementSetter() {
//            @Override
//            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
//                preparedStatement.setObject(1, nodeRules.get(i).getId());
//                preparedStatement.setObject(2, nodeRules.get(i).getNodeId());
//                preparedStatement.setObject(3, nodeRules.get(i).getType());
//                preparedStatement.setObject(4, nodeRules.get(i).isActive());
//                preparedStatement.setObject(5, nodeRules.get(i).getActiveTime());
//                preparedStatement.setObject(6, nodeRules.get(i).getCreateTime());
//                preparedStatement.setObject(7, null);
//                preparedStatement.setObject(8, nodeRules.get(i).getServiceKey());
//                preparedStatement.setObject(9, nodeRules.get(i).getUpstreamTypesId());
//                preparedStatement.setObject(10, nodeRules.get(i).getInherit());
//                preparedStatement.setObject(11, nodeRules.get(i).getSort());
//            }
//
//            @Override
//            public int getBatchSize() {
//                return nodeRules.size();
//            }
//        });
//        Boolean contains = ints.toString().contains("-1");
//        return contains? null : nodeDto;
        return null;
    }
}
