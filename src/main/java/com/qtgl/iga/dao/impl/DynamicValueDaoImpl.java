package com.qtgl.iga.dao.impl;


import com.qtgl.iga.bo.DynamicValue;
import com.qtgl.iga.dao.DynamicValueDao;
import com.qtgl.iga.utils.MyBeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
@Component
public class DynamicValueDaoImpl implements DynamicValueDao {


    @Resource(name = "jdbcSSO")
    JdbcTemplate jdbcSSO;

    @Override
    public List<DynamicValue> findAllByAttrId(List<String> attrIds, String tenantId) {
        List<Object> param = new ArrayList<>();
        param.add(tenantId);
        StringBuffer sql = new StringBuffer("select id, attr_id as attrId , entity_id as entityId, value ,tenant_id as tenantId  from dynamic_value where tenant_id=?  ");
        if (!CollectionUtils.isEmpty(attrIds)) {
            sql.append(" and attr_id in (");
            for (String attrId : attrIds) {
                sql.append(" ?,");
                param.add(attrId);
            }
            sql.deleteCharAt(sql.length() - 1).append(" ) ");
        } else {
            return null;
        }

        List<Map<String, Object>> listMaps = jdbcSSO.queryForList(sql.toString(), param.toArray());
        List<DynamicValue> dynamicValues = new ArrayList<>();
        listMaps.forEach(map -> {
            DynamicValue dynamicValue = new DynamicValue();
            try {
                MyBeanUtils.populate(dynamicValue, map);
            } catch (Exception e) {
                e.printStackTrace();
            }
            dynamicValues.add(dynamicValue);
        });
        return dynamicValues;
    }

}
