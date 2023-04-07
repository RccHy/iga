package com.qtgl.iga.dao.impl;


import com.qtgl.iga.bo.ShadowCopy;
import com.qtgl.iga.dao.ShadowCopyDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Repository
@Slf4j
public class ShadowCopyDaoImpl implements ShadowCopyDao {
    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;

    @Override
    public ShadowCopy save(ShadowCopy shadowCopy) {
        if (StringUtils.isBlank(shadowCopy.getId())) {
            //String sql = "INSERT INTO t_mgr_shadow_copy (`id`, `data`, `upstream_type_id`, `type`, `domain`, `create_time`) VALUES (uuid(), ?, ?, ?, ?, now())";
            String sql = "INSERT INTO t_mgr_shadow_copy (`id`, `data`, `upstream_type_id`, `type`, `domain`, `create_time`) VALUES (uuid(), COMPRESS(?), ?, ?, ?, now())";
            //生成主键和时间

            jdbcIGA.update(sql, preparedStatement -> {
                preparedStatement.setObject(1, shadowCopy.getData());
                preparedStatement.setObject(2, shadowCopy.getUpstreamTypeId());
                preparedStatement.setObject(3, shadowCopy.getType());
                preparedStatement.setObject(4, shadowCopy.getDomain());

            });
        } else {
            String sql = "UPDATE `t_mgr_shadow_copy` SET `data` = COMPRESS(?),  `create_time` = now() WHERE `id` = ?";
            //String sql = "UPDATE `t_mgr_shadow_copy` SET `data` = ?,  `create_time` = now() WHERE `id` = ?";
            //生成主键和时间

            jdbcIGA.update(sql, preparedStatement -> {
                preparedStatement.setObject(1, shadowCopy.getData());
                preparedStatement.setObject(2, shadowCopy.getId());
            });
        }


        return shadowCopy;
    }

    @Override
    public ShadowCopy findByUpstreamTypeAndType(String upstreamTypeId, String type, String domain) {
        String sql = "select id,  upstream_type_id as upstreamTypeId ,type, domain ,create_time as createTime from t_mgr_shadow_copy where type= ? and upstream_type_id= ? and domain=? ";

        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, type, upstreamTypeId, domain);
        if (mapList.size() == 1) {
            ShadowCopy shadowCopy = new ShadowCopy();
            for (Map<String, Object> map : mapList) {

                BeanMap beanMap = BeanMap.create(shadowCopy);
                beanMap.putAll(map);
            }
            return shadowCopy;
        }
        return null;
    }

    @Override
    public ShadowCopy findDataByUpstreamTypeAndType(String upstreamTypeId, String type, String domain) {
        String sql = "select  UNCOMPRESS(`data`) as data  from t_mgr_shadow_copy where type= ? and upstream_type_id= ? and domain=? ";

        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, type, upstreamTypeId, domain);
        ShadowCopy shadowCopy = new ShadowCopy();
        if (mapList.size() == 1) {
            for (Map<String, Object> map : mapList) {

                BeanMap beanMap = BeanMap.create(shadowCopy);
                beanMap.putAll(map);
            }
            return shadowCopy;
        }
        return null;
    }
}
