package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.DeptTreeType;
import com.qtgl.iga.dao.DeptTreeTypeDao;
import com.qtgl.iga.utils.FilterCodeEnum;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.util.*;


@Repository
@Component
public class DeptTreeTypeDaoImpl implements DeptTreeTypeDao {


    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;


    @Override
    public List<DeptTreeType> findAll(Map<String, Object> arguments, String domain) {
        String sql = "select id, code, name, description," +
                "multiple_root_node as multipleRootNode, create_time as createTime," +
                "update_time as updateTime, create_user as createUser, domain ,tree_index as treeIndex " +
                "from t_mgr_dept_tree_type where 1 = 1 ";
        //拼接sql
        StringBuffer stb = new StringBuffer(sql);
        //存入参数
        List<Object> param = new ArrayList<>();

        dealData(arguments, stb, param);
        System.out.println(stb.toString());
        stb.append("order by tree_index");
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(stb.toString(), param.toArray());
        ArrayList<DeptTreeType> list = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {
                DeptTreeType deptTreeType = new DeptTreeType();
                BeanMap beanMap = BeanMap.create(deptTreeType);
                beanMap.putAll(map);
                System.out.println(deptTreeType);
                list.add(deptTreeType);
            }
            return list;
        }

        return null;
    }


    @Override
    @Transactional
    public DeptTreeType saveDeptTreeType(DeptTreeType deptTreeType, String domain) throws Exception {
        //判重
        Object[] param = new Object[]{deptTreeType.getCode(), deptTreeType.getName()};
        List<Map<String, Object>> mapList = jdbcIGA.queryForList("select  * from t_mgr_dept_tree_type where code =? or name = ?", param);
        if (null != mapList && mapList.size() > 0) {
            throw new Exception("code 或 name 不能重复,添加组织机构树类别失败");
        }
        String sql = "insert into t_mgr_dept_tree_type  values(?,?,?,?,?,?,?,?,?,?)";
        //生成主键和时间
        String id = UUID.randomUUID().toString().replace("-", "");
        deptTreeType.setId(id);
        Timestamp date = new Timestamp(System.currentTimeMillis());
        deptTreeType.setCreateTime(date);
        deptTreeType.setUpdateTime(date);
        int update = jdbcIGA.update(sql, preparedStatement -> {
            preparedStatement.setObject(1, id);
            preparedStatement.setObject(2, deptTreeType.getCode());
            preparedStatement.setObject(3, deptTreeType.getName());
            preparedStatement.setObject(4, deptTreeType.getDescription());
            preparedStatement.setObject(5, deptTreeType.getMultipleRootNode());
            preparedStatement.setObject(6, deptTreeType.getCreateTime());
            preparedStatement.setObject(7, deptTreeType.getUpdateTime());
            preparedStatement.setObject(8, deptTreeType.getCreateUser());
            preparedStatement.setObject(9, domain);
            preparedStatement.setObject(10, deptTreeType.getTreeIndex());

        });
        return update > 0 ? deptTreeType : null;
    }

    @Override
    @Transactional
    public DeptTreeType deleteDeptTreeType(String id, String domain) throws Exception {
        Object[] objects = new Object[2];
        objects[0] = id;
        objects[1] = domain;
        List<Map<String, Object>> mapList = jdbcIGA.queryForList("select  * from t_mgr_dept_tree_type  where id =? and domain=?", objects);
        ArrayList<DeptTreeType> list = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {

            for (Map<String, Object> map : mapList) {
                DeptTreeType deptTreeType = new DeptTreeType();
                BeanMap beanMap = BeanMap.create(deptTreeType);
                beanMap.putAll(map);
                list.add(deptTreeType);
            }
        }
        if (null == list || list.size() > 1 || list.size() == 0) {
            throw new Exception("数据异常，删除失败");
        }
        DeptTreeType deptTreeType = list.get(0);


        //删除组织类别树数据
        String sql = "delete from t_mgr_dept_tree_type  where id =? and domain =? ";
        int i = jdbcIGA.update(sql, id, domain);


        return i > 0 ? deptTreeType : null;

    }

    @Override
    @Transactional
    public DeptTreeType updateDeptTreeType(DeptTreeType deptTreeType) throws Exception {
        //判重
        Object[] param = new Object[]{deptTreeType.getCode(), deptTreeType.getName(), deptTreeType.getId()};
        List<Map<String, Object>> mapList = jdbcIGA.queryForList("select  * from t_mgr_dept_tree_type where (code = ? or name = ?) and id != ?  ", param);
        if (null != mapList && mapList.size() > 0) {
            throw new Exception("code 或 name 不能重复,修改组织机构类别树失败");
        }
        String sql = "update t_mgr_dept_tree_type  set code = ?,name = ?,description = ?,multiple_root_node = ?,create_time = ?," +
                "update_time = ?,create_user = ?,domain= ?,tree_index=?  where id=?";
        Date date = new Date();
        return jdbcIGA.update(sql, preparedStatement -> {
            preparedStatement.setObject(1, deptTreeType.getCode());
            preparedStatement.setObject(2, deptTreeType.getName());
            preparedStatement.setObject(3, deptTreeType.getDescription());
            preparedStatement.setObject(4, deptTreeType.getMultipleRootNode());
            preparedStatement.setObject(5, deptTreeType.getCreateTime());
            preparedStatement.setObject(6, date);
            preparedStatement.setObject(7, deptTreeType.getCreateUser());
            preparedStatement.setObject(8, deptTreeType.getDomain());
            preparedStatement.setObject(9, deptTreeType.getTreeIndex());
            preparedStatement.setObject(10, deptTreeType.getId());
        }) > 0 ? deptTreeType : null;
    }

    @Override
    public DeptTreeType findById(String id) {
        String sql = "select id, code, name, description," +
                "multiple_root_node as multipleRootNode, create_time as createTime," +
                "update_time as updateTime, create_user as createUser, domain ,tree_index as treeIndex " +
                "from t_mgr_dept_tree_type where id= ?  order by tree_index";

        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, id);
        DeptTreeType deptTreeType = new DeptTreeType();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {

                BeanMap beanMap = BeanMap.create(deptTreeType);
                beanMap.putAll(map);
            }
            return deptTreeType;
        }
        return null;
    }

    @Override
    public DeptTreeType findByCode(String treeType) {
        String sql = "select id, code, name, description," +
                "multiple_root_node as multipleRootNode, create_time as createTime," +
                "update_time as updateTime, create_user as createUser, domain ,tree_index as treeIndex " +
                "from t_mgr_dept_tree_type where code = ?  order by tree_index";

        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, treeType);
        DeptTreeType deptTreeType = new DeptTreeType();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {

                BeanMap beanMap = BeanMap.create(deptTreeType);
                beanMap.putAll(map);
            }
            return deptTreeType;
        }
        return null;
    }

    @Override
    public void initialization(String domain) {
        String sql = "INSERT INTO t_mgr_dept_tree_type (id, code, name, description, multiple_root_node, create_time, update_time,\n" +
                "                                  create_user, domain, tree_index)\n" +
                "VALUES (UUID(), '01', '单位类型组织机构', null, null, now(), now(), 'iga',?, 1)\n" +
                "     , (UUID(), '02', '党务类型组织机构', null, null, now(), now(), 'iga',?, 2)\n" +
                "     , (UUID(), '03', '学术类型组织机构', null, null, now(), now(), 'iga',?, 3)\n" +
                "     , (UUID(), '04', '小组类型组织机构', null, null, now(), now(), 'iga',?, 4);";

        jdbcIGA.update(sql, preparedStatement -> {
            preparedStatement.setObject(1, domain);
            preparedStatement.setObject(2, domain);
            preparedStatement.setObject(3, domain);
            preparedStatement.setObject(4, domain);

        });
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
                    if ("code".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("like")) {
                                stb.append("and code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("name".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("like")) {
                                stb.append("and name ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and name ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and name ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("createTime".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            //判断是否是区间
                            if (soe.getKey().equals("gt") || soe.getKey().equals("lt")
                                    || soe.getKey().equals("gte") || soe.getKey().equals("lte")) {
                                stb.append("and create_time ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());

                            }
                        }
                    }

                    if (str.getKey().equals("updateTime")) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            //判断是否是区间
                            if (soe.getKey().equals("gt") || soe.getKey().equals("lt")
                                    || soe.getKey().equals("gte") || soe.getKey().equals("lte")) {
                                stb.append("and update_time ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());

                            }
                        }
                    }

                }
            }
        }
    }
}
