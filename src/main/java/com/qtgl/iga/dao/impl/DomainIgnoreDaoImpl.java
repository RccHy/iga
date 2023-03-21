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
}
