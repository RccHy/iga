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

    @Override
    public UpstreamDept findUpstreamDeptByUpstreamId(String id) {
        String sql = "select id,upstream_type_id as upstreamTypeId ,dept,create_time as createTime from t_mgr_upstream_dept  where upstream_type_id = ? ";
        UpstreamDept upstreamDept = jdbcIGA.queryForObject(sql, UpstreamDept.class, id);

        return upstreamDept;
    }

    @Override
    public UpstreamDept updateUpstreamDepts(UpstreamDept upstreamDept) {
        String sql = "update t_mgr_upstream_dept  set dept=?,create_time =? where upstream_type_id=? ";

        int update = jdbcIGA.update(sql, preparedStatement -> {
            preparedStatement.setObject(1, upstreamDept.getDept());
            preparedStatement.setObject(2, upstreamDept.getCreateTime());
            preparedStatement.setObject(3, upstreamDept.getUpstreamTypeId());
        });
        return update > 0 ? upstreamDept : null;
    }
}
