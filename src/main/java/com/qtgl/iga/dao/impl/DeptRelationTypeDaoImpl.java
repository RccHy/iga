package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.DeptRelationType;
import com.qtgl.iga.dao.DeptRelationTypeDao;
import com.qtgl.iga.utils.MyBeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
@Component
public class DeptRelationTypeDaoImpl implements DeptRelationTypeDao {


    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;

    @Override
    public List<DeptRelationType> findAll(String domain) {
        //todo 超级租户处理  关联关系类型
        String sql = "select id,code,name,domain,relation_index as relationIndex from t_mgr_dept_relation_type where  domain=?";
        List<Map<String, Object>> listMaps = jdbcIGA.queryForList(sql, domain);
        List<DeptRelationType> deptRelationTypes = new ArrayList<>();
        listMaps.forEach(map -> {
            DeptRelationType deptRelationType = new DeptRelationType();
            try {
                MyBeanUtils.populate(deptRelationType, map);
            } catch (Exception e) {
                e.printStackTrace();
            }
            deptRelationTypes.add(deptRelationType);
        });
        return deptRelationTypes;
    }

    @Override
    public void initialization(String domainInfo) {

         String sql = "INSERT INTO t_mgr_dept_relation_type (id, code, name, domain, relation_index)\n" +
                "VALUES (uuid(), '01', '隶属', ?,1)," +
                "       (uuid(), '02', '直设',  ?,2)," +
                "       (uuid(), '03', '内设',  ?,3)," +
                "       (uuid(), '04', '挂靠',  ?,4),  " +
                "       (uuid(), '05', '合署',  ?,5);";

        jdbcIGA.update(sql, preparedStatement -> {
            preparedStatement.setObject(1, domainInfo);
            preparedStatement.setObject(2, domainInfo);
            preparedStatement.setObject(3, domainInfo);
            preparedStatement.setObject(4, domainInfo);
            preparedStatement.setObject(5, domainInfo);
        });
    }
}
