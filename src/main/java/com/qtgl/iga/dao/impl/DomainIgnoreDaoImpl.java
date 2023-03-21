package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.DomainIgnore;
import com.qtgl.iga.dao.DomainIgnoreDao;
import com.qtgl.iga.utils.MyBeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
@Component
public class DomainIgnoreDaoImpl implements DomainIgnoreDao {
    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;

    @Override
    public List<DomainIgnore> findByDomain(String domainId) {
        String sql = "select id, domain, upstream_id as upstreamId,node_rule_id as nodeRuleId  from t_mgr_domain_ignore  where domain= ? ";

        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, domainId);
        if (!CollectionUtils.isEmpty(mapList)) {
            ArrayList<DomainIgnore> list = new ArrayList<>();

            for (Map<String, Object> map : mapList) {
                DomainIgnore domainIgnore = new DomainIgnore();
                try {
                    MyBeanUtils.populate(domainIgnore, map);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }

                list.add(domainIgnore);
            }
            return list;
        }
        return null;
    }

    @Override
    public DomainIgnore save(DomainIgnore domainIgnore) {
        String sql = "INSERT INTO `t_mgr_domain_ignore`(`id`, `domain`, `upstream_id`, `node_rule_id`) VALUES (uuid(), ?, ?, ?)";
        jdbcIGA.update(sql, preparedStatement -> {
            preparedStatement.setObject(1, domainIgnore.getDomain());
            preparedStatement.setObject(2, domainIgnore.getUpstreamId());
            preparedStatement.setObject(3, domainIgnore.getNodeRuleId());
        });
        return domainIgnore;
    }

    @Override
    public Integer deleteByUpstreamId(String upstreamId) {
        String sql = "delete from t_mgr_domain_ignore where upstream_id = ?";
        return jdbcIGA.update(sql, preparedStatement -> {
            preparedStatement.setObject(1, upstreamId);
        });
    }

    @Override
    public DomainIgnore deleteByUpstreamIdAndDomain(String upstreamId, String domainId) {
        String sql = "DELETE FROM `t_mgr_domain_ignore` WHERE upstream_id = ? and domain= ? ";
        jdbcIGA.update(sql, preparedStatement -> {
            preparedStatement.setObject(1, upstreamId);
            preparedStatement.setObject(2, domainId);
        });
        return new DomainIgnore();
    }

    @Override
    public DomainIgnore deleteByNodeRuleIdAndDomain(String nodeRuleId, String domainId) {
        String sql = "DELETE FROM `t_mgr_domain_ignore` WHERE node_rule_id = ? and domain= ? ";
        jdbcIGA.update(sql, preparedStatement -> {
            preparedStatement.setObject(1, nodeRuleId);
            preparedStatement.setObject(2, domainId);
        });
        return new DomainIgnore();
    }

    @Override
    public List<DomainIgnore> findByParam(DomainIgnore domainIgnore) {
        String sql = "select id  from t_mgr_domain_ignore  where domain= ? ";
        List<Object> param = new ArrayList<>();
        param.add(domainIgnore.getDomain());
        if (null != domainIgnore.getNodeRuleId()) {
            sql = sql + "and node_rule_id =? ";
            param.add(domainIgnore.getNodeRuleId());
        }
        if (null != domainIgnore.getUpstreamId()) {
            sql = sql + "and upstream_id =? ";
            param.add(domainIgnore.getUpstreamId());
        }
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, param.toArray());
        if (!CollectionUtils.isEmpty(mapList)) {
            ArrayList<DomainIgnore> list = new ArrayList<>();
            for (Map<String, Object> map : mapList) {
                DomainIgnore ignore = new DomainIgnore();
                try {
                    MyBeanUtils.populate(ignore, map);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }

                list.add(ignore);
            }
            return list;
        }
        return null;
    }
}
