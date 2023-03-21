package com.qtgl.iga.dao.impl;


import com.qtgl.iga.AutoUpRunner;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.dao.UpstreamDao;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.enums.FilterCodeEnum;
import com.qtgl.iga.utils.exception.CustomException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;


@Repository
@Component
public class UpstreamDaoImpl implements UpstreamDao {


    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;
    @Resource(name = "iga-txTemplate")
    TransactionTemplate txTemplate;

    @Override
    public List<Upstream> findAll(Map<String, Object> arguments, String domain) {
        String sql = "select id,app_code as appCode,app_name as appName,data_code as dataCode," +
                "create_time as createTime,create_user as createUser,active,color,domain ," +
                "active_time as activeTime,update_time as updateTime from t_mgr_upstream where 1 = 1 and domain in (? ";
        //存入参数
        List<Object> param = new ArrayList<>();
        param.add(domain);
        //拼接sql
        StringBuffer stb = new StringBuffer(sql);
        if (StringUtils.isNotBlank(AutoUpRunner.superDomainId)) {
            stb.append(",? ");
            param.add(AutoUpRunner.superDomainId);
        }
        stb.append(" ) ");

        dealData(arguments, stb, param);
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(stb.toString(), param.toArray());

        ArrayList<Upstream> list = new ArrayList<>();
        if (!CollectionUtils.isEmpty(mapList)) {
            for (Map<String, Object> map : mapList) {
                Upstream upstream = new Upstream();
                BeanMap beanMap = BeanMap.create(upstream);
                beanMap.putAll(map);
                list.add(upstream);
            }
            return list;
        }
        return null;
    }


    @Override
    @Transactional
    public Upstream saveUpstream(Upstream upstream, String domain) {


        String sql = "insert into t_mgr_upstream  values(?,?,?,?,?,?,?,?,?,?,?)";
        //生成主键和时间
        String id = UUID.randomUUID().toString().replace("-", "");
        upstream.setId(id);
        Timestamp date = new Timestamp(System.currentTimeMillis());
        upstream.setCreateTime(date);
        int update = jdbcIGA.update(sql, preparedStatement -> {
            preparedStatement.setObject(1, id);
            preparedStatement.setObject(2, upstream.getAppCode());
            preparedStatement.setObject(3, upstream.getAppName());
            preparedStatement.setObject(4, upstream.getDataCode());
            preparedStatement.setObject(5, date);
            preparedStatement.setObject(6, upstream.getCreateUser());
            preparedStatement.setObject(7, null == upstream.getActive() ? true : upstream.getActive());
            preparedStatement.setObject(8, upstream.getColor());
            preparedStatement.setObject(9, domain);
            preparedStatement.setObject(10, date);
            preparedStatement.setObject(11, date);

        });
        return update > 0 ? upstream : null;
    }

    @Override
    @Transactional
    public Integer deleteUpstream(String id) {

        //删除权威源数据
        String sql = "delete from t_mgr_upstream  where id =?";


        return jdbcIGA.update(sql, preparedStatement -> preparedStatement.setObject(1, id));


    }

    @Override
    public ArrayList<Upstream> getUpstreams(String id, String domain) {
        //查询权威源状态
        List<Object> param = new ArrayList<>();

        StringBuffer sql = new StringBuffer("select id,app_code as appCode,app_name as appName,data_code as dataCode,create_time as createTime,create_user as createUser,active,color,domain ,active_time as activeTime,update_time as updateTime from t_mgr_upstream  where id =? and domain in (? ");
        param.add(id);
        param.add(domain);

        if (StringUtils.isNotBlank(AutoUpRunner.superDomainId)) {
            sql.append(",? ");
            param.add(AutoUpRunner.superDomainId);
        }
        sql.append(") ");
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql.toString(), param.toArray());

        ArrayList<Upstream> upstreamList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(mapList)) {
            for (Map<String, Object> map : mapList) {
                Upstream upstream = new Upstream();
                BeanMap beanMap = BeanMap.create(upstream);
                beanMap.putAll(map);
                upstreamList.add(upstream);
            }
        }
        return upstreamList;
    }

    @Override
    public Upstream updateUpstream(Upstream upstream) {
        //判重
        Object[] param = new Object[]{upstream.getAppCode(), upstream.getAppName(), upstream.getId(), upstream.getDomain()};
        List<Map<String, Object>> mapList = jdbcIGA.queryForList("select  * from t_mgr_upstream where (app_code = ? or app_name = ?) and id != ? and domain=?  ", param);
        if (!CollectionUtils.isEmpty(mapList)) {
            throw new CustomException(ResultCode.FAILED, "code 或 name 不能重复,修改失败");
        }


        String sql = "update t_mgr_upstream  set app_code = ?,app_name = ?,data_code = ?,create_user = ?,active = ?," +
                "color = ?,domain = ?,active_time = ?,update_time= ?  where id=? ";
        Timestamp date = new Timestamp(System.currentTimeMillis());
        int update = jdbcIGA.update(sql, preparedStatement -> {
            preparedStatement.setObject(1, upstream.getAppCode());
            preparedStatement.setObject(2, upstream.getAppName());
            preparedStatement.setObject(3, upstream.getDataCode());
            preparedStatement.setObject(4, upstream.getCreateUser());
            preparedStatement.setObject(5, null == upstream.getActive() ? true : upstream.getActive());
            preparedStatement.setObject(6, upstream.getColor());
            preparedStatement.setObject(7, upstream.getDomain());
            preparedStatement.setObject(8, date);
            preparedStatement.setObject(9, date);
            preparedStatement.setObject(10, upstream.getId());
        });
        return update > 0 ? upstream : null;
    }

    @Override
    public Upstream findById(String id) {
        String sql = "select id,app_code as appCode,app_name as appName,data_code as dataCode," +
                "create_time as createTime,create_user as createUser,active,color,domain ," +
                "active_time as activeTime,update_time as updateTime from t_mgr_upstream where id= ? ";

        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, id);
        Upstream upstream = new Upstream();
        if (!CollectionUtils.isEmpty(mapList)) {
            for (Map<String, Object> map : mapList) {

                BeanMap beanMap = BeanMap.create(upstream);
                beanMap.putAll(map);
            }
            return upstream;
        }
        return null;
    }

    /**
     * @param code   权威源代码
     * @param domain 需要精准租户
     * @return
     */
    @Override
    public Upstream findByCodeAndDomain(String code, String domain) {
        String sql = "select id,app_code as appCode,app_name as appName,data_code as dataCode," +
                "create_time as createTime,create_user as createUser,active,color,domain ," +
                "active_time as activeTime,update_time as updateTime from t_mgr_upstream where app_code= ? and  domain=?";

        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, code, domain);
        Upstream upstream = new Upstream();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {

                BeanMap beanMap = BeanMap.create(upstream);
                beanMap.putAll(map);
            }
            return upstream;
        }
        return null;
    }

    public Integer delAboutNode(Upstream upstream, DomainInfo domainInfo) throws Exception {
        try {
            // 权威源发生了修改,需要下线删除 node nodeRule nodeRuleRange 所有相关信息
            //删除nodeRuleRange,
            String deleteNodeRuleRangeSql = "delete from t_mgr_node_rules_range where  node_rules_id  in " +
                    "(select id from t_mgr_node_rules where upstream_types_id in (select id from t_mgr_upstream_types where upstream_id = ?))";
            jdbcIGA.update(deleteNodeRuleRangeSql, upstream.getId());

            //删除nodeRule
            String deleteNodeRuleSql = "delete from  t_mgr_node_rules where upstream_types_id in (select id from t_mgr_upstream_types where upstream_id = ?) or service_key in (select id from t_mgr_upstream_types where upstream_id = ?)";
            jdbcIGA.update(deleteNodeRuleSql, upstream.getId(),upstream.getId());

            //删除node, nodeRule 没有数据关联 则需要删除
            String deleteNodeSql = "delete from t_mgr_node where domain = ? and id not in (select node_id from t_mgr_node_rules)";
            jdbcIGA.update(deleteNodeSql, domainInfo.getId());

            // 删除权威源所有字段映射
            String deleteUpstreamFieldSql = "delete from t_mgr_upstream_types_field where upstream_type_id in (select id from t_mgr_upstream_types where upstream_id = ?)";
            jdbcIGA.update(deleteUpstreamFieldSql, upstream.getId());


        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        }

        return 1;
    }

    @Override
    public Integer saveUpstreamTypesAndFields(List<UpstreamType> upstreamTypes, List<UpstreamType> updateUpstreamTypes, List<UpstreamTypeField> upstreamTypeFields, DomainInfo domainInfo) {
        return txTemplate.execute(transactionStatus -> {
            Timestamp date = new Timestamp(System.currentTimeMillis());

            //添加权威源类型
            if (null != upstreamTypes && upstreamTypes.size() > 0) {
                String upstreamTypeSql = "INSERT INTO t_mgr_upstream_types (id, upstream_id, code,description, syn_type, dept_type_id, enable_prefix, active, active_time, root, create_time, update_time, graphql_url, service_code, " +
                        "domain, dept_tree_type_id, is_page, syn_way, is_incremental, person_characteristic,builtin_data) VALUES " +
                        "                    (?, ?, ?, ?, ?,?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?)";
                jdbcIGA.batchUpdate(upstreamTypeSql, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                        preparedStatement.setObject(1, upstreamTypes.get(i).getId());
                        preparedStatement.setObject(2, upstreamTypes.get(i).getUpstreamId());
                        preparedStatement.setObject(3, upstreamTypes.get(i).getCode());
                        preparedStatement.setObject(4, upstreamTypes.get(i).getDescription());
                        preparedStatement.setObject(5, upstreamTypes.get(i).getSynType());
                        preparedStatement.setObject(6, upstreamTypes.get(i).getDeptTypeId());
                        preparedStatement.setObject(7, upstreamTypes.get(i).getEnablePrefix());
                        preparedStatement.setObject(8, upstreamTypes.get(i).getActive());
                        preparedStatement.setObject(9, upstreamTypes.get(i).getActiveTime());
                        preparedStatement.setObject(10, upstreamTypes.get(i).getRoot());
                        preparedStatement.setObject(11, date);
                        preparedStatement.setObject(12, null);
                        preparedStatement.setObject(13, upstreamTypes.get(i).getGraphqlUrl());
                        preparedStatement.setObject(14, upstreamTypes.get(i).getServiceCode());
                        preparedStatement.setObject(15, domainInfo.getId());
                        preparedStatement.setObject(16, upstreamTypes.get(i).getDeptTreeTypeId());
                        preparedStatement.setObject(17, upstreamTypes.get(i).getIsPage());
                        preparedStatement.setObject(18, upstreamTypes.get(i).getSynWay());
                        preparedStatement.setObject(19, upstreamTypes.get(i).getIsIncremental());
                        preparedStatement.setObject(20, upstreamTypes.get(i).getPersonCharacteristic());
                        preparedStatement.setObject(21, upstreamTypes.get(i).getBuiltinData());
                    }

                    @Override
                    public int getBatchSize() {
                        return upstreamTypes.size();
                    }
                });
            }
            //修改权威源类型
            if (null != updateUpstreamTypes && updateUpstreamTypes.size() > 0) {
                String updateUpstreamTypeSql = "UPDATE t_mgr_upstream_types SET " +
                        "upstream_id = ?, description = ?, syn_type = ?, dept_type_id = ?, enable_prefix = ?, active = ?, active_time = ?, root = ?, " +
                        " update_time = now(), graphql_url = ?, service_code = ?, domain = ?, dept_tree_type_id = ?, is_page = ?, syn_way = ?, is_incremental = ?, person_characteristic = ?,builtin_data =? WHERE id = ?;";
                jdbcIGA.batchUpdate(updateUpstreamTypeSql, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                        preparedStatement.setObject(1, updateUpstreamTypes.get(i).getUpstreamId());
                        preparedStatement.setObject(2, updateUpstreamTypes.get(i).getDescription());
                        preparedStatement.setObject(3, updateUpstreamTypes.get(i).getSynType());
                        preparedStatement.setObject(4, updateUpstreamTypes.get(i).getDeptTypeId());
                        preparedStatement.setObject(5, updateUpstreamTypes.get(i).getEnablePrefix());
                        preparedStatement.setObject(6, updateUpstreamTypes.get(i).getActive());
                        preparedStatement.setObject(7, updateUpstreamTypes.get(i).getActiveTime());
                        preparedStatement.setObject(8, updateUpstreamTypes.get(i).getRoot());
                        preparedStatement.setObject(9, updateUpstreamTypes.get(i).getGraphqlUrl());
                        preparedStatement.setObject(10, updateUpstreamTypes.get(i).getServiceCode());
                        preparedStatement.setObject(11, domainInfo.getId());
                        preparedStatement.setObject(12, updateUpstreamTypes.get(i).getDeptTreeTypeId());
                        preparedStatement.setObject(13, updateUpstreamTypes.get(i).getIsPage());
                        preparedStatement.setObject(14, updateUpstreamTypes.get(i).getSynWay());
                        preparedStatement.setObject(15, updateUpstreamTypes.get(i).getIsIncremental());
                        preparedStatement.setObject(16, updateUpstreamTypes.get(i).getPersonCharacteristic());
                        preparedStatement.setObject(17, updateUpstreamTypes.get(i).getBuiltinData());
                        preparedStatement.setObject(18, updateUpstreamTypes.get(i).getId());
                    }
                    @Override
                    public int getBatchSize() {
                        return updateUpstreamTypes.size();
                    }
                });
            }
            // 权威源映射字段
            if (null != upstreamTypeFields && upstreamTypeFields.size() > 0) {
                String upstreamFieldSql = "insert into t_mgr_upstream_types_field (id,upstream_type_id,source_field,target_field,create_time,update_time,domain)values(?,?,?,?,?,?,?)";
                jdbcIGA.batchUpdate(upstreamFieldSql, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                        preparedStatement.setObject(1, upstreamTypeFields.get(i).getId());
                        preparedStatement.setObject(2, upstreamTypeFields.get(i).getUpstreamTypeId());
                        preparedStatement.setObject(3, upstreamTypeFields.get(i).getSourceField());
                        preparedStatement.setObject(4, upstreamTypeFields.get(i).getTargetField());
                        preparedStatement.setObject(5, date);
                        preparedStatement.setObject(6, null);
                        preparedStatement.setObject(7, domainInfo.getId());
                    }
                    @Override
                    public int getBatchSize() {
                        return upstreamTypeFields.size();
                    }
                });
            }
            return 1;

        });

    }

    /**
     * [bootstrap]批量保存 权威源相关的规则信息
     * @param nodes
     * @param nodeRulesList
     * @param nodeRulesRangeList
     * @param domainInfo
     * @return
     */
    @Override
    public Integer saveUpstreamAbountNodes(List<Node> nodes, List<NodeRules> nodeRulesList, List<NodeRulesRange> nodeRulesRangeList, DomainInfo domainInfo) {
        return txTemplate.execute(transactionStatus -> {
            try {
                Timestamp date = new Timestamp(System.currentTimeMillis());
                //添加node
                if (null != nodes && nodes.size() > 0) {
                    String nodeSql = "insert into t_mgr_node  (id,manual,node_code,create_time,update_time,domain,dept_tree_type,status,type)values(?,?,?,?,?,?,?,?,?)";
                    jdbcIGA.batchUpdate(nodeSql, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, nodes.get(i).getId());
                            preparedStatement.setObject(2, nodes.get(i).getManual());
                            preparedStatement.setObject(3, nodes.get(i).getNodeCode());
                            preparedStatement.setObject(4, date);
                            preparedStatement.setObject(5, null);
                            preparedStatement.setObject(6, nodes.get(i).getDomain());
                            preparedStatement.setObject(7, nodes.get(i).getDeptTreeType());
                            preparedStatement.setObject(8, nodes.get(i).getStatus());
                            preparedStatement.setObject(9, nodes.get(i).getType());
                        }
                        @Override
                        public int getBatchSize() {
                            return nodes.size();
                        }
                    });
                }
                //添加nodeRules
                String nodeRulesSql = "insert into t_mgr_node_rules (id,node_id,type,active,active_time,create_time,update_time,service_key,upstream_types_id,inherit_id,sort,status) values(?,?,?,?,?,?,?,?,?,?,?,?)";
                if (null != nodeRulesList && nodeRulesList.size() > 0) {
                    jdbcIGA.batchUpdate(nodeRulesSql, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, nodeRulesList.get(i).getId());
                            preparedStatement.setObject(2, nodeRulesList.get(i).getNodeId());
                            preparedStatement.setObject(3, nodeRulesList.get(i).getType());
                            preparedStatement.setObject(4, nodeRulesList.get(i).getActive());
                            preparedStatement.setObject(5, null);
                            preparedStatement.setObject(6, date);
                            preparedStatement.setObject(7, null);
                            preparedStatement.setObject(8, nodeRulesList.get(i).getServiceKey());
                            preparedStatement.setObject(9, nodeRulesList.get(i).getUpstreamTypesId());
                            preparedStatement.setObject(10, nodeRulesList.get(i).getInheritId());
                            preparedStatement.setObject(11, 0);
                            preparedStatement.setObject(12, nodeRulesList.get(i).getStatus());
                        }

                        @Override
                        public int getBatchSize() {
                            return nodeRulesList.size();
                        }
                    });
                }
                // 添加nodeRulesRange
                String nodeRuleRange = "INSERT INTO t_mgr_node_rules_range (id, node_rules_id, type, node, `range`, create_time, `rename`, update_time, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                if (null != nodeRulesRangeList && nodeRulesRangeList.size() > 0) {
                    jdbcIGA.batchUpdate(nodeRuleRange, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, nodeRulesRangeList.get(i).getId());
                            preparedStatement.setObject(2, nodeRulesRangeList.get(i).getNodeRulesId());
                            preparedStatement.setObject(3, nodeRulesRangeList.get(i).getType());
                            preparedStatement.setObject(4, nodeRulesRangeList.get(i).getNode());
                            preparedStatement.setObject(5, nodeRulesRangeList.get(i).getRange());
                            preparedStatement.setObject(6, date);
                            preparedStatement.setObject(7, nodeRulesRangeList.get(i).getRename());
                            preparedStatement.setObject(8, null);
                            preparedStatement.setObject(9, nodeRulesRangeList.get(i).getStatus());

                        }

                        @Override
                        public int getBatchSize() {
                            return nodeRulesRangeList.size();
                        }
                    });
                }

                return 1;
            } catch (Exception e) {
                transactionStatus.setRollbackOnly();
                e.printStackTrace();
                return -1;
            }

        });

    }

    @Override
    public List<Map<String, Object>> findByAppNameAndAppCode(String appName, String appCode, String domain) {
        Object[] param = new Object[]{appCode, appName, domain};
        List<Map<String, Object>> mapList = jdbcIGA.queryForList("select  * from t_mgr_upstream where app_code =? or app_name = ? and domain=? ", param);
        return mapList;
    }

    @Override
    public ArrayList<Upstream> findByDomainAndActiveIsFalse(String domain) {
        //拼接sql
        StringBuffer stb = new StringBuffer("select id,app_code as appCode,app_name as appName,data_code as dataCode," +
                "create_time as createTime,create_user as createUser,active,color,domain ," +
                "active_time as activeTime,update_time as updateTime from t_mgr_upstream where 1 = 1  and active=false and domain in (? ");
        //存入参数
        List<Object> param = new ArrayList<>();
        param.add(domain);
        if (StringUtils.isNotBlank(AutoUpRunner.superDomainId)) {
            stb.append(",? ");
            param.add(AutoUpRunner.superDomainId);
        }
        stb.append(" ) ");
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(stb.toString(), param.toArray());

        ArrayList<Upstream> list = new ArrayList<>();
        if (!CollectionUtils.isEmpty(mapList)) {
            for (Map<String, Object> map : mapList) {
                Upstream upstream = new Upstream();
                BeanMap beanMap = BeanMap.create(upstream);
                beanMap.putAll(map);
                list.add(upstream);
            }
            return list;
        }
        return null;
    }

    @Override
    public ArrayList<Upstream> findByOtherUpstream(List<String> ids, String domainId) {
        //拼接sql
        StringBuffer stb = new StringBuffer("select id,app_code as appCode,app_name as appName,data_code as dataCode," +
                "create_time as createTime,create_user as createUser,active,color,domain ," +
                "active_time as activeTime,update_time as updateTime from t_mgr_upstream where 1 = 1 and id not in ( ");
        //存入参数
        List<Object> param = new ArrayList<>();
        if (CollectionUtils.isEmpty(ids)) {
            return null;
        }
        for (String id : ids) {
            stb.append("?,");
            param.add(id);
        }
        stb.replace(stb.length() - 1, stb.length(), " ) ");
        stb.append(" and domain in (?");
        param.add(domainId);
        if (StringUtils.isNotBlank(AutoUpRunner.superDomainId)) {
            stb.append(",? ");
            param.add(AutoUpRunner.superDomainId);
        }
        stb.append(" ) ");
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(stb.toString(), param.toArray());

        ArrayList<Upstream> list = new ArrayList<>();
        if (!CollectionUtils.isEmpty(mapList)) {
            for (Map<String, Object> map : mapList) {
                Upstream upstream = new Upstream();
                BeanMap beanMap = BeanMap.create(upstream);
                beanMap.putAll(map);
                list.add(upstream);
            }
            return list;
        }
        return null;
    }

    @Override
    public List<Upstream> findByUpstreamTypeIds(ArrayList<String> ids, String domainId) {
        //拼接sql
        StringBuffer stb = new StringBuffer("select u.id,u.app_code as appCode,u.app_name as appName,u.data_code as dataCode," +
                "u.create_time as createTime,u.create_user as createUser,u.active,u.color,u.domain ," +
                "u.active_time as activeTime,u.update_time as updateTime from t_mgr_upstream u ,t_mgr_upstream_types t  where t.upstream_id =u.id and t.id in ( ");
        //存入参数
        List<Object> param = new ArrayList<>();
        if (CollectionUtils.isEmpty(ids)) {
            return null;
        }
        for (String id : ids) {
            stb.append("?,");
            param.add(id);
        }
        stb.replace(stb.length() - 1, stb.length(), " ) ");
        stb.append(" and u.domain in (?");
        param.add(domainId);
        if (StringUtils.isNotBlank(AutoUpRunner.superDomainId)) {
            stb.append(",? ");
            param.add(AutoUpRunner.superDomainId);
        }
        stb.append(" ) ");
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(stb.toString(), param.toArray());

        ArrayList<Upstream> list = new ArrayList<>();
        if (!CollectionUtils.isEmpty(mapList)) {
            for (Map<String, Object> map : mapList) {
                Upstream upstream = new Upstream();
                BeanMap beanMap = BeanMap.create(upstream);
                beanMap.putAll(map);
                list.add(upstream);
            }
            return list;
        }
        return null;
    }


    private void dealData(Map<String, Object> arguments, StringBuffer stb, List<Object> param) {
        Iterator<Map.Entry<String, Object>> it = arguments.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            if ("id".equals(entry.getKey())) {
                stb.append("and id= ? ");
                param.add(entry.getValue());
            }

            if ("filter".equals(entry.getKey())) {
                HashMap<String, Object> map = (HashMap<String, Object>) entry.getValue();
                for (Map.Entry<String, Object> str : map.entrySet()) {
                    if ("appCode".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if ("like".equals(FilterCodeEnum.getDescByCode(soe.getKey()))) {
                                stb.append("and app_code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if ("in".equals(FilterCodeEnum.getDescByCode(soe.getKey())) || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and app_code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and app_code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("appName".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if ("like".equals(FilterCodeEnum.getDescByCode(soe.getKey()))) {
                                stb.append("and app_name ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if ("in".equals(FilterCodeEnum.getDescByCode(soe.getKey())) || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and app_name ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and app_name ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("active".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            stb.append("and active ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                            param.add(soe.getValue());
                        }
                    }
                    if ("createTime".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            //判断是否是区间
                            if ("gt".equals(soe.getKey()) || "lt".equals(soe.getKey())
                                    || "gte".equals(soe.getKey()) || "lte".equals(soe.getKey())) {
                                stb.append("and create_time ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());

                            }
                        }
                    }

                    if ("updateTime".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            //判断是否是区间
                            if ("gt".equals(soe.getKey()) || "lt".equals(soe.getKey())
                                    || "gte".equals(soe.getKey()) || "lte".equals(soe.getKey())) {
                                stb.append("and update_time ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());

                            }
                        }
                    }
                    if ("dataCode".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "like")) {
                                stb.append("and data_code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and data_code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and data_code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("color".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("like")) {
                                stb.append("and color ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and color ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and color ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }

                }

            }
        }
    }
}
