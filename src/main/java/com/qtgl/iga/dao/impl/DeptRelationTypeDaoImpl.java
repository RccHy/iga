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
    JdbcTemplate jdbcSSOAPI;

    @Override
    public List<DeptRelationType> findAll(String domain) {
        String sql = "select id,code,name,domain,relation_index as relationIndex from t_mgr_dept_relation_type where  domain=?";
        List<Map<String, Object>> listMaps = jdbcSSOAPI.queryForList(sql, domain);
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
}
