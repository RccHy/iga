package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bean.TreeBean;
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

    @Resource(name = "sso-txTemplate")
    TransactionTemplate txTemplate2;


    @Override
    public List<TreeBean> findByTenantId(String id) {
        //
        String sql = "select  user_type as code , name, parent_code as parentCode , " +
                " update_time as createTime," +
                "source,data_source as dataSource,user_type_index as deptIndex,del_mark as delMark,update_time as updateTime,post_type as type,active from user_type where tenant_id = ? and client_id is null or client_id = ''  ";

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
                ", user_type_index = ?,del_mark =0  where user_type =?";
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
                preparedStatement.setObject(7, list.get(i).getCreateTime() == null ? LocalDateTime.now() : list.get(i).getCreateTime());
                preparedStatement.setObject(8, list.get(i).getTags());
                preparedStatement.setObject(9, list.get(i).getSource());
                preparedStatement.setObject(10, null == list.get(i).getDeptIndex() ? null : list.get(i).getDeptIndex());
                preparedStatement.setObject(11, list.get(i).getCode());

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
                preparedStatement.setObject(16, list.get(i).getDeptIndex());
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
                " tags ,data_source as dataSource , description ,source,post_type as postType,user_type_index as deptIndex,del_mark as delMark ,active " +
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
                " tags ,data_source as dataSource , description ,source,post_type as postType,user_type_index as deptIndex,active  " +
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

    @Override
    public Integer renewData(ArrayList<TreeBean> insertList, ArrayList<TreeBean> updateList, ArrayList<TreeBean> deleteList, ArrayList<TreeBean> invalidList, String tenantId) {
        String insertStr = "insert into user_type (id,user_type, name, parent_code, can_login ,tenant_id ,tags," +
                " data_source, description,create_time,del_mark,active,active_time,update_time,source,user_type_index,post_type,formal) values" +
                "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        return txTemplate2.execute(transactionStatus -> {
            try {
                if (null != insertList && insertList.size() > 0) {
                    jdbcSSO.batchUpdate(insertStr, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, UUID.randomUUID().toString().replace("-", ""));
                            preparedStatement.setObject(2, insertList.get(i).getCode());
                            preparedStatement.setObject(3, insertList.get(i).getName());
                            preparedStatement.setObject(4, insertList.get(i).getParentCode());
                            preparedStatement.setObject(5, 0);
                            preparedStatement.setObject(6, tenantId);
                            preparedStatement.setObject(7, insertList.get(i).getTags());
                            preparedStatement.setObject(8, "PULL");
                            preparedStatement.setObject(9, insertList.get(i).getDescription());
                            preparedStatement.setObject(10, insertList.get(i).getCreateTime());
                            preparedStatement.setObject(11, insertList.get(i).getDelMark());
                            preparedStatement.setObject(12, insertList.get(i).getActive());
                            preparedStatement.setObject(13, LocalDateTime.now());
                            preparedStatement.setObject(14, insertList.get(i).getUpdateTime());
                            preparedStatement.setObject(15, insertList.get(i).getSource());
                            preparedStatement.setObject(16, insertList.get(i).getDeptIndex());
                            preparedStatement.setObject(17, insertList.get(i).getType());
                            preparedStatement.setObject(18, insertList.get(i).getFormal());
                        }

                        @Override
                        public int getBatchSize() {
                            return insertList.size();
                        }
                    });
                }
                String updateStr = "update user_type set  name=?, parent_code=?, del_mark=? ,tenant_id =?" +
                        ", data_source=?, description=?,update_time=?,tags=?,source=?" +
                        ", user_type_index = ?,post_type=?,active=?,formal=?  where user_type =? and update_time<= ?";
                if (null != updateList && updateList.size() > 0) {
                    jdbcSSO.batchUpdate(updateStr, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, updateList.get(i).getName());
                            preparedStatement.setObject(2, updateList.get(i).getParentCode());
                            preparedStatement.setObject(3, null == updateList.get(i).getDelMark() ? 0 : updateList.get(i).getDelMark());
                            preparedStatement.setObject(4, tenantId);
                            preparedStatement.setObject(5, "PULL");
                            preparedStatement.setObject(6, updateList.get(i).getDescription());
                            preparedStatement.setObject(7, updateList.get(i).getUpdateTime());
                            preparedStatement.setObject(8, updateList.get(i).getTags());
                            preparedStatement.setObject(9, updateList.get(i).getSource());
                            preparedStatement.setObject(10, updateList.get(i).getDeptIndex());
                            preparedStatement.setObject(11, updateList.get(i).getType());
                            preparedStatement.setObject(12, updateList.get(i).getActive());
                            preparedStatement.setObject(13, updateList.get(i).getFormal());
                            preparedStatement.setObject(14, updateList.get(i).getCode());
                            preparedStatement.setObject(15, updateList.get(i).getUpdateTime());

                        }

                        @Override
                        public int getBatchSize() {
                            return updateList.size();
                        }
                    });
                }
                String deleteStr = "update user_type set   del_mark= ? , active = ?,active_time= ? ,update_time=? " +
                        "where user_type =?  and update_time<= ? ";

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
                            preparedStatement.setObject(5, treeBeans.get(i).getCode());
                            preparedStatement.setObject(6, treeBeans.get(i).getUpdateTime());
                        }

                        @Override
                        public int getBatchSize() {
                            return treeBeans.size();
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
                " update_time as createTime," +
                "source,data_source as dataSource,user_type_index as deptIndex,del_mark as delMark,update_time as updateTime,post_type as type,active from user_type where tenant_id = ? and " +
                " active = true and del_mark = false and client_id is null or client_id = '' ";
        List<Map<String, Object>> mapList = jdbcSSO.queryForList(sql, tenantId);
        return getUserTypes(mapList);
    }
}
