package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.NodeRulesRange;
import com.qtgl.iga.dao.NodeRulesRangeDao;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


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
}
