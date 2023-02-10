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
    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;

    @Override
    public List<DynamicValue> findAllByAttrId(List<String> attrIds, String tenantId) {
        List<Object> param = new ArrayList<>();
        param.add(tenantId);
        StringBuffer sql = new StringBuffer("select v.id, attr_id as attrId , entity_id as entityId, value ,v.tenant_id as tenantId,a.code as code  from dynamic_value v,dynamic_attr a where v.attr_id = a.id and v.tenant_id=?  ");
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
            dynamicValue.setKey(dynamicValue.getAttrId());
            dynamicValues.add(dynamicValue);
        });
        return dynamicValues;
    }

    @Override
    public List<DynamicValue> findAllByAttrIdIGA(List<String> attrIds, String tenantId) {
        List<Object> param = new ArrayList<>();
        param.add(tenantId);
        StringBuffer sql = new StringBuffer("select v.id, attr_id as attrId , entity_id as entityId, value ,v.tenant_id as tenantId,a.code as code  from dynamic_value v,dynamic_attr a where v.attr_id = a.id and v.tenant_id=?  ");
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

        List<Map<String, Object>> listMaps = jdbcIGA.queryForList(sql.toString(), param.toArray());
        List<DynamicValue> dynamicValues = new ArrayList<>();
        listMaps.forEach(map -> {
            DynamicValue dynamicValue = new DynamicValue();
            try {
                MyBeanUtils.populate(dynamicValue, map);
            } catch (Exception e) {
                e.printStackTrace();
            }
            dynamicValue.setKey(dynamicValue.getAttrId());
            dynamicValues.add(dynamicValue);
        });
        return dynamicValues;
    }

    @Override
    public List<DynamicValue> findAllAttrByType(String tenantId, String type) {
        List<Object> param = new ArrayList<>();
        param.add(tenantId);
        StringBuffer sql = new StringBuffer("select v.id, attr_id as attrId , entity_id as entityId, value ,v.tenant_id as tenantId,a.code as code  from dynamic_value v,dynamic_attr a ");
        switch (type) {
            case "USER":
                sql.append(" , identity i where v.attr_id = a.id and i.id=v.entity_id and i.del_mark =0 and v.tenant_id=?  and a.type= ? ");
                break;
            case "IDENTITY":
                sql.append(" , user i,identity_user iu,identity o where v.attr_id = a.id and i.id=v.entity_id and i.del_mark =0 " +
                        "and iu.user_id=i.id and o.id=iu.identity_id and o.del_mark=0 and v.tenant_id=?  and a.type= ? ");
                break;
            case "DEPT":
                sql.append(" , dept i where v.attr_id = a.id and i.id=v.entity_id and i.del_mark =0 and v.tenant_id=?  and a.type= ? ");
                break;
            case "POST":
                sql.append(" , user_type i where v.attr_id = a.id and i.id=v.entity_id and i.del_mark =0 and v.tenant_id=?  and a.type= ? ");
                break;
            default:
                return null;
        }
        param.add(type);

        List<Map<String, Object>> listMaps = jdbcSSO.queryForList(sql.toString(), param.toArray());
        List<DynamicValue> dynamicValues = new ArrayList<>();
        listMaps.forEach(map -> {
            DynamicValue dynamicValue = new DynamicValue();
            try {
                MyBeanUtils.populate(dynamicValue, map);
            } catch (Exception e) {
                e.printStackTrace();
            }
            dynamicValue.setKey(dynamicValue.getAttrId());
            dynamicValues.add(dynamicValue);
        });
        return dynamicValues;
    }

}
