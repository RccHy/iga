package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.Dept;
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

import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;


@Repository
@Component
public class DeptDaoImpl implements DeptDao {


    @Resource(name = "jdbcSSOAPI")
    JdbcTemplate jdbcSSOAPI;

    @Resource(name = "api-txTemplate")
    TransactionTemplate txTemplate;

    @Override
    public List<Dept> getAllDepts() {
        String sql = "select id, code, name, type_id as typeId,create_time as createTime from dept";

        List<Map<String, Object>> mapList = jdbcSSOAPI.queryForList(sql);
        return getDepts(mapList);
    }

    @Override
    public Dept findById(String id) {
        String sql = "select id, code, name, type_id as typeId,create_time as createTime,abbreviation from dept where id= ? ";

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
        String sql = "select dept_code as code , dept_name as name , parent_code as parentCode , " +
                " update_time as createTime , source, tree_type as treeType,data_source as dataSource, abbreviation,tags,type,independent,update_time as updateTime,del_mark as delMark,active  from dept where tenant_id = ? ";
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
                list.add(dept);
            }
            return list;
        }

        return null;
    }

    @Override
    public ArrayList<TreeBean> updateDept(ArrayList<TreeBean> list, String tenantId) {
        String str = "update dept set  dept_name=?, parent_code=?, del_mark=? ,tenant_id =?" +
                ",source =?, data_source=?, description=?, meta=?,update_time=?,tags=?,independent=?,tree_type= ?,active=? ,abbreviation=?,del_mark=0 ,type = ? " +
                "where dept_code =? and update_time< ?";
        boolean contains = false;

        int[] ints = jdbcSSOAPI.batchUpdate(str, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                preparedStatement.setObject(1, list.get(i).getName());
                preparedStatement.setObject(2, list.get(i).getParentCode());
                preparedStatement.setObject(3, 0);
                preparedStatement.setObject(4, tenantId);
                preparedStatement.setObject(5, list.get(i).getSource());
                preparedStatement.setObject(6, "PULL");
                preparedStatement.setObject(7, list.get(i).getDescription());
                preparedStatement.setObject(8, list.get(i).getMeta());
                preparedStatement.setObject(9, list.get(i).getCreateTime() == null ? LocalDateTime.now() : list.get(i).getCreateTime());
                preparedStatement.setObject(10, list.get(i).getTags());
                preparedStatement.setObject(11, list.get(i).getIndependent());
                preparedStatement.setObject(12, list.get(i).getTreeType());
                preparedStatement.setObject(13, 0);
                preparedStatement.setObject(14, null == list.get(i).getAbbreviation() ? null : list.get(i).getAbbreviation());
                preparedStatement.setObject(15, null == list.get(i).getType() ? null : list.get(i).getType());
                preparedStatement.setObject(16, list.get(i).getCode());
                preparedStatement.setObject(17, list.get(i).getCreateTime() == null ? LocalDateTime.now() : list.get(i).getCreateTime());

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
        String str = "insert into dept (id,dept_code, dept_name, parent_code, del_mark ,tenant_id ,source, data_source, description, meta,create_time,tags,independent,active,active_time,tree_type,dept_index,abbreviation,update_time,type) values" +
                "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        boolean contains = false;

        int[] ints = jdbcSSOAPI.batchUpdate(str, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                preparedStatement.setObject(1, UUID.randomUUID().toString().replace("-", ""));
                preparedStatement.setObject(2, list.get(i).getCode());
                preparedStatement.setObject(3, list.get(i).getName());
                preparedStatement.setObject(4, list.get(i).getParentCode());
                preparedStatement.setObject(5, 0);
                preparedStatement.setObject(6, tenantId);
                preparedStatement.setObject(7, list.get(i).getSource());
                preparedStatement.setObject(8, "PULL");
                preparedStatement.setObject(9, list.get(i).getDescription());
                preparedStatement.setObject(10, list.get(i).getMeta());
                preparedStatement.setObject(11, list.get(i).getCreateTime() == null ? LocalDateTime.now() : list.get(i).getCreateTime());
                preparedStatement.setObject(12, list.get(i).getTags());
                preparedStatement.setObject(13, list.get(i).getIndependent());
                preparedStatement.setObject(14, 0);
                preparedStatement.setObject(15, LocalDateTime.now());
                preparedStatement.setObject(16, list.get(i).getTreeType());
                preparedStatement.setObject(17, null == list.get(i).getDeptIndex() ? null : list.get(i).getDeptIndex());
                preparedStatement.setObject(18, null == list.get(i).getAbbreviation() ? null : list.get(i).getAbbreviation());
                preparedStatement.setObject(19, LocalDateTime.now());
                preparedStatement.setObject(20, list.get(i).getType());
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
    public Integer renewData(ArrayList<TreeBean> insertList, ArrayList<TreeBean> updateList, ArrayList<TreeBean> deleteList, ArrayList<TreeBean> invalidList, String tenantId) {
        String insertStr = "insert into dept (id,dept_code, dept_name, parent_code, del_mark ,tenant_id ,source, data_source, description, meta,create_time,tags,independent,active,active_time,tree_type,dept_index,abbreviation,update_time,type) values" +
                "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        String updateStr = "update dept set  dept_name=?, parent_code=?, del_mark=? ,tenant_id =?" +
                ",source =?, data_source=?, description=?, meta=?,update_time=?,tags=?,independent=?,tree_type= ?,active=? ,abbreviation=?,type = ?,dept_index=?  " +
                "where dept_code =? and update_time<= ?";
        String deleteStr = "update dept set   active = ?,active_time= ?,del_mark=?   " +
                "where dept_code =? and update_time<= ? ";
        return txTemplate.execute(transactionStatus -> {

            try {
                if (null != insertList && insertList.size() > 0) {
                    jdbcSSOAPI.batchUpdate(insertStr, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, UUID.randomUUID().toString().replace("-", ""));
                            preparedStatement.setObject(2, insertList.get(i).getCode());
                            preparedStatement.setObject(3, insertList.get(i).getName());
                            preparedStatement.setObject(4, insertList.get(i).getParentCode());
                            preparedStatement.setObject(5, insertList.get(i).getDelMark());
                            preparedStatement.setObject(6, tenantId);
                            preparedStatement.setObject(7, insertList.get(i).getSource());
                            preparedStatement.setObject(8, "PULL");
                            preparedStatement.setObject(9, insertList.get(i).getDescription());
                            preparedStatement.setObject(10, insertList.get(i).getMeta());
                            preparedStatement.setObject(11, insertList.get(i).getCreateTime());
                            preparedStatement.setObject(12, insertList.get(i).getTags());
                            preparedStatement.setObject(13, insertList.get(i).getIndependent());
                            preparedStatement.setObject(14, insertList.get(i).getActive());
                            preparedStatement.setObject(15, LocalDateTime.now());
                            preparedStatement.setObject(16, insertList.get(i).getTreeType());
                            preparedStatement.setObject(17, insertList.get(i).getDeptIndex());
                            preparedStatement.setObject(18, insertList.get(i).getAbbreviation());
                            preparedStatement.setObject(19, insertList.get(i).getUpdateTime());
                            preparedStatement.setObject(20, insertList.get(i).getType());
                        }

                        @Override
                        public int getBatchSize() {
                            return insertList.size();
                        }
                    });
                }
                if (null != updateList && updateList.size() > 0) {
                    int[] i = jdbcSSOAPI.batchUpdate(updateStr, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, updateList.get(i).getName());
                            preparedStatement.setObject(2, updateList.get(i).getParentCode());
                            preparedStatement.setObject(3, null == updateList.get(i).getDelMark() ? 0 : updateList.get(i).getDelMark());
                            preparedStatement.setObject(4, tenantId);
                            preparedStatement.setObject(5, updateList.get(i).getSource());
                            preparedStatement.setObject(6, "PULL");
                            preparedStatement.setObject(7, updateList.get(i).getDescription());
                            preparedStatement.setObject(8, updateList.get(i).getMeta());
                            preparedStatement.setObject(9, updateList.get(i).getUpdateTime());
                            preparedStatement.setObject(10, updateList.get(i).getTags());
                            preparedStatement.setObject(11, updateList.get(i).getIndependent());
                            preparedStatement.setObject(12, updateList.get(i).getTreeType());
                            preparedStatement.setObject(13, updateList.get(i).getActive());
                            preparedStatement.setObject(14, updateList.get(i).getAbbreviation());
                            preparedStatement.setObject(15, updateList.get(i).getType());
                            preparedStatement.setObject(16, updateList.get(i).getDeptIndex());
                            preparedStatement.setObject(17, updateList.get(i).getCode());
                            preparedStatement.setObject(18, updateList.get(i).getUpdateTime());

                        }

                        @Override
                        public int getBatchSize() {
                            return updateList.size();
                        }
                    });
                }
                if (null != deleteList && deleteList.size() > 0) {
                    jdbcSSOAPI.batchUpdate(deleteStr, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, deleteList.get(i).getActive());
                            preparedStatement.setObject(2, LocalDateTime.now());
                            preparedStatement.setObject(3, deleteList.get(i).getDelMark());
                            preparedStatement.setObject(4, deleteList.get(i).getCode());
                            preparedStatement.setObject(5, deleteList.get(i).getUpdateTime());
                        }

                        @Override
                        public int getBatchSize() {
                            return deleteList.size();
                        }
                    });
                }
                if (null != invalidList && invalidList.size() > 0) {
                    jdbcSSOAPI.batchUpdate(deleteStr, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, invalidList.get(i).getActive());
                            preparedStatement.setObject(2, LocalDateTime.now());
                            preparedStatement.setObject(3, invalidList.get(i).getDelMark());
                            preparedStatement.setObject(4, invalidList.get(i).getCode());
                            preparedStatement.setObject(5, invalidList.get(i).getUpdateTime());
                        }

                        @Override
                        public int getBatchSize() {
                            return invalidList.size();
                        }
                    });
                }
                return 1;
            } catch (Exception e) {
                transactionStatus.setRollbackOnly();
                // transactionStatus.rollbackToSavepoint(savepoint);
                throw new CustomException(ResultCode.FAILED, "同步终止，部门同步异常！");

            }

        });

    }

    @Override
    public List<TreeBean> findBySourceAndTreeType(String api, String treeType, String tenantId) {
        String sql = "select dept_code as code , dept_name as name , parent_code as parentCode , " +
                " update_time as createTime , source, tree_type as treeType,data_source as dataSource, abbreviation,tags,type,independent,update_time as updateTime,del_mark as delMark,active  from dept where tenant_id = ? and data_source=?  and del_mark=0 ";
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
        String sql = "select dept_code as code , dept_name as name , parent_code as parentCode , " +
                " update_time as createTime , source, tree_type as treeType,data_source as dataSource, abbreviation,tags,type,independent,update_time as updateTime,del_mark as delMark,active  from dept where tenant_id = ? " +
                " and active=true and del_mark=false ";
        List<Object> param = new ArrayList<>();
        param.add(tenantId);

        List<Map<String, Object>> mapList = jdbcSSOAPI.queryForList(sql, param.toArray());
        return getDeptBeans(mapList);
    }
}
