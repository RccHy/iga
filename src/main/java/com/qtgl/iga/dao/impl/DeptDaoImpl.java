package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.Dept;
import com.qtgl.iga.bo.DynamicAttr;
import com.qtgl.iga.bo.DynamicValue;
import com.qtgl.iga.dao.DeptDao;
import com.qtgl.iga.utils.MyBeanUtils;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;


@Repository
@Component
public class DeptDaoImpl implements DeptDao {


    @Resource(name = "jdbcSSO")
    JdbcTemplate jdbcSSOAPI;


    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;

    @Resource(name = "api-txTemplate")
    TransactionTemplate txTemplate;


    @Resource(name = "iga-txTemplate")
    TransactionTemplate igaTemplate;

    @Override
    public Dept findById(String id) {
        String sql = "select id, code, name,independent, type_id as typeId,create_time as createTime,abbreviation,relation_type as relationType,dept_en_name as enName,dept_index as 'index',active_time as activeTime,orphan  from dept where id= ? ";

        List<Map<String, Object>> mapList = jdbcSSOAPI.queryForList(sql, id);
        Dept dept = new Dept();
        if (null != mapList && mapList.size() == 1) {
            for (Map<String, Object> map : mapList) {

                BeanMap beanMap = BeanMap.create(dept);
                beanMap.putAll(map);
            }
            return dept;
        }
        return null;
    }

    @Override
    public List<TreeBean> findByTenantId(String id, String treeType, Integer delMark) {
        String sql = "select id ,dept_code as code , dept_name as name , parent_code as parentCode ,independent,dept_en_name as enName, " +
                " create_time as createTime , source, tree_type as treeType,data_source as dataSource, abbreviation,tags,type,update_time as updateTime,del_mark as delMark,active,relation_type as relationType,dept_en_name as enName,dept_index as 'index',active_time as activeTime,orphan  from dept where tenant_id = ? ";
        List<Object> param = new ArrayList<>();
        param.add(id);
        if (null != treeType) {
            sql = sql + " and tree_type=? ";
            param.add(treeType);
        }
        if (null != treeType) {
            sql = sql + " and del_mark=? ";
            param.add(delMark);
        }
        List<Map<String, Object>> mapList = jdbcSSOAPI.queryForList(sql, param.toArray());
        return getDeptBeans(mapList);
    }


    private List<Dept> getDepts(List<Map<String, Object>> mapList) {
        ArrayList<Dept> list = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {
                Dept dept = new Dept();
                BeanMap beanMap = BeanMap.create(dept);
                beanMap.putAll(map);
                list.add(dept);
            }
            return list;
        }

        return null;
    }

    private List<TreeBean> getDeptBeans(List<Map<String, Object>> mapList) {
        ArrayList<TreeBean> list = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {
                TreeBean dept = new TreeBean();
                try {
                    MyBeanUtils.populate(dept, map);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
                if (null == dept.getParentCode()) {
                    dept.setParentCode("");
                }
                if (null == dept.getOrphan()) {
                    dept.setOrphan(0);
                }
                list.add(dept);
            }
            return list;
        }

        return null;
    }

    @Override
    public ArrayList<TreeBean> updateDept(ArrayList<TreeBean> list, String tenantId) {
        String str = "update dept set  dept_name=?,dept_en_name=?, parent_code=?, del_mark=? ,tenant_id =?" +
                ",source =?, data_source=?, description=?,update_time=?,tags=?,tree_type= ?,active=? ,abbreviation=?,del_mark=0 ," +
                " type = ?,relation_type=?,dept_en_name=?,independent=? ,active_time=?  " +
                " where dept_code =? and update_time< ? ";
        boolean contains = false;

        int[] ints = jdbcSSOAPI.batchUpdate(str, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                preparedStatement.setObject(1, list.get(i).getName());
                preparedStatement.setObject(2, list.get(i).getEnName());
                preparedStatement.setObject(3, list.get(i).getParentCode());
                preparedStatement.setObject(4, 0);
                preparedStatement.setObject(5, tenantId);
                preparedStatement.setObject(6, list.get(i).getSource());
                preparedStatement.setObject(7, "PULL");
                preparedStatement.setObject(8, list.get(i).getDescription());
                preparedStatement.setObject(9, list.get(i).getCreateTime() == null ? LocalDateTime.now() : list.get(i).getCreateTime());
                preparedStatement.setObject(10, list.get(i).getTags());
                preparedStatement.setObject(11, list.get(i).getTreeType());
                preparedStatement.setObject(12, 0);
                preparedStatement.setObject(13, null == list.get(i).getAbbreviation() ? null : list.get(i).getAbbreviation());
                preparedStatement.setObject(14, null == list.get(i).getType() ? null : list.get(i).getType());
                preparedStatement.setObject(15, list.get(i).getRelationType());
                preparedStatement.setObject(16, list.get(i).getEnName());
                preparedStatement.setObject(17, null == list.get(i).getIndependent() ? 0 : list.get(i).getIndependent());
                preparedStatement.setObject(18, list.get(i).getActiveTime());
                preparedStatement.setObject(19, list.get(i).getCode());
                preparedStatement.setObject(20, list.get(i).getUpdateTime() == null ? LocalDateTime.now() : list.get(i).getUpdateTime());

            }

            @Override
            public int getBatchSize() {
                return list.size();
            }
        });

        contains = Arrays.toString(ints).contains("-1");


        return contains ? null : list;
    }

    @Override
    public ArrayList<TreeBean> saveDept(ArrayList<TreeBean> list, String tenantId) {
        String str = "insert into dept (id,dept_code, dept_name,dept_en_name, parent_code, del_mark ,tenant_id ,source, data_source, description,create_time,tags,active,active_time,tree_type,dept_index,abbreviation,update_time,type,relation_type,independent) values" +
                "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        boolean contains = false;

        int[] ints = jdbcSSOAPI.batchUpdate(str, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                preparedStatement.setObject(1, UUID.randomUUID().toString().replace("-", ""));
                preparedStatement.setObject(2, list.get(i).getCode());
                preparedStatement.setObject(3, list.get(i).getName());
                preparedStatement.setObject(4, list.get(i).getEnName());
                preparedStatement.setObject(5, list.get(i).getParentCode());
                preparedStatement.setObject(6, 0);
                preparedStatement.setObject(7, tenantId);
                preparedStatement.setObject(8, list.get(i).getSource());
                preparedStatement.setObject(9, "PULL");
                preparedStatement.setObject(10, list.get(i).getDescription());
                preparedStatement.setObject(11, list.get(i).getCreateTime() == null ? LocalDateTime.now() : list.get(i).getCreateTime());
                preparedStatement.setObject(12, list.get(i).getTags());
                preparedStatement.setObject(13, 0);
                preparedStatement.setObject(14, LocalDateTime.now());
                preparedStatement.setObject(15, list.get(i).getTreeType());
                preparedStatement.setObject(16, null == list.get(i).getIndex() ? null : list.get(i).getIndex());
                preparedStatement.setObject(17, null == list.get(i).getAbbreviation() ? null : list.get(i).getAbbreviation());
                preparedStatement.setObject(18, LocalDateTime.now());
                preparedStatement.setObject(19, list.get(i).getType());
                preparedStatement.setObject(20, list.get(i).getRelationType());
                preparedStatement.setObject(21, null == list.get(i).getIndependent() ? 0 : list.get(i).getIndependent());
            }

            @Override
            public int getBatchSize() {
                return list.size();
            }
        });
        contains = Arrays.toString(ints).contains("-1");


        return contains ? null : list;
    }

    @Override
    public ArrayList<TreeBean> deleteDept(ArrayList<TreeBean> list) {
        String str = "update dept set   del_mark= ? , active = ?,active_time= ?  " +
                "where dept_code =?";
        boolean contains = false;

        int[] ints = jdbcSSOAPI.batchUpdate(str, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                preparedStatement.setObject(1, 1);
                preparedStatement.setObject(2, 1);
                preparedStatement.setObject(3, LocalDateTime.now());
                preparedStatement.setObject(4, list.get(i).getCode());
            }

            @Override
            public int getBatchSize() {
                return list.size();
            }
        });
        contains = Arrays.toString(ints).contains("-1");


        return contains ? null : list;
    }

    @Override
    public Integer renewData(ArrayList<TreeBean> insertList, ArrayList<TreeBean> updateList, ArrayList<TreeBean> deleteList, ArrayList<TreeBean> invalidList, List<DynamicValue> valueUpdate, List<DynamicValue> valueInsert, String tenantId) {
        String insertStr = "insert into dept (id,dept_code, dept_name,dept_en_name, parent_code, del_mark ,tenant_id ,source, data_source, description," +
                "create_time,tags,active,active_time,tree_type,dept_index,abbreviation,update_time,type,relation_type,independent,create_data_source,create_source) values" +
                "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        return txTemplate.execute(transactionStatus -> {

            try {
                if (null != insertList && insertList.size() > 0) {
                    jdbcSSOAPI.batchUpdate(insertStr, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, insertList.get(i).getId());
                            preparedStatement.setObject(2, insertList.get(i).getCode());
                            preparedStatement.setObject(3, insertList.get(i).getName());
                            preparedStatement.setObject(4, insertList.get(i).getEnName());
                            preparedStatement.setObject(5, insertList.get(i).getParentCode());
                            preparedStatement.setObject(6, insertList.get(i).getDelMark());
                            preparedStatement.setObject(7, tenantId);
                            preparedStatement.setObject(8, insertList.get(i).getSource());
                            preparedStatement.setObject(9, insertList.get(i).getDataSource());
                            preparedStatement.setObject(10, insertList.get(i).getDescription());
                            preparedStatement.setObject(11, insertList.get(i).getCreateTime());
                            preparedStatement.setObject(12, insertList.get(i).getTags());
                            preparedStatement.setObject(13, insertList.get(i).getActive());
                            preparedStatement.setObject(14, LocalDateTime.now());
                            preparedStatement.setObject(15, insertList.get(i).getTreeType());
                            preparedStatement.setObject(16, insertList.get(i).getIndex());
                            preparedStatement.setObject(17, insertList.get(i).getAbbreviation());
                            preparedStatement.setObject(18, insertList.get(i).getUpdateTime());
                            preparedStatement.setObject(19, insertList.get(i).getType());
                            preparedStatement.setObject(20, insertList.get(i).getRelationType());
                            preparedStatement.setObject(21, null == insertList.get(i).getIndependent() ? 0 : insertList.get(i).getIndependent());
                            preparedStatement.setObject(22, insertList.get(i).getDataSource());
                            preparedStatement.setObject(23, insertList.get(i).getSource());
                        }

                        @Override
                        public int getBatchSize() {
                            return insertList.size();
                        }
                    });
                }
                String updateStr = "update dept set  dept_name=?,dept_en_name=?, parent_code=?, del_mark=? ,tenant_id =?" +
                        ",source =?, data_source=?, description=?,update_time=?,tags=?,tree_type= ?,active=? ,abbreviation=?,type = ?,dept_index=?,relation_type=?,independent=?  " +
                        ", active_time = ?  where dept_code =? and tenant_id=? and  update_time<= ?";
                if (null != updateList && updateList.size() > 0) {
                    int[] i = jdbcSSOAPI.batchUpdate(updateStr, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, updateList.get(i).getName());
                            preparedStatement.setObject(2, updateList.get(i).getEnName());
                            preparedStatement.setObject(3, updateList.get(i).getParentCode());
                            preparedStatement.setObject(4, null == updateList.get(i).getDelMark() ? 0 : updateList.get(i).getDelMark());
                            preparedStatement.setObject(5, tenantId);
                            preparedStatement.setObject(6, updateList.get(i).getSource());
                            preparedStatement.setObject(7, updateList.get(i).getDataSource());
                            preparedStatement.setObject(8, updateList.get(i).getDescription());
                            preparedStatement.setObject(9, updateList.get(i).getUpdateTime());
                            preparedStatement.setObject(10, updateList.get(i).getTags());
                            preparedStatement.setObject(11, updateList.get(i).getTreeType());
                            preparedStatement.setObject(12, updateList.get(i).getActive());
                            preparedStatement.setObject(13, updateList.get(i).getAbbreviation());
                            preparedStatement.setObject(14, updateList.get(i).getType());
                            preparedStatement.setObject(15, updateList.get(i).getIndex());
                            preparedStatement.setObject(16, updateList.get(i).getRelationType());
                            preparedStatement.setObject(17, null == updateList.get(i).getIndependent() ? 0 : updateList.get(i).getIndependent());
                            preparedStatement.setObject(18, updateList.get(i).getActiveTime());
                            preparedStatement.setObject(19, updateList.get(i).getCode());
                            preparedStatement.setObject(20, tenantId);
                            preparedStatement.setObject(21, updateList.get(i).getUpdateTime());

                        }

                        @Override
                        public int getBatchSize() {
                            return updateList.size();
                        }
                    });
                }
                String deleteStr = "update dept set   active = ?,active_time= ?,del_mark=? ,update_time =?, data_source=?  " +
                        "where dept_code =? and tenant_id = ? and update_time<= ? ";
                ArrayList<TreeBean> treeBeans = new ArrayList<>();
                if (null != deleteList && deleteList.size() > 0) {
                    treeBeans.addAll(deleteList);
                }
                if (null != invalidList && invalidList.size() > 0) {
                    treeBeans.addAll(invalidList);
                }
                if (null != treeBeans && treeBeans.size() > 0) {
                    jdbcSSOAPI.batchUpdate(deleteStr, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, treeBeans.get(i).getActive());
                            preparedStatement.setObject(2, LocalDateTime.now());
                            preparedStatement.setObject(3, treeBeans.get(i).getDelMark());
                            preparedStatement.setObject(4, treeBeans.get(i).getUpdateTime());
                            preparedStatement.setObject(5, treeBeans.get(i).getDataSource());
                            preparedStatement.setObject(6, treeBeans.get(i).getCode());
                            preparedStatement.setObject(7, tenantId);
                            preparedStatement.setObject(8, treeBeans.get(i).getUpdateTime());
                        }

                        @Override
                        public int getBatchSize() {
                            return treeBeans.size();
                        }
                    });
                }
                if (!CollectionUtils.isEmpty(valueInsert)) {
                    String valueStr = "INSERT INTO dynamic_value (`id`, `attr_id`, `entity_id`, `value`, `tenant_id`) VALUES (?, ?, ?, ?, ?)";
                    jdbcSSOAPI.batchUpdate(valueStr, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, valueInsert.get(i).getId());
                            preparedStatement.setObject(2, valueInsert.get(i).getAttrId());
                            preparedStatement.setObject(3, valueInsert.get(i).getEntityId());
                            preparedStatement.setObject(4, valueInsert.get(i).getValue());
                            preparedStatement.setObject(5, tenantId);
                        }

                        @Override
                        public int getBatchSize() {
                            return valueInsert.size();
                        }
                    });
                }

                if (!CollectionUtils.isEmpty(valueUpdate)) {
                    String valueStr = "update dynamic_value set `value`=? where id= ?";
                    jdbcSSOAPI.batchUpdate(valueStr, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, valueUpdate.get(i).getValue());
                            preparedStatement.setObject(2, valueUpdate.get(i).getId());
                        }

                        @Override
                        public int getBatchSize() {
                            return valueUpdate.size();
                        }
                    });
                }
                return 1;
            } catch (Exception e) {
                transactionStatus.setRollbackOnly();
                // transactionStatus.rollbackToSavepoint(savepoint);
                e.printStackTrace();
                throw new CustomException(ResultCode.FAILED, "同步终止，部门同步异常！");

            }

        });

    }


    @Override
    public Integer renewDataTest(ArrayList<TreeBean> keepList, ArrayList<TreeBean> insertList, ArrayList<TreeBean> updateList, ArrayList<TreeBean> deleteList, ArrayList<TreeBean> invalidList, List<DynamicValue> valueUpdate,
                                 List<DynamicValue> valueInsert, List<DynamicAttr> attrList, String tenantId) {
        // 删除租户下所有数据
        String deleteAll = "delete from dept where tenant_id = ?";
        jdbcIGA.update(deleteAll, tenantId);

        String deptSql = "insert into dept (id,dept_code, dept_name,dept_en_name, parent_code, del_mark ,tenant_id ,source, data_source, description," +
                "create_time,tags,active,active_time,tree_type,dept_index,abbreviation,update_time,type,relation_type,independent,create_data_source,create_source,sync_state) values" +
                "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        return igaTemplate.execute(transactionStatus -> {
            try {
                if (null != keepList && keepList.size() > 0) {
                    jdbcIGA.batchUpdate(deptSql, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, keepList.get(i).getId());
                            preparedStatement.setObject(2, keepList.get(i).getCode());
                            preparedStatement.setObject(3, keepList.get(i).getName());
                            preparedStatement.setObject(4, keepList.get(i).getEnName());
                            preparedStatement.setObject(5, keepList.get(i).getParentCode());
                            preparedStatement.setObject(6, keepList.get(i).getDelMark());
                            preparedStatement.setObject(7, tenantId);
                            preparedStatement.setObject(8, keepList.get(i).getSource());
                            preparedStatement.setObject(9, keepList.get(i).getDataSource());
                            preparedStatement.setObject(10, keepList.get(i).getDescription());
                            preparedStatement.setObject(11, keepList.get(i).getCreateTime());
                            preparedStatement.setObject(12, keepList.get(i).getTags());
                            preparedStatement.setObject(13, keepList.get(i).getActive());
                            preparedStatement.setObject(14, LocalDateTime.now());
                            preparedStatement.setObject(15, keepList.get(i).getTreeType());
                            preparedStatement.setObject(16, keepList.get(i).getIndex());
                            preparedStatement.setObject(17, keepList.get(i).getAbbreviation());
                            preparedStatement.setObject(18, keepList.get(i).getUpdateTime());
                            preparedStatement.setObject(19, keepList.get(i).getType());
                            preparedStatement.setObject(20, keepList.get(i).getRelationType());
                            preparedStatement.setObject(21, null == keepList.get(i).getIndependent() ? 0 : keepList.get(i).getIndependent());
                            preparedStatement.setObject(22, keepList.get(i).getDataSource());
                            preparedStatement.setObject(23, keepList.get(i).getSource());
                            preparedStatement.setObject(24, 0);
                        }

                        @Override
                        public int getBatchSize() {
                            return keepList.size();
                        }
                    });
                }


                if (null != insertList && insertList.size() > 0) {
                    jdbcIGA.batchUpdate(deptSql, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, insertList.get(i).getId());
                            preparedStatement.setObject(2, insertList.get(i).getCode());
                            preparedStatement.setObject(3, insertList.get(i).getName());
                            preparedStatement.setObject(4, insertList.get(i).getEnName());
                            preparedStatement.setObject(5, insertList.get(i).getParentCode());
                            preparedStatement.setObject(6, insertList.get(i).getDelMark());
                            preparedStatement.setObject(7, tenantId);
                            preparedStatement.setObject(8, insertList.get(i).getSource());
                            preparedStatement.setObject(9, insertList.get(i).getDataSource());
                            preparedStatement.setObject(10, insertList.get(i).getDescription());
                            preparedStatement.setObject(11, insertList.get(i).getCreateTime());
                            preparedStatement.setObject(12, insertList.get(i).getTags());
                            preparedStatement.setObject(13, insertList.get(i).getActive());
                            preparedStatement.setObject(14, LocalDateTime.now());
                            preparedStatement.setObject(15, insertList.get(i).getTreeType());
                            preparedStatement.setObject(16, insertList.get(i).getIndex());
                            preparedStatement.setObject(17, insertList.get(i).getAbbreviation());
                            preparedStatement.setObject(18, insertList.get(i).getUpdateTime());
                            preparedStatement.setObject(19, insertList.get(i).getType());
                            preparedStatement.setObject(20, insertList.get(i).getRelationType());
                            preparedStatement.setObject(21, null == insertList.get(i).getIndependent() ? 0 : insertList.get(i).getIndependent());
                            preparedStatement.setObject(22, insertList.get(i).getDataSource());
                            preparedStatement.setObject(23, insertList.get(i).getSource());
                            preparedStatement.setObject(24, 1);
                        }

                        @Override
                        public int getBatchSize() {
                            return insertList.size();
                        }
                    });
                }
                if (null != updateList && updateList.size() > 0) {
                    int[] i = jdbcIGA.batchUpdate(deptSql, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, updateList.get(i).getId());
                            preparedStatement.setObject(2, updateList.get(i).getCode());
                            preparedStatement.setObject(3, updateList.get(i).getName());
                            preparedStatement.setObject(4, updateList.get(i).getEnName());
                            preparedStatement.setObject(5, updateList.get(i).getParentCode());
                            preparedStatement.setObject(6, updateList.get(i).getDelMark());
                            preparedStatement.setObject(7, tenantId);
                            preparedStatement.setObject(8, updateList.get(i).getSource());
                            preparedStatement.setObject(9, updateList.get(i).getDataSource());
                            preparedStatement.setObject(10, updateList.get(i).getDescription());
                            preparedStatement.setObject(11, updateList.get(i).getCreateTime());
                            preparedStatement.setObject(12, updateList.get(i).getTags());
                            preparedStatement.setObject(13, updateList.get(i).getActive());
                            preparedStatement.setObject(14, updateList.get(i).getActiveTime());
                            preparedStatement.setObject(15, updateList.get(i).getTreeType());
                            preparedStatement.setObject(16, updateList.get(i).getIndex());
                            preparedStatement.setObject(17, updateList.get(i).getAbbreviation());
                            preparedStatement.setObject(18, LocalDateTime.now());
                            preparedStatement.setObject(19, updateList.get(i).getType());
                            preparedStatement.setObject(20, updateList.get(i).getRelationType());
                            preparedStatement.setObject(21, null == updateList.get(i).getIndependent() ? 0 : updateList.get(i).getIndependent());
                            preparedStatement.setObject(22, updateList.get(i).getDataSource());
                            preparedStatement.setObject(23, updateList.get(i).getSource());
                            preparedStatement.setObject(24, 3);

                        }

                        @Override
                        public int getBatchSize() {
                            return updateList.size();
                        }
                    });
                }
                if (null != deleteList && deleteList.size() > 0) {
                    jdbcIGA.batchUpdate(deptSql, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, deleteList.get(i).getId());
                            preparedStatement.setObject(2, deleteList.get(i).getCode());
                            preparedStatement.setObject(3, deleteList.get(i).getName());
                            preparedStatement.setObject(4, deleteList.get(i).getEnName());
                            preparedStatement.setObject(5, deleteList.get(i).getParentCode());
                            preparedStatement.setObject(6, deleteList.get(i).getDelMark());
                            preparedStatement.setObject(7, tenantId);
                            preparedStatement.setObject(8, deleteList.get(i).getSource());
                            preparedStatement.setObject(9, deleteList.get(i).getDataSource());
                            preparedStatement.setObject(10, deleteList.get(i).getDescription());
                            preparedStatement.setObject(11, deleteList.get(i).getCreateTime());
                            preparedStatement.setObject(12, deleteList.get(i).getTags());
                            preparedStatement.setObject(13, deleteList.get(i).getActive());
                            preparedStatement.setObject(14, deleteList.get(i).getActiveTime());
                            preparedStatement.setObject(15, deleteList.get(i).getTreeType());
                            preparedStatement.setObject(16, deleteList.get(i).getIndex());
                            preparedStatement.setObject(17, deleteList.get(i).getAbbreviation());
                            preparedStatement.setObject(18, LocalDateTime.now());
                            preparedStatement.setObject(19, deleteList.get(i).getType());
                            preparedStatement.setObject(20, deleteList.get(i).getRelationType());
                            preparedStatement.setObject(21, null == deleteList.get(i).getIndependent() ? 0 : deleteList.get(i).getIndependent());
                            preparedStatement.setObject(22, deleteList.get(i).getDataSource());
                            preparedStatement.setObject(23, deleteList.get(i).getSource());
                            preparedStatement.setObject(24, 2);
                        }

                        @Override
                        public int getBatchSize() {
                            return deleteList.size();
                        }
                    });
                }

                if (null != invalidList && invalidList.size() > 0) {
                    jdbcIGA.batchUpdate(deptSql, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, invalidList.get(i).getId());
                            preparedStatement.setObject(2, invalidList.get(i).getCode());
                            preparedStatement.setObject(3, invalidList.get(i).getName());
                            preparedStatement.setObject(4, invalidList.get(i).getEnName());
                            preparedStatement.setObject(5, invalidList.get(i).getParentCode());
                            preparedStatement.setObject(6, invalidList.get(i).getDelMark());
                            preparedStatement.setObject(7, tenantId);
                            preparedStatement.setObject(8, invalidList.get(i).getSource());
                            preparedStatement.setObject(9, invalidList.get(i).getDataSource());
                            preparedStatement.setObject(10, invalidList.get(i).getDescription());
                            preparedStatement.setObject(11, invalidList.get(i).getCreateTime());
                            preparedStatement.setObject(12, invalidList.get(i).getTags());
                            preparedStatement.setObject(13, invalidList.get(i).getActive());
                            preparedStatement.setObject(14, invalidList.get(i).getActiveTime());
                            preparedStatement.setObject(15, invalidList.get(i).getTreeType());
                            preparedStatement.setObject(16, invalidList.get(i).getIndex());
                            preparedStatement.setObject(17, invalidList.get(i).getAbbreviation());
                            preparedStatement.setObject(18, LocalDateTime.now());
                            preparedStatement.setObject(19, invalidList.get(i).getType());
                            preparedStatement.setObject(20, invalidList.get(i).getRelationType());
                            preparedStatement.setObject(21, null == invalidList.get(i).getIndependent() ? 0 : invalidList.get(i).getIndependent());
                            preparedStatement.setObject(22, invalidList.get(i).getDataSource());
                            preparedStatement.setObject(23, invalidList.get(i).getSource());
                            preparedStatement.setObject(24, 4);
                        }

                        @Override
                        public int getBatchSize() {
                            return invalidList.size();
                        }
                    });
                }

                List<DynamicValue> dynamicValues = new ArrayList<>();
                if (!CollectionUtils.isEmpty(valueInsert)) {
                    dynamicValues.addAll(valueInsert);
                }
                if (!CollectionUtils.isEmpty(valueUpdate)) {
                    dynamicValues.addAll(valueUpdate);
                }

                if (!CollectionUtils.isEmpty(dynamicValues)) {
                    // 删除、并重新创建扩展字段
                    String deleteDynamicAttrSql = "delete from dynamic_attr where   type='DEPT' and tenant_id = ?";
                    jdbcIGA.update(deleteDynamicAttrSql, new Object[]{new String(tenantId)});

                    String deleteDynamicValueSql = "delete from dynamic_value where  tenant_id = ? and attr_id not in (select id from dynamic_attr )";
                    jdbcIGA.update(deleteDynamicValueSql, new Object[]{new String(tenantId)});

                    String addDynamicValueSql = "INSERT INTO dynamic_attr (id, name, code, required, description, tenant_id, create_time, update_time, type, field_type, format, is_search, attr_index) VALUES (?,?,?,?,?,?,?,?,'DEPT',?,?,?,?)";
                    jdbcIGA.batchUpdate(addDynamicValueSql, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            DynamicAttr attr = attrList.get(i);
                            preparedStatement.setObject(1, attr.getId());
                            preparedStatement.setObject(2, attr.getName());
                            preparedStatement.setObject(3, attr.getCode());
                            preparedStatement.setObject(4, attr.getRequired());
                            preparedStatement.setObject(5, attr.getDescription());
                            preparedStatement.setObject(6, tenantId);
                            preparedStatement.setObject(7, attr.getCreateTime());
                            preparedStatement.setObject(8, attr.getUpdateTime());
                            preparedStatement.setObject(9, attr.getFieldType());
                            preparedStatement.setObject(10, attr.getFormat());
                            preparedStatement.setObject(11, attr.getIsSearch());
                            preparedStatement.setObject(12, attr.getAttrIndex());
                        }
                        @Override
                        public int getBatchSize() {
                            return dynamicValues.size();
                        }
                    });


                    String valueStr = "INSERT INTO dynamic_value (`id`, `attr_id`, `entity_id`, `value`, `tenant_id`) VALUES (?, ?, ?, ?, ?)";
                    jdbcIGA.batchUpdate(valueStr, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, dynamicValues.get(i).getId());
                            preparedStatement.setObject(2, dynamicValues.get(i).getAttrId());
                            preparedStatement.setObject(3, dynamicValues.get(i).getEntityId());
                            preparedStatement.setObject(4, dynamicValues.get(i).getValue());
                            preparedStatement.setObject(5, tenantId);
                        }

                        @Override
                        public int getBatchSize() {
                            return dynamicValues.size();
                        }
                    });
                }

                return 1;
            } catch (Exception e) {
                transactionStatus.setRollbackOnly();
                // transactionStatus.rollbackToSavepoint(savepoint);
                e.printStackTrace();
                throw new CustomException(ResultCode.FAILED, "同步终止，部门同步异常！");

            }

        });

    }


    @Override
    public List<TreeBean> findBySourceAndTreeType(String api, String treeType, String tenantId) {
        String sql = "select dept_code as code , dept_name as name ,independent,dept_en_name as enName , parent_code as parentCode ,relation_type as relationType ," +
                " create_time as createTime , source, tree_type as treeType,data_source as dataSource, abbreviation,tags,type,update_time as updateTime,del_mark as delMark,active,dept_index as 'index',active_time as activeTime,orphan from dept where tenant_id = ? and data_source=?  and del_mark=0 ";
        List<Object> param = new ArrayList<>();
        param.add(tenantId);
        param.add(api);

        if (null != treeType) {
            sql = sql + " and tree_type= ? ";
            param.add(treeType);
        }


        List<Map<String, Object>> mapList = jdbcSSOAPI.queryForList(sql, param.toArray());

        return getDeptBeans(mapList);
    }

    @Override
    public List<TreeBean> findActiveDataByTenantId(String tenantId) {
        String sql = "select dept_code as code , dept_name as name ,independent,dept_en_name as enName , parent_code as parentCode ,relation_type as relationType , " +
                " create_time as createTime , source, tree_type as treeType,data_source as dataSource, abbreviation,tags,type,update_time as updateTime,del_mark as delMark,active,dept_index as 'index',active_time as activeTime ,orphan from dept where tenant_id = ? " +
                " and active=true and del_mark=false ";
        List<Object> param = new ArrayList<>();
        param.add(tenantId);

        List<Map<String, Object>> mapList = jdbcSSOAPI.queryForList(sql, param.toArray());
        return getDeptBeans(mapList);
    }
}
