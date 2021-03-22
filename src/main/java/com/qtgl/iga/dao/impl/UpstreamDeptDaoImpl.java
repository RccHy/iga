package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.UpstreamDept;
import com.qtgl.iga.dao.UpstreamDeptDao;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

/**
 * <FileName> UpstreamDeptDaoImpl
 * <Desc>
 **/
@Repository
public class UpstreamDeptDaoImpl implements UpstreamDeptDao {

    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;

    @Override
    public UpstreamDept saveUpstreamDepts(UpstreamDept upstreamDept) {

        String sql = "insert into t_mgr_upstream_dept  values(?,?,?,?)";
        //生成主键和时间
        String id = UUID.randomUUID().toString().replace("-", "");
        upstreamDept.setId(id);
        upstreamDept.setCreateTime(new Timestamp(new Date().getTime()));
        int update = jdbcIGA.update(sql, preparedStatement -> {
            preparedStatement.setObject(1, id);
            preparedStatement.setObject(2, upstreamDept.getUpstreamTypeId());
            preparedStatement.setObject(3, upstreamDept.getDept());
            preparedStatement.setObject(4, upstreamDept.getCreateTime());


        });
        return update > 0 ? upstreamDept : null;
    }
}
