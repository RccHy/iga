package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bean.MergeAttrRule;
import com.qtgl.iga.dao.MergeAttrRuleDao;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
@Component
public class MergeAttrRuleDaoImpl implements MergeAttrRuleDao {
    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;

    @Override
    public List<MergeAttrRule> findMergeAttrRules(String userId, String tenantId) {
        StringBuilder sql = new StringBuilder("select id,attr_name as attrName,entity_id as entityId,from_entity_id as fromEntityId,dynamic_attr_id as dynamicAttrId," +
                " create_time as createTime from t_mgr_merge_attr_rule where tenant_id=?  ");
        List<Object> params = new ArrayList<>();
        params.add(tenantId);
        if (StringUtils.isNotBlank(userId)) {
            sql.append(" and entity_id=? ");
            params.add(userId);
        }
        sql.append(" order by  create_time desc");
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql.toString(), params.toArray());
        return getMergeAttrRules(mapList);
    }

    @Override
    public List<MergeAttrRule> findMergeAttrRulesByTenantId(String tenantId) {
        String sql = "select id,attr_name as attrName,entity_id as entityId,from_entity_id as fromEntityId,dynamic_attr_id as dynamicAttrId," +
                " create_time as createTime from t_mgr_merge_attr_rule where tenant_id=?  ";
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, tenantId);
        return getMergeAttrRules(mapList);
    }

    @Override
    public List<MergeAttrRule> saveMergeAttrRule(List<MergeAttrRule> mergeAttrRules, String tenantId) {
        String sql = "INSERT INTO `t_mgr_merge_attr_rule`(`id`, `attr_name`, `entity_id`, `from_entity_id`, `dynamic_attr_id`, `create_time`, `tenant_id`) " +
                "VALUES (uuid(), ?, ?, ?, ?, ?, ?) ";
        LocalDateTime now = LocalDateTime.now();
        int[] ints = jdbcIGA.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                preparedStatement.setObject(1, mergeAttrRules.get(i).getAttrName());
                preparedStatement.setObject(2, mergeAttrRules.get(i).getEntityId());
                preparedStatement.setObject(3, mergeAttrRules.get(i).getFromEntityId());
                preparedStatement.setObject(4, mergeAttrRules.get(i).getDynamicAttrId());
                preparedStatement.setObject(5, now);
                preparedStatement.setObject(6, tenantId);
            }

            @Override
            public int getBatchSize() {
                return mergeAttrRules.size();
            }
        });

        return mergeAttrRules;
    }

    @Override
    public Integer deleteMergeAttrRuleByEntityIds(List<String> entityIds, String tenantId) {
        StringBuilder sql = new StringBuilder("delete from t_mgr_merge_attr_rule where tenant_id=? and entity_id in( ");

        List<Object> params = new ArrayList<>();
        params.add(tenantId);
        for (String entityId : entityIds) {
            sql.append("?,");
            params.add(entityId);
        }

        sql.replace(sql.length() - 1, sql.length(), " ) ");
        return jdbcIGA.update(sql.toString(), params.toArray());
    }

    @Override
    public Integer deleteMergeAttrRuleByEntityId(String userId, String tenantId) {
        return jdbcIGA.update("delete from t_mgr_merge_attr_rule where tenant_id = ? and entity_id = ? ", tenantId, userId);
    }

    private List<MergeAttrRule> getMergeAttrRules(List<Map<String, Object>> mapList) {
        List<MergeAttrRule> list = new ArrayList<>();
        if (!CollectionUtils.isEmpty(mapList)) {
            for (Map<String, Object> map : mapList) {
                MergeAttrRule mergeAttrRule = new MergeAttrRule();
                try {
                    BeanMap beanMap = BeanMap.create(mergeAttrRule);
                    beanMap.putAll(map);
                    if ("account_no".equals(mergeAttrRule.getAttrName())) {
                        mergeAttrRule.setAttrName("username");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                list.add(mergeAttrRule);
            }
            return list;
        }

        return null;
    }
}
