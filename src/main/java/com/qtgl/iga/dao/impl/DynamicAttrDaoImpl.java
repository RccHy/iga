package com.qtgl.iga.dao.impl;


import com.qtgl.iga.bo.DynamicAttr;
import com.qtgl.iga.dao.DynamicAttrDao;
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
public class DynamicAttrDaoImpl implements DynamicAttrDao {


    @Resource(name = "jdbcSSO")
    JdbcTemplate jdbcSSO;

    @Override
    public List<DynamicAttr> findAllByType(String type, String tenantId) {
        String sql = "select id, name, code, required ,description,tenant_id as tenantId,type  from dynamic_attr where tenant_id=? and type=?";
        List<Map<String, Object>> listMaps = jdbcSSO.queryForList(sql, tenantId, type);
        List<DynamicAttr> dynamicAttrs = new ArrayList<>();
        listMaps.forEach(map -> {
            DynamicAttr dynamicAttr = new DynamicAttr();
            try {
                MyBeanUtils.populate(dynamicAttr, map);
            } catch (Exception e) {
                e.printStackTrace();
            }
            dynamicAttrs.add(dynamicAttr);
        });
        return dynamicAttrs;
    }

}
