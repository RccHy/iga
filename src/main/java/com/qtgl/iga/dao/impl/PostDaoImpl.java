package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.DynamicAttr;
import com.qtgl.iga.bo.DynamicValue;
import com.qtgl.iga.dao.PostDao;
import com.qtgl.iga.utils.MyBeanUtils;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import org.apache.commons.lang3.StringUtils;
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
public class PostDaoImpl implements PostDao {


    @Resource(name = "jdbcSSO")
    JdbcTemplate jdbcSSO;

    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;


    @Resource(name = "sso-txTemplate")
    TransactionTemplate txTemplate2;


    @Resource(name = "iga-txTemplate")
    TransactionTemplate igaTemplate;


    @Override
    public List<TreeBean> findByTenantId(String id) {
        //
        String sql = "select id, user_type as code , name, parent_code as parentCode , " +
                " create_time as createTime," +
                "source,data_source as dataSource,user_type_index as deptIndex,del_mark as delMark,update_time as updateTime,post_type as type,active,tags,user_type_index as 'index',active_time as activeTime,formal,orphan from user_type where tenant_id = ? and client_id is null or client_id = ''  ";

        List<Map<String, Object>> mapList = jdbcSSO.queryForList(sql, id);
        return getUserTypes(mapList);
    }

    private List<TreeBean> getUserTypes(List<Map<String, Object>> mapList) {
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

        String str = "update user_type set  name=?, parent_code=?, del_mark=? ,tenant_id =?" +
                ", data_source=?, description=?,update_time=?,tags=?,source=?" +
                ", user_type_index = ?,del_mark =0,active_time=?  where user_type =?";
        boolean contains = false;

        int[] ints = jdbcSSO.batchUpdate(str, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                preparedStatement.setObject(1, list.get(i).getName());
                preparedStatement.setObject(2, list.get(i).getParentCode());
                preparedStatement.setObject(3, 0);
                preparedStatement.setObject(4, tenantId);
                preparedStatement.setObject(5, "PULL");
                preparedStatement.setObject(6, list.get(i).getDescription());
                preparedStatement.setObject(7, list.get(i).getUpdateTime() == null ? LocalDateTime.now() : list.get(i).getUpdateTime());
                preparedStatement.setObject(8, list.get(i).getTags());
                preparedStatement.setObject(9, list.get(i).getSource());
                preparedStatement.setObject(10, null == list.get(i).getIndex() ? null : list.get(i).getIndex());
                preparedStatement.setObject(11, list.get(i).getActiveTime());
                preparedStatement.setObject(12, list.get(i).getCode());

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
        String str = "insert into user_type (id,user_type, name, parent_code, can_login ,tenant_id ,tags, data_source, description,create_time,del_mark,active,active_time,update_time,source,user_type_index) values" +
                "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        boolean contains = false;

        int[] ints = jdbcSSO.batchUpdate(str, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                preparedStatement.setObject(1, UUID.randomUUID().toString().replace("-", ""));
                preparedStatement.setObject(2, list.get(i).getCode());
                preparedStatement.setObject(3, list.get(i).getName());
                preparedStatement.setObject(4, list.get(i).getParentCode());
                preparedStatement.setObject(5, 0);
                preparedStatement.setObject(6, tenantId);
                preparedStatement.setObject(7, list.get(i).getTags());
                preparedStatement.setObject(8, "PULL");
                preparedStatement.setObject(9, list.get(i).getDescription());
                preparedStatement.setObject(10, list.get(i).getCreateTime() == null ? LocalDateTime.now() : list.get(i).getCreateTime());
                preparedStatement.setObject(11, 0);
                preparedStatement.setObject(12, 0);
                preparedStatement.setObject(13, LocalDateTime.now());
                preparedStatement.setObject(14, LocalDateTime.now());
                preparedStatement.setObject(15, list.get(i).getSource());
                preparedStatement.setObject(16, list.get(i).getIndex());
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
        String str = "update user_type set   del_mark= ? , active = ?,active_time= ?  " +
                "where user_type =?";
        boolean contains = false;

        int[] ints = jdbcSSO.batchUpdate(str, new BatchPreparedStatementSetter() {
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
    public List<TreeBean> findRootData(String tenantId) {
        String sql = "select  user_type as code , name as name , parent_code as parentCode ,create_time as createTime, " +
                " tags ,data_source as dataSource , description ,source,post_type as postType,user_type_index as deptIndex,del_mark as delMark ,active,user_type_index as 'index',active_time as activeTime,formal,orphan  " +
                " from user_type where tenant_id=? and del_mark=0  and data_source!=? and client_id is null or client_id = '' ";

        //String sql = "select  user_type as code , name as name , parent_code as parentCode ,create_time as createTime, " +
        //        " tags ,data_source as dataSource , description ,source,post_type as postType,user_type_index as deptIndex,del_mark as delMark ,active " +
        //        " from user_type where tenant_id=? and del_mark=0 and active=1 and client_id is null or client_id = '' ";

        List<Map<String, Object>> mapList = jdbcSSO.queryForList(sql, tenantId, "PULL");
        ArrayList<TreeBean> list = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {
                TreeBean treeBean = new TreeBean();
                try {
                    MyBeanUtils.populate(treeBean, map);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
                if (StringUtils.isBlank(treeBean.getParentCode())) {
                    treeBean.setParentCode("");
                }
                list.add(treeBean);
            }
            return list;
        }

        return null;
    }

    @Override
    public List<TreeBean> findPostType(String id) {
        String sql = "select  user_type as code , name as name , parent_code as parentCode , " +
                " tags ,data_source as dataSource , description ,source,post_type as postType,user_type_index as deptIndex,active,user_type_index as 'index',active_time as activeTime,formal,orphan  " +
                " from user_type where tenant_id=? and del_mark=0";

        List<Map<String, Object>> mapList = jdbcSSO.queryForList(sql, id);
        ArrayList<TreeBean> list = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {
                TreeBean treeBean = new TreeBean();
                try {
                    MyBeanUtils.populate(treeBean, map);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
                if (StringUtils.isBlank(treeBean.getParentCode())) {
                    treeBean.setParentCode("");
                }
                list.add(treeBean);
            }
            return list;
        }

        return null;
    }


    public Integer renewData(ArrayList<TreeBean> insertList, ArrayList<TreeBean> updateList, ArrayList<TreeBean> deleteList, ArrayList<TreeBean> invalidList, List<DynamicValue> valueUpdate, List<DynamicValue> valueInsert, String tenantId) {
        String insertStr = "insert into user_type (id,user_type, name, parent_code, can_login ,tenant_id ,tags," +
                " data_source, description,create_time,del_mark,active,active_time,update_time,source,user_type_index,post_type,formal,create_data_source,create_source) values" +
                "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        return txTemplate2.execute(transactionStatus -> {
            try {
                if (null != insertList && insertList.size() > 0) {
                    jdbcSSO.batchUpdate(insertStr, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, insertList.get(i).getId());
                            preparedStatement.setObject(2, insertList.get(i).getCode());
                            preparedStatement.setObject(3, insertList.get(i).getName());
                            preparedStatement.setObject(4, insertList.get(i).getParentCode());
                            // 默认是否能登陆 默认为true
                            preparedStatement.setObject(5, 1);
                            preparedStatement.setObject(6, tenantId);
                            preparedStatement.setObject(7, insertList.get(i).getTags());
                            preparedStatement.setObject(8, insertList.get(i).getDataSource());
                            preparedStatement.setObject(9, insertList.get(i).getDescription());
                            preparedStatement.setObject(10, insertList.get(i).getCreateTime());
                            preparedStatement.setObject(11, insertList.get(i).getDelMark());
                            preparedStatement.setObject(12, insertList.get(i).getActive());
                            preparedStatement.setObject(13, LocalDateTime.now());
                            preparedStatement.setObject(14, insertList.get(i).getUpdateTime());
                            preparedStatement.setObject(15, insertList.get(i).getSource());
                            preparedStatement.setObject(16, insertList.get(i).getIndex());
                            preparedStatement.setObject(17, insertList.get(i).getType());
                            preparedStatement.setObject(18, insertList.get(i).getFormal());
                            preparedStatement.setObject(19, insertList.get(i).getDataSource());
                            preparedStatement.setObject(20, insertList.get(i).getSource());
                        }

                        @Override
                        public int getBatchSize() {
                            return insertList.size();
                        }
                    });
                }
                String updateStr = "update user_type set  name=?, parent_code=?, del_mark=? ,tenant_id =?" +
                        ", data_source=?, description=?,update_time=?,tags=?,source=?" +
                        ", user_type_index = ?,post_type=?,active=?,formal=? ,active_time=? where user_type =? and tenant_id=? and update_time<= ?";
                if (null != updateList && updateList.size() > 0) {
                    jdbcSSO.batchUpdate(updateStr, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, updateList.get(i).getName());
                            preparedStatement.setObject(2, updateList.get(i).getParentCode());
                            preparedStatement.setObject(3, null == updateList.get(i).getDelMark() ? 0 : updateList.get(i).getDelMark());
                            preparedStatement.setObject(4, tenantId);
                            preparedStatement.setObject(5, updateList.get(i).getDataSource());
                            preparedStatement.setObject(6, updateList.get(i).getDescription());
                            preparedStatement.setObject(7, updateList.get(i).getUpdateTime());
                            preparedStatement.setObject(8, updateList.get(i).getTags());
                            preparedStatement.setObject(9, updateList.get(i).getSource());
                            preparedStatement.setObject(10, updateList.get(i).getIndex());
                            preparedStatement.setObject(11, updateList.get(i).getType());
                            preparedStatement.setObject(12, updateList.get(i).getActive());
                            preparedStatement.setObject(13, updateList.get(i).getFormal());
                            preparedStatement.setObject(14, updateList.get(i).getActiveTime());
                            preparedStatement.setObject(15, updateList.get(i).getCode());
                            preparedStatement.setObject(16, tenantId);
                            preparedStatement.setObject(17, updateList.get(i).getUpdateTime());

                        }

                        @Override
                        public int getBatchSize() {
                            return updateList.size();
                        }
                    });
                }
                String deleteStr = "update user_type set   del_mark= ? , active = ?,active_time= ? ,update_time=? , data_source=?,active_time=? " +
                        "where user_type =? and tenant_id=? and update_time<= ? ";

                ArrayList<TreeBean> treeBeans = new ArrayList<>();
                if (null != deleteList && deleteList.size() > 0) {
                    treeBeans.addAll(deleteList);
                }
                if (null != invalidList && invalidList.size() > 0) {
                    treeBeans.addAll(invalidList);
                }
                if (null != treeBeans && treeBeans.size() > 0) {
                    jdbcSSO.batchUpdate(deleteStr, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, treeBeans.get(i).getDelMark());
                            preparedStatement.setObject(2, treeBeans.get(i).getActive());
                            preparedStatement.setObject(3, LocalDateTime.now());
                            preparedStatement.setObject(4, treeBeans.get(i).getUpdateTime());
                            preparedStatement.setObject(5, treeBeans.get(i).getDataSource());
                            preparedStatement.setObject(6, treeBeans.get(i).getActiveTime());
                            preparedStatement.setObject(7, treeBeans.get(i).getCode());
                            preparedStatement.setObject(8, tenantId);
                            preparedStatement.setObject(9, treeBeans.get(i).getUpdateTime());
                        }

                        @Override
                        public int getBatchSize() {
                            return treeBeans.size();
                        }
                    });
                }

                if (!CollectionUtils.isEmpty(valueInsert)) {
                    String valueStr = "INSERT INTO dynamic_value (`id`, `attr_id`, `entity_id`, `value`, `tenant_id`) VALUES (?, ?, ?, ?, ?)";
                    jdbcSSO.batchUpdate(valueStr, new BatchPreparedStatementSetter() {
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
                    jdbcSSO.batchUpdate(valueStr, new BatchPreparedStatementSetter() {
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
                throw new CustomException(ResultCode.FAILED, "同步终止，岗位同步异常！");
            }
        });
    }


    @Override
    public Integer renewDataTest(ArrayList<TreeBean> keepList, ArrayList<TreeBean> insertList, ArrayList<TreeBean> updateList, ArrayList<TreeBean> deleteList, ArrayList<TreeBean> invalidList,
                                 List<DynamicValue> valueUpdate, List<DynamicValue> valueInsert, List<DynamicAttr> attrList, String tenantId) {
        // 先删除租户下所有数据
        String deleteAll = "delete from user_type where tenant_id = ?";
        jdbcIGA.update(deleteAll, new Object[]{new String(tenantId)});

        String postSql = "insert into user_type (id,user_type, name, parent_code ,tenant_id ,tags," +
                " data_source, description,create_time,del_mark,active,active_time,update_time,source,user_type_index,post_type,formal,create_data_source,create_source,sync_state) values" +
                "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        return igaTemplate.execute(transactionStatus -> {
            try {
                if (null != keepList && keepList.size() > 0) {
                    jdbcIGA.batchUpdate(postSql, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, keepList.get(i).getId());
                            preparedStatement.setObject(2, keepList.get(i).getCode());
                            preparedStatement.setObject(3, keepList.get(i).getName());
                            preparedStatement.setObject(4, keepList.get(i).getParentCode());
                            preparedStatement.setObject(5, tenantId);
                            preparedStatement.setObject(6, keepList.get(i).getTags());
                            preparedStatement.setObject(7, keepList.get(i).getDataSource());
                            preparedStatement.setObject(8, keepList.get(i).getDescription());
                            preparedStatement.setObject(9, keepList.get(i).getCreateTime());
                            preparedStatement.setObject(10, keepList.get(i).getDelMark());
                            preparedStatement.setObject(11, keepList.get(i).getActive());
                            preparedStatement.setObject(12, keepList.get(i).getActiveTime());
                            preparedStatement.setObject(13, keepList.get(i).getUpdateTime());
                            preparedStatement.setObject(14, keepList.get(i).getSource());
                            preparedStatement.setObject(15, keepList.get(i).getIndex());
                            preparedStatement.setObject(16, keepList.get(i).getType());
                            preparedStatement.setObject(17, keepList.get(i).getFormal());
                            preparedStatement.setObject(18, keepList.get(i).getDataSource());
                            preparedStatement.setObject(19, keepList.get(i).getSource());
                            preparedStatement.setObject(20, 0);
                        }

                        @Override
                        public int getBatchSize() {
                            return insertList.size();
                        }
                    });
                }
                if (null != insertList && insertList.size() > 0) {
                    jdbcIGA.batchUpdate(postSql, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, insertList.get(i).getId());
                            preparedStatement.setObject(2, insertList.get(i).getCode());
                            preparedStatement.setObject(3, insertList.get(i).getName());
                            preparedStatement.setObject(4, insertList.get(i).getParentCode());
                            preparedStatement.setObject(5, tenantId);
                            preparedStatement.setObject(6, insertList.get(i).getTags());
                            preparedStatement.setObject(7, insertList.get(i).getDataSource());
                            preparedStatement.setObject(8, insertList.get(i).getDescription());
                            preparedStatement.setObject(9, insertList.get(i).getCreateTime());
                            preparedStatement.setObject(10, insertList.get(i).getDelMark());
                            preparedStatement.setObject(11, insertList.get(i).getActive());
                            preparedStatement.setObject(12, LocalDateTime.now());
                            preparedStatement.setObject(13, insertList.get(i).getUpdateTime());
                            preparedStatement.setObject(14, insertList.get(i).getSource());
                            preparedStatement.setObject(15, insertList.get(i).getIndex());
                            preparedStatement.setObject(16, insertList.get(i).getType());
                            preparedStatement.setObject(17, insertList.get(i).getFormal());
                            preparedStatement.setObject(18, insertList.get(i).getDataSource());
                            preparedStatement.setObject(19, insertList.get(i).getSource());
                            preparedStatement.setObject(20, 1);
                        }

                        @Override
                        public int getBatchSize() {
                            return insertList.size();
                        }
                    });
                }

                if (null != updateList && updateList.size() > 0) {
                    jdbcIGA.batchUpdate(postSql, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, updateList.get(i).getId());
                            preparedStatement.setObject(2, updateList.get(i).getCode());
                            preparedStatement.setObject(3, updateList.get(i).getName());
                            preparedStatement.setObject(4, updateList.get(i).getParentCode());
                            preparedStatement.setObject(5, tenantId);
                            preparedStatement.setObject(6, updateList.get(i).getTags());
                            preparedStatement.setObject(7, updateList.get(i).getDataSource());
                            preparedStatement.setObject(8, updateList.get(i).getDescription());
                            preparedStatement.setObject(9, updateList.get(i).getCreateTime());
                            preparedStatement.setObject(10, updateList.get(i).getDelMark());
                            preparedStatement.setObject(11, updateList.get(i).getActive());
                            preparedStatement.setObject(12, updateList.get(i).getActiveTime());
                            preparedStatement.setObject(13, LocalDateTime.now());
                            preparedStatement.setObject(14, updateList.get(i).getSource());
                            preparedStatement.setObject(15, updateList.get(i).getIndex());
                            preparedStatement.setObject(16, updateList.get(i).getType());
                            preparedStatement.setObject(17, updateList.get(i).getFormal());
                            preparedStatement.setObject(18, updateList.get(i).getDataSource());
                            preparedStatement.setObject(19, updateList.get(i).getSource());
                            preparedStatement.setObject(20, 3);

                        }

                        @Override
                        public int getBatchSize() {
                            return updateList.size();
                        }
                    });
                }


                if (null != deleteList && deleteList.size() > 0) {
                    jdbcIGA.batchUpdate(postSql, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, deleteList.get(i).getId());
                            preparedStatement.setObject(2, deleteList.get(i).getCode());
                            preparedStatement.setObject(3, deleteList.get(i).getName());
                            preparedStatement.setObject(4, deleteList.get(i).getParentCode());
                            preparedStatement.setObject(5, tenantId);
                            preparedStatement.setObject(6, deleteList.get(i).getTags());
                            preparedStatement.setObject(7, deleteList.get(i).getDataSource());
                            preparedStatement.setObject(8, deleteList.get(i).getDescription());
                            preparedStatement.setObject(9, deleteList.get(i).getCreateTime());
                            preparedStatement.setObject(10, deleteList.get(i).getDelMark());
                            preparedStatement.setObject(11, deleteList.get(i).getActive());
                            preparedStatement.setObject(12, deleteList.get(i).getActiveTime());
                            preparedStatement.setObject(13, LocalDateTime.now());
                            preparedStatement.setObject(14, deleteList.get(i).getSource());
                            preparedStatement.setObject(15, deleteList.get(i).getIndex());
                            preparedStatement.setObject(16, deleteList.get(i).getType());
                            preparedStatement.setObject(17, deleteList.get(i).getFormal());
                            preparedStatement.setObject(18, deleteList.get(i).getDataSource());
                            preparedStatement.setObject(19, deleteList.get(i).getSource());
                            preparedStatement.setObject(20, 2);
                        }

                        @Override
                        public int getBatchSize() {
                            return deleteList.size();
                        }
                    });
                }


                if (null != invalidList && invalidList.size() > 0) {
                    jdbcIGA.batchUpdate(postSql, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, invalidList.get(i).getId());
                            preparedStatement.setObject(2, invalidList.get(i).getCode());
                            preparedStatement.setObject(3, invalidList.get(i).getName());
                            preparedStatement.setObject(4, invalidList.get(i).getParentCode());
                            preparedStatement.setObject(5, tenantId);
                            preparedStatement.setObject(6, invalidList.get(i).getTags());
                            preparedStatement.setObject(7, invalidList.get(i).getDataSource());
                            preparedStatement.setObject(8, invalidList.get(i).getDescription());
                            preparedStatement.setObject(9, invalidList.get(i).getCreateTime());
                            preparedStatement.setObject(10, invalidList.get(i).getDelMark());
                            preparedStatement.setObject(11, invalidList.get(i).getActive());
                            preparedStatement.setObject(12, LocalDateTime.now());
                            preparedStatement.setObject(13, LocalDateTime.now());
                            preparedStatement.setObject(14, invalidList.get(i).getSource());
                            preparedStatement.setObject(15, invalidList.get(i).getIndex());
                            preparedStatement.setObject(16, invalidList.get(i).getType());
                            preparedStatement.setObject(17, invalidList.get(i).getFormal());
                            preparedStatement.setObject(18, invalidList.get(i).getDataSource());
                            preparedStatement.setObject(19, invalidList.get(i).getSource());
                            preparedStatement.setObject(20, 4);
                        }

                        @Override
                        public int getBatchSize() {
                            return deleteList.size();
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
                    String deleteDynamicAttrSql = "delete from dynamic_attr where   type='POST' and tenant_id = ?";
                    jdbcIGA.update(deleteDynamicAttrSql, new Object[]{new String(tenantId)});

                    String deleteDynamicValueSql = "delete from dynamic_value where  tenant_id = ? and attr_id not in (select id from dynamic_attr )";
                    jdbcIGA.update(deleteDynamicValueSql, new Object[]{new String(tenantId)});

                    String addDynamicValueSql = "INSERT INTO dynamic_attr (id, name, code, required, description, tenant_id, create_time, update_time, type, field_type, format, is_search, attr_index) VALUES (?,?,?,?,?,?,?,?,'POST',?,?,?,?)";
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

                return 1;
            } catch (Exception e) {
                transactionStatus.setRollbackOnly();
                // transactionStatus.rollbackToSavepoint(savepoint);
                e.printStackTrace();
                throw new CustomException(ResultCode.FAILED, "同步终止，岗位同步异常！");
            }
        });
    }


    @Override
    public List<TreeBean> findActiveDataByTenantId(String tenantId) {
        String sql = "select  user_type as code , name, parent_code as parentCode , " +
                " create_time as createTime," +
                "source,data_source as dataSource,user_type_index as deptIndex,del_mark as delMark,update_time as updateTime,post_type as type,active,active_time as activeTime,formal,orphan from user_type where tenant_id = ? and " +
                " active = true and del_mark = false and client_id is null or client_id = '' ";
        List<Map<String, Object>> mapList = jdbcSSO.queryForList(sql, tenantId);
        return getUserTypes(mapList);
    }
}
