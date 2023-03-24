package com.qtgl.iga.dao.impl;

import com.qtgl.iga.AutoUpRunner;
import com.qtgl.iga.bean.NodeDto;
import com.qtgl.iga.bo.Node;
import com.qtgl.iga.bo.NodeRulesRange;
import com.qtgl.iga.dao.NodeDao;
import com.qtgl.iga.utils.MyBeanUtils;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.enums.FilterCodeEnum;
import com.qtgl.iga.utils.exception.CustomException;
import com.qtgl.iga.vo.NodeRulesVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;


@Repository
@Slf4j
public class NodeDaoImpl implements NodeDao {

    //超级租户  node规则
    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;

    @Resource(name = "iga-txTemplate")
    TransactionTemplate txTemplate;

    @Override
    public NodeDto save(NodeDto node) {

        String sql = "insert into t_mgr_node  (id,manual,node_code,create_time,update_time,domain,dept_tree_type,status,type)values(?,?,?,?,?,?,?,?,?)";
        //生成主键和时间
        if (StringUtils.isBlank(node.getId())) {
            String id = UUID.randomUUID().toString().replace("-", "");
            node.setId(id);
        }
        node.setCreateTime(null == node.getCreateTime() ? System.currentTimeMillis() : node.getCreateTime());
        int update = jdbcIGA.update(sql, preparedStatement -> {
            preparedStatement.setObject(1, node.getId());
            preparedStatement.setObject(2, node.getManual());
            preparedStatement.setObject(3, node.getNodeCode());
            preparedStatement.setObject(4, new Timestamp(node.getCreateTime()));
            preparedStatement.setObject(5, null);
            preparedStatement.setObject(6, node.getDomain());
            preparedStatement.setObject(7, node.getDeptTreeType());
            preparedStatement.setObject(8, node.getStatus());
            preparedStatement.setObject(9, node.getType());

        });
        return update > 0 ? node : null;
    }

    @Override
    public List<Node> getByCode(String domain, String deptTreeType, String nodeCode, Integer status, String type) {
        List<Node> nodes = new ArrayList<>();

        String sql = "select id,manual," +
                "node_code as nodeCode," +
                "create_time as createTime,update_time as updateTime,domain,dept_tree_type as deptTreeType,status,type" +
                " from t_mgr_node where domain=? and dept_tree_type=? and node_code=?   and status=?  and type=?  ";
        List<Map<String, Object>> mapList = new ArrayList<>();
        if (null == nodeCode) {
            sql = sql.replace("node_code=?", "node_code is null or node_code=\"\"");
            mapList = jdbcIGA.queryForList(sql, domain, deptTreeType, status, type);
        }

        if (null == deptTreeType) {
            sql = sql.replace("dept_tree_type=? ", " (dept_tree_type is null or dept_tree_type=\"\") ");
            mapList = jdbcIGA.queryForList(sql, domain, nodeCode, status, type);
        }
        if (null != deptTreeType && null != nodeCode) {
            mapList = jdbcIGA.queryForList(sql, domain, deptTreeType, nodeCode, status, type);

        }
        for (Map<String, Object> map : mapList) {
            Node node = new Node();
            try {
                MyBeanUtils.populate(node, map);
                nodes.add(node);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return nodes;
    }

    @Override
    public Integer deleteNode(Map<String, Object> arguments, String domain) {
        Object[] params = new Object[2];
        params[0] = arguments.get("id");
        params[1] = domain;
        String sql = "delete from t_mgr_node where  id = ? and domain = ? ";

        return jdbcIGA.update(sql, params);
    }

    @Override
    public List<Node> findNodes(Map<String, Object> arguments, String domain) {
        ArrayList<Node> nodes = new ArrayList<>();
        Integer status = null == arguments.get("status") ? null : (Integer) arguments.get("status");
        Object version = arguments.get("version");
        Object type = arguments.get("type");
        String sql = "select id,manual," +
                "node_code as nodeCode," +
                "create_time as createTime,update_time as updateTime,domain,dept_tree_type as deptTreeType,status,type" +
                " from t_mgr_node where domain= ?    ";
        //拼接sql
        StringBuffer stb = new StringBuffer(sql);
        //存入参数
        List<Object> param = new ArrayList<>();
        param.add(domain);
        if (null != type) {
            stb.append(" and type =?  ");
            param.add(type);
        }
        if (null != status) {
            stb.append(" and status =?  ");
            if (StringUtils.isNotBlank(AutoUpRunner.superDomainId) && domain.equals(AutoUpRunner.superDomainId)) {
                param.add(0);
            } else {
                param.add(status);
            }
        }
        if (null != version) {
            stb.append(" and create_time =?  ");
            param.add(version);
        }
        dealData(arguments, stb, param);
//        getChild(arguments,param,stb);
        log.info("sql:{}", stb.toString());
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(stb.toString(), param.toArray());

        for (Map<String, Object> map : mapList) {
            try {
                Node node = new Node();
                MyBeanUtils.populate(node, map);
                nodes.add(node);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return nodes;
    }

    @Override
    public List<Node> findByTreeTypeCode(String code, Integer status, String domain) {
        ArrayList<Node> nodes = new ArrayList<>();

        String sql = "select id,manual," +
                "node_code as nodeCode," +
                "create_time as createTime,update_time as updateTime,domain,dept_tree_type as deptTreeType,status,type" +
                " from t_mgr_node where dept_tree_type= ? and status=? and domain =? ";
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, code, status, domain);

        return getNodes(nodes, mapList);
    }

    @Override
    public List<Node> findNodesPlus(Map<String, Object> arguments, String domainId) {
        ArrayList<Node> nodes = new ArrayList<>();

        List<String> codes = (List<String>) arguments.get("codes");
        String treeType = (String) arguments.get("treeType");
        Integer status = (Integer) arguments.get("status");
        String type = (String) arguments.get("type");
        String sql = "select id,manual," +
                "node_code as nodeCode," +
                "create_time as createTime,update_time as updateTime,domain,dept_tree_type as deptTreeType, status,type" +
                " from t_mgr_node where domain= ? and status=? and type=? ";
        List<Object> param = new ArrayList<>();
        param.add(domainId);
        if (StringUtils.isNotBlank(AutoUpRunner.superDomainId) && domainId.equals(AutoUpRunner.superDomainId)) {
            param.add(0);
        } else {
            param.add(status);
        }
        param.add(type);
        if (StringUtils.isNotBlank(treeType)) {
            sql = sql + " and dept_tree_type =?  ";
            param.add(treeType);
        } else {
            sql = sql + " and (dept_tree_type is null or dept_tree_type= \"\")";
        }
        sql = sql + "and node_code in (";

        sql = handleSql(sql, codes, param);
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, param.toArray());

        return getNodes(nodes, mapList);
    }

    @Override
    public List<Node> findNodesByCode(String code, String domain, String type) {
        ArrayList<Node> nodes = new ArrayList<>();
        String sql = "select id,manual," +
                "node_code as nodeCode," +
                "create_time as createTime,update_time as updateTime,domain,dept_tree_type as deptTreeType,status,type" +
                " from t_mgr_node where domain= ?  and node_code =? and type=?  ";
        //存入参数
        List<Object> param = new ArrayList<>();
        param.add(domain);

        param.add(code);
        param.add(type);
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, param.toArray());
        if (null == mapList || mapList.size() == 0) {
            return null;
        }
        for (Map<String, Object> map : mapList) {
            try {
                Node node = new Node();
                MyBeanUtils.populate(node, map);
                nodes.add(node);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return nodes;
    }

    @Override
    public Integer makeNodeToHistory(String domain, Integer status, String id) {
        String str = "update t_mgr_node set  status= ? " +
                "where domain =? and id=?  ";
        int update = jdbcIGA.update(str, preparedStatement -> {

            preparedStatement.setObject(1, status);
            preparedStatement.setObject(2, domain);
            preparedStatement.setObject(3, id);
        });
        return update;
    }

    @Override
    public List<Node> findNodesByStatusAndType(Integer status, String type, String domain, Timestamp version) {
        ArrayList<Node> nodes = new ArrayList<>();
        String sql = "select id,manual," +
                "node_code as nodeCode," +
                "create_time as createTime,update_time as updateTime,domain,dept_tree_type as deptTreeType,status,type" +
                " from t_mgr_node where domain= ?  and type =?    ";
        List<Object> param = new ArrayList<>();
        param.add(domain);
        param.add(type);
        //存入参数
        if (null != status) {
            sql = sql + " and status = ? ";
            param.add(status);
        }
        if (null != version) {
            sql = sql + " and create_time = ?";
            param.add(version);
        }

        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, param.toArray());
        if (!CollectionUtils.isEmpty(mapList)) {
            for (Map<String, Object> map : mapList) {
                try {
                    Node node = new Node();
                    MyBeanUtils.populate(node, map);
                    nodes.add(node);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return nodes;
        }
        return null;
    }

    @Override
    public List<Node> findById(String id) {
        ArrayList<Node> nodes = new ArrayList<>();
        String sql = "select id,manual," +
                "node_code as nodeCode," +
                "create_time as createTime,update_time as updateTime,domain,dept_tree_type as deptTreeType,status,type" +
                " from t_mgr_node where id=?  ";
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, id);
        if (null == mapList || mapList.size() == 0) {
            return null;
        }
        for (Map<String, Object> map : mapList) {
            try {
                Node node = new Node();
                MyBeanUtils.populate(node, map);
                nodes.add(node);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (null != nodes && nodes.size() > 0) {
            return nodes;
        }
        return null;
    }

    @Override
    public List<Node> findByStatus(Integer status, String domain, String type) {
        ArrayList<Node> nodes = new ArrayList<>();
        String sql = "select id,manual," +
                "node_code as nodeCode," +
                "create_time as createTime,update_time as updateTime,domain,dept_tree_type as deptTreeType,status,type" +
                " from t_mgr_node where status=? and domain =? and type = ? ";
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, status, domain, type);
        if (null == mapList || mapList.size() == 0) {
            return null;
        }
        for (Map<String, Object> map : mapList) {
            try {
                Node node = new Node();
                MyBeanUtils.populate(node, map);
                nodes.add(node);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (null != nodes && nodes.size() > 0) {
            return nodes;
        }
        return null;
    }

    @Override
    public Integer deleteNodeById(String id, String domain) {
        Object[] params = new Object[2];
        params[0] = id;
        params[1] = domain;
        String sql = "delete from t_mgr_node where  id = ? and domain = ? ";

        return jdbcIGA.update(sql, params);
    }

    private List<Node> getNodes(ArrayList<Node> nodes, List<Map<String, Object>> mapList) {
        for (Map<String, Object> map : mapList) {
            try {
                Node node = new Node();
                MyBeanUtils.populate(node, map);
                nodes.add(node);


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return nodes;

    }

    @Override
    public Integer updateNodeAndRules(ArrayList<Node> invalidNodes, ArrayList<NodeRulesVo> invalidNodeRules, ArrayList<NodeRulesRange> invalidNodeRulesRanges) {
        return txTemplate.execute(transactionStatus -> {
            try {
                if (!CollectionUtils.isEmpty(invalidNodes)) {
                    String nodeStr = "UPDATE `t_mgr_node` SET  `update_time` = ? ,  `status` = 3  WHERE `id` = ? and domain = ?";
                    jdbcIGA.batchUpdate(nodeStr, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, new Timestamp(System.currentTimeMillis()));
                            preparedStatement.setObject(2, invalidNodes.get(i).getId());
                            preparedStatement.setObject(3, invalidNodes.get(i).getDomain());
                        }

                        @Override
                        public int getBatchSize() {
                            return invalidNodes.size();
                        }
                    });
                }
                if (!CollectionUtils.isEmpty(invalidNodeRules)) {
                    String rulesStr = "UPDATE `t_mgr_node_rules` SET  `update_time` = ?,  `status` = 3 WHERE `id` = ? ";
                    jdbcIGA.batchUpdate(rulesStr, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, new Timestamp(System.currentTimeMillis()));
                            preparedStatement.setObject(2, invalidNodeRules.get(i).getId());
                        }

                        @Override
                        public int getBatchSize() {
                            return invalidNodeRules.size();
                        }
                    });
                }
                if (!CollectionUtils.isEmpty(invalidNodeRulesRanges)) {
                    String rangeStr = "UPDATE `t_mgr_node_rules_range` SET  `update_time` = ? ,  `status` = 3  WHERE `id` = ? ";
                    jdbcIGA.batchUpdate(rangeStr, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, new Timestamp(System.currentTimeMillis()));
                            preparedStatement.setObject(2, invalidNodeRulesRanges.get(i).getId());
                        }

                        @Override
                        public int getBatchSize() {
                            return invalidNodeRulesRanges.size();
                        }
                    });
                }

                return 1;
            } catch (Exception e) {
                e.printStackTrace();
                transactionStatus.setRollbackOnly();
                throw new CustomException(ResultCode.FAILED, "标记失效规则异常！");
            }
        });

    }

    @Override
    public List<Node> getByTreeType(String domain, String deptTreeTypeCode, Integer status, String type) {
        ArrayList<Node> nodes = new ArrayList<>();
        List<Map<String, Object>> mapList;

        String sql = "select id,manual," +
                "node_code as nodeCode," +
                "create_time as createTime,update_time as updateTime,domain,dept_tree_type as deptTreeType,status,type" +
                " from t_mgr_node where type =?  and  dept_tree_type= ? and status=? and domain =? ";
        if (null == deptTreeTypeCode) {
            sql = sql.replace("dept_tree_type= ? ", " (dept_tree_type is null or dept_tree_type=\"\") ");
            mapList = jdbcIGA.queryForList(sql, type, status, domain);
        } else {
            mapList = jdbcIGA.queryForList(sql, type, deptTreeTypeCode, status, domain);
        }
        return getNodes(nodes, mapList);
    }

    @Override
    public List<Node> findNodes(String domainId, Integer status, String type) {
        ArrayList<Node> nodes = new ArrayList<>();
        //拼接sql
        StringBuffer stb = new StringBuffer("select id,manual," +
                "node_code as nodeCode," +
                "create_time as createTime,update_time as updateTime,domain,dept_tree_type as deptTreeType,status,type" +
                " from t_mgr_node where domain= ?    ");
        //存入参数
        List<Object> param = new ArrayList<>();
        param.add(domainId);
        if (null != type) {
            stb.append(" and type =?  ");
            param.add(type);
        }
        if (null != status) {
            stb.append(" and status =?  ");
            param.add(status);
        }
        stb.append(" order by create_time");
        log.info("sql:{}", stb.toString());
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(stb.toString(), param.toArray());

        for (Map<String, Object> map : mapList) {
            try {
                Node node = new Node();
                MyBeanUtils.populate(node, map);
                nodes.add(node);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return nodes;
    }

    @Override
    public List<Node> findNodeById(String id) {
        List<Node> nodes = new ArrayList<>();
        String sql = "select id,manual,node_code as nodeCode,create_time as createTime,update_time as updateTime,domain,dept_tree_type as deptTreeType,status,type" +
                " from t_mgr_node where id= ?    ";
        log.info("sql:{}", sql);
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, id);

        if (!CollectionUtils.isEmpty(mapList)) {
            for (Map<String, Object> map : mapList) {
                try {
                    Node node = new Node();
                    MyBeanUtils.populate(node, map);
                    nodes.add(node);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return nodes;
    }

    @Override
    public Node findNodeByIdAndDomain(String id, String domain) {
        String sql = "select id,manual,node_code as nodeCode,create_time as createTime,update_time as updateTime,domain,dept_tree_type as deptTreeType,status,type" +
                " from t_mgr_node where id= ?  and domain=?   ";
        log.info("sql:{}", sql);
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, id,domain);

        if (!CollectionUtils.isEmpty(mapList) && mapList.size() == 1) {
            for (Map<String, Object> map : mapList) {
                try {
                    Node node = new Node();
                    MyBeanUtils.populate(node, map);
                    return node;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }


    private String handleSql(String sql, List<String> codes, List<Object> param) {
        StringBuffer stb = new StringBuffer(sql);
        for (String code : codes) {
            stb.append(" ?,");
            param.add(code);
        }
        String s = stb.toString();
        s = s.substring(0, s.length() - 1) + ")";
        return s;
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
                    if ("deptTreeType".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "=")) {
                                stb.append("and dept_tree_type ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }


                    }
                    if ("nodeCode".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "=")) {
                                stb.append(" and node_code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }


                    }


                }

            }

        }
    }


}
