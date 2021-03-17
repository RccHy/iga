package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.dao.DomainInfoDao;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
@Component
public class DomainInfoDaoImpl implements DomainInfoDao {


    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;


    @Override
    public void save(DomainInfo domainInfo) {
        jdbcIGA.update("INSERT INTO `t_mgr_domain_info`(`id`, `domain_id`," +
                        " `domain_name` ,`client_id` , `client_secret` , " +
                        "`status`, `create_time`,`create_user` ,`update_time` ) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?);",
                domainInfo.getId(), domainInfo.getDomainId(), domainInfo.getDomainName(), domainInfo.getClientId(), domainInfo.getClientSecret(), domainInfo.getStatus(), domainInfo.getCreateTime(), domainInfo.getCreateUser(),domainInfo.getUpdateTime()
        );
    }

    @Override
    public List<DomainInfo> findAll() {
        List<Map<String, Object>> mapList = jdbcIGA.queryForList("select id,domain_id as domainId,domain_name as domainName,client_id as clientId,client_secret as clientSecret," +
                "create_time as createTime,create_user as createUser,status,update_time as updateTime from t_mgr_domain_info where status=0");
        ArrayList<DomainInfo> list = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {
                DomainInfo domainInfo = new DomainInfo();
                BeanMap beanMap = BeanMap.create(domainInfo);
                beanMap.putAll(map);
                list.add(domainInfo);
            }
            return list;
        }

        return null;
    }

//    @Override
//    public DomainInfo getByDomainName(String name) {
//        DomainInfo domainInfo = jdbcIGA.queryForObject("select * from t_mgr_domain_info where status=0", new DomainInfoRowMapper());
//        return domainInfo;
//    }

    @Override
    public DomainInfo getByDomainName(String name) {
        List<Map<String, Object>> mapList = jdbcIGA.queryForList("select id,domain_id as domainId,domain_name as domainName,client_id as clientId,client_secret as clientSecret,create_time as createTime,update_time as updateTime,create_user as createUser,status from t_mgr_domain_info where status=0");
        DomainInfo domainInfo = new DomainInfo();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {

                BeanMap beanMap = BeanMap.create(domainInfo);
                beanMap.putAll(map);

            }
            return domainInfo;
        }
        return null;
    }

    @Override
    public DomainInfo findById(String id) {
        String sql = "select id,domain_id as domainId,domain_name as domainName,client_id as clientId,client_secret as clientSecret," +
                "create_time as createTime,create_user as createUser,status from t_mgr_domain_info where id= ? ";

        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, id);
        DomainInfo domainInfo = new DomainInfo();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {

                BeanMap beanMap = BeanMap.create(domainInfo);
                beanMap.putAll(map);
            }
            return domainInfo;
        }
        return null;
    }
}
