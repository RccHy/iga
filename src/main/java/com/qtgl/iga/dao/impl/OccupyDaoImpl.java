package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bean.IgaOccupy;
import com.qtgl.iga.bean.OccupyDto;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.dao.DynamicAttrDao;
import com.qtgl.iga.dao.OccupyDao;
import com.qtgl.iga.utils.FilterCodeEnum;
import com.qtgl.iga.utils.MyBeanUtils;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
@Component
@Slf4j
public class OccupyDaoImpl implements OccupyDao {


    @Resource(name = "jdbcSSO")
    JdbcTemplate jdbcSSO;
    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;

    @Resource(name = "sso-txTemplate")
    TransactionTemplate txTemplate;

    @Resource(name = "iga-txTemplate")
    TransactionTemplate igaTemplate;
    @Resource
    DynamicAttrDao dynamicAttrDao;

    @Override
    public List<OccupyDto> findAll(String tenantId, String deptCode, String postCode) {
        // 以人（identity）为主查询 人和身份的关联关系（identity_user）数据。再通过关联数据 查询身份信息详情。过滤掉 关键属性为空的数据行
        String sql = "select " +
                "       iu.identity_id as personId," +
                "       iu.user_id     as occupyId," +
                "       u.user_type               as postCode," +
                "       u.dept_code               as deptCode," +
                "       i.card_type               as personCardType," +
                "       i.card_no               as personCardNo," +
                "       u.card_type               as identityCardType," +
                "       u.user_code                 as identityCardNo," +
                "       u.del_mark                  as delMark," +
                "       u.start_time                  as startTime," +
                "       u.end_time                  as endTime," +
                "       u.valid_start_time                  as validStartTime," +
                "       u.valid_end_time                  as validEndTime," +
                "       u.user_index                  as `index`," +
                "       u.source                                ," +
                "       u.data_source               as dataSource," +
                "       u.create_time               as createTime," +
                "       u.update_time               as updateTime," +
                "       u.active               as active," +
                "       u.account_no               as accountNo," +
                "       u.orphan               as orphan" +
                " from identity i" +
                "         left join identity_user iu on i.id = iu.identity_id" +
                "         left join user u on iu.user_id = u.id" +
                " where i.tenant_id = ?" +
                "  and u.dept_code is not null" +
                "  and u.user_type is not null" +
                "  and i.del_mark=0" +
                "  and u.del_mark=0;";


        List<Map<String, Object>> mapList = jdbcSSO.queryForList(sql, tenantId);
        List<OccupyDto> occupies = new ArrayList<>();
        mapList.forEach(map -> {
            OccupyDto occupy = new OccupyDto();
            try {
                MyBeanUtils.populate(occupy, map);
            } catch (Exception e) {
                e.printStackTrace();
            }
            occupies.add(occupy);
        });
        return occupies;
    }


    @Override
    public Integer saveToSso(Map<String, List<OccupyDto>> occupyMap, String tenantId, List<DynamicValue> valueUpdate, List<DynamicValue> valueInsert) {
        return txTemplate.execute(transactionStatus -> {
            try {
                if (occupyMap.containsKey("insert")) {
                    List<OccupyDto> list = occupyMap.get("insert");
                    String sql = "INSERT INTO user " +
                            "               (id, user_type, card_type,user_code, del_mark, start_time, end_time, create_time, update_time, tenant_id, dept_code, source, data_source, active, active_time,user_index,post_code,account_no,valid_start_time,valid_end_time,orphan,create_data_source,create_source) " +
                            "               VALUES (?,?,?,?,0,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                    int[] ints = jdbcSSO.batchUpdate(sql, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, list.get(i).getOccupyId());
                            preparedStatement.setObject(2, list.get(i).getPostCode());
                            preparedStatement.setObject(3, list.get(i).getIdentityCardType());
                            preparedStatement.setObject(4, list.get(i).getIdentityCardNo());
                            preparedStatement.setObject(5, list.get(i).getStartTime());
                            preparedStatement.setObject(6, list.get(i).getEndTime());
                            preparedStatement.setObject(7, list.get(i).getCreateTime());
                            preparedStatement.setObject(8, list.get(i).getUpdateTime());
                            preparedStatement.setObject(9, tenantId);
                            preparedStatement.setObject(10, list.get(i).getDeptCode());
                            preparedStatement.setObject(11, list.get(i).getSource());
                            preparedStatement.setObject(12, list.get(i).getDataSource());
                            preparedStatement.setObject(13, list.get(i).getActive());
                            preparedStatement.setObject(14, LocalDateTime.now());
                            preparedStatement.setObject(15, list.get(i).getIndex());
                            preparedStatement.setObject(16, list.get(i).getPostCode());
                            preparedStatement.setObject(17, list.get(i).getAccountNo());
                            preparedStatement.setObject(18, list.get(i).getValidStartTime());
                            preparedStatement.setObject(19, list.get(i).getValidEndTime());
                            preparedStatement.setObject(20, list.get(i).getOrphan());
                            preparedStatement.setObject(21, list.get(i).getDataSource());
                            preparedStatement.setObject(22, list.get(i).getSource());
                        }

                        @Override
                        public int getBatchSize() {
                            return list.size();
                        }
                    });

                    String sql2 = "INSERT INTO identity_user (id, identity_id, user_id) VALUES (?, ?, ?)";
                    jdbcSSO.batchUpdate(sql2, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, UUID.randomUUID().toString());
                            preparedStatement.setObject(2, list.get(i).getPersonId());
                            preparedStatement.setObject(3, list.get(i).getOccupyId());

                        }

                        @Override
                        public int getBatchSize() {
                            return list.size();
                        }
                    });
                }


                if (occupyMap.containsKey("update") || occupyMap.containsKey("invalid") || occupyMap.containsKey("recover")) {
                    List<OccupyDto> list = new ArrayList<>();
                    List<OccupyDto> update = occupyMap.get("update");
                    List<OccupyDto> invalid = occupyMap.get("invalid");
                    if (null != update) {
                        list.addAll(update);
                    }
                    if (null != invalid) {
                        list.addAll(invalid);
                    }


                    String sql = "UPDATE `user` SET user_type = ?, card_type = ?, user_code = ?, del_mark = ?, start_time = ?, end_time = ?, update_time = ?,dept_code = ?,  " +
                            " source = ?, data_source = ?,  user_index = ?,active=?,active_time=?,account_no=?,valid_start_time=?,valid_end_time=?,orphan=?" +
                            " WHERE id = ? and update_time <= ?  ";

                    int[] ints = jdbcSSO.batchUpdate(sql, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, list.get(i).getPostCode());
                            preparedStatement.setObject(2, list.get(i).getIdentityCardType());
                            preparedStatement.setObject(3, list.get(i).getIdentityCardNo());
                            preparedStatement.setObject(4, list.get(i).getDelMark());
                            preparedStatement.setObject(5, list.get(i).getStartTime());
                            preparedStatement.setObject(6, list.get(i).getEndTime());
                            preparedStatement.setObject(7, list.get(i).getUpdateTime());
                            preparedStatement.setObject(8, list.get(i).getDeptCode());
                            preparedStatement.setObject(9, list.get(i).getSource());
                            preparedStatement.setObject(10, list.get(i).getDataSource());
                            preparedStatement.setObject(11, list.get(i).getIndex());
                            preparedStatement.setObject(12, list.get(i).getActive());
                            preparedStatement.setObject(13, list.get(i).getActiveTime());
                            preparedStatement.setObject(14, list.get(i).getAccountNo());
                            preparedStatement.setObject(15, list.get(i).getValidStartTime());
                            preparedStatement.setObject(16, list.get(i).getValidEndTime());
                            preparedStatement.setObject(17, list.get(i).getOrphan() == null ? 0 : list.get(i).getOrphan());
                            preparedStatement.setObject(18, list.get(i).getOccupyId());
                            preparedStatement.setObject(19, list.get(i).getUpdateTime());
                        }

                        @Override
                        public int getBatchSize() {
                            return list.size();
                        }
                    });
                }


                if (occupyMap.containsKey("delete")) {
                    List<OccupyDto> list = occupyMap.get("delete");
                    String sql = "UPDATE `user` SET  del_mark = 1, update_time = ?, data_source=?,active=0,valid_start_time=?,valid_end_time=? " +
                            " WHERE id = ? and update_time <= ?  ";
                    int[] ints = jdbcSSO.batchUpdate(sql, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, list.get(i).getUpdateTime());
                            preparedStatement.setObject(2, list.get(i).getDataSource());
                            preparedStatement.setObject(3, list.get(i).getValidStartTime());
                            preparedStatement.setObject(4, list.get(i).getValidEndTime());
                            preparedStatement.setObject(5, list.get(i).getOccupyId());
                            preparedStatement.setObject(6, list.get(i).getUpdateTime());
                        }

                        @Override
                        public int getBatchSize() {
                            return list.size();
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
                e.printStackTrace();
                transactionStatus.setRollbackOnly();
                throw new CustomException(ResultCode.FAILED, "同步终止，人员身份同步异常！");
            }

        });
    }


    @Override
    public Integer saveToSsoTest(Map<String, List<OccupyDto>> occupyMap, String tenantId, List<DynamicValue> valueUpdate, List<DynamicValue> valueInsert, List<DynamicAttr> attrList, List<DynamicValue> dynamicValues) {


        String sql = "INSERT INTO user " +
                "               (id, user_type, card_type,user_code, del_mark, start_time, end_time, create_time, update_time, tenant_id, dept_code, source, data_source, active, active_time,user_index,account_no,valid_start_time,valid_end_time,orphan,create_data_source,create_source,sync_state,identity_id) " +
                "               VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        return igaTemplate.execute(transactionStatus -> {
            try {
                // 删除租户下数据
                String delUserSql = "delete from user where tenant_id = ? ";
                jdbcIGA.update(delUserSql, tenantId);

                if (occupyMap.containsKey("keep")) {
                    List<OccupyDto> list = occupyMap.get("keep");
                    int[] ints = jdbcIGA.batchUpdate(sql, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, list.get(i).getOccupyId());
                            preparedStatement.setObject(2, list.get(i).getPostCode());
                            preparedStatement.setObject(3, list.get(i).getIdentityCardType());
                            preparedStatement.setObject(4, list.get(i).getIdentityCardNo());
                            preparedStatement.setObject(5, list.get(i).getDelMark());
                            preparedStatement.setObject(6, list.get(i).getStartTime());
                            preparedStatement.setObject(7, list.get(i).getEndTime());
                            preparedStatement.setObject(8, list.get(i).getCreateTime());
                            preparedStatement.setObject(9, list.get(i).getUpdateTime());
                            preparedStatement.setObject(10, tenantId);
                            preparedStatement.setObject(11, list.get(i).getDeptCode());
                            preparedStatement.setObject(12, list.get(i).getSource());
                            preparedStatement.setObject(13, list.get(i).getDataSource());
                            preparedStatement.setObject(14, list.get(i).getActive());
                            preparedStatement.setObject(15, LocalDateTime.now());
                            preparedStatement.setObject(16, list.get(i).getIndex());
                            preparedStatement.setObject(17, list.get(i).getAccountNo());
                            preparedStatement.setObject(18, list.get(i).getValidStartTime());
                            preparedStatement.setObject(19, list.get(i).getValidEndTime());
                            preparedStatement.setObject(20, list.get(i).getOrphan());
                            preparedStatement.setObject(21, list.get(i).getDataSource());
                            preparedStatement.setObject(22, list.get(i).getSource());
                            preparedStatement.setObject(23, 0);
                            preparedStatement.setObject(24, list.get(i).getPersonId());
                        }

                        @Override
                        public int getBatchSize() {
                            return list.size();
                        }
                    });
                }


                if (occupyMap.containsKey("insert")) {
                    List<OccupyDto> list = occupyMap.get("insert");
                    int[] ints = jdbcIGA.batchUpdate(sql, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, list.get(i).getOccupyId());
                            preparedStatement.setObject(2, list.get(i).getPostCode());
                            preparedStatement.setObject(3, list.get(i).getIdentityCardType());
                            preparedStatement.setObject(4, list.get(i).getIdentityCardNo());
                            preparedStatement.setObject(5, list.get(i).getDelMark());
                            preparedStatement.setObject(6, list.get(i).getStartTime());
                            preparedStatement.setObject(7, list.get(i).getEndTime());
                            preparedStatement.setObject(8, list.get(i).getCreateTime());
                            preparedStatement.setObject(9, list.get(i).getUpdateTime());
                            preparedStatement.setObject(10, tenantId);
                            preparedStatement.setObject(11, list.get(i).getDeptCode());
                            preparedStatement.setObject(12, list.get(i).getSource());
                            preparedStatement.setObject(13, list.get(i).getDataSource());
                            preparedStatement.setObject(14, list.get(i).getActive());
                            preparedStatement.setObject(15, LocalDateTime.now());
                            preparedStatement.setObject(16, list.get(i).getIndex());
                            preparedStatement.setObject(17, list.get(i).getAccountNo());
                            preparedStatement.setObject(18, list.get(i).getValidStartTime());
                            preparedStatement.setObject(19, list.get(i).getValidEndTime());
                            preparedStatement.setObject(20, list.get(i).getOrphan());
                            preparedStatement.setObject(21, list.get(i).getDataSource());
                            preparedStatement.setObject(22, list.get(i).getSource());
                            preparedStatement.setObject(23, 1);
                            preparedStatement.setObject(24, list.get(i).getPersonId());
                        }

                        @Override
                        public int getBatchSize() {
                            return list.size();
                        }
                    });
                }


                if (occupyMap.containsKey("update")) {
                    List<OccupyDto> list = occupyMap.get("update");
                    int[] ints = jdbcIGA.batchUpdate(sql, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, list.get(i).getOccupyId());
                            preparedStatement.setObject(2, list.get(i).getPostCode());
                            preparedStatement.setObject(3, list.get(i).getIdentityCardType());
                            preparedStatement.setObject(4, list.get(i).getIdentityCardNo());
                            preparedStatement.setObject(5, list.get(i).getDelMark());
                            preparedStatement.setObject(6, list.get(i).getStartTime());
                            preparedStatement.setObject(7, list.get(i).getEndTime());
                            preparedStatement.setObject(8, list.get(i).getCreateTime());
                            preparedStatement.setObject(9, LocalDateTime.now());
                            preparedStatement.setObject(10, tenantId);
                            preparedStatement.setObject(11, list.get(i).getDeptCode());
                            preparedStatement.setObject(12, list.get(i).getSource());
                            preparedStatement.setObject(13, list.get(i).getDataSource());
                            preparedStatement.setObject(14, list.get(i).getActive());
                            preparedStatement.setObject(15, list.get(i).getCreateTime());
                            preparedStatement.setObject(16, list.get(i).getIndex());
                            preparedStatement.setObject(17, list.get(i).getAccountNo());
                            preparedStatement.setObject(18, list.get(i).getValidStartTime());
                            preparedStatement.setObject(19, list.get(i).getValidEndTime());
                            preparedStatement.setObject(20, list.get(i).getOrphan());
                            preparedStatement.setObject(21, list.get(i).getDataSource());
                            preparedStatement.setObject(22, list.get(i).getSource());
                            preparedStatement.setObject(23, 3);
                            preparedStatement.setObject(24, list.get(i).getPersonId());
                        }

                        @Override
                        public int getBatchSize() {
                            return list.size();
                        }
                    });
                }

                if (occupyMap.containsKey("delete")) {
                    List<OccupyDto> list = occupyMap.get("delete");

                    int[] ints = jdbcSSO.batchUpdate(sql, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, list.get(i).getOccupyId());
                            preparedStatement.setObject(2, list.get(i).getPostCode());
                            preparedStatement.setObject(3, list.get(i).getIdentityCardType());
                            preparedStatement.setObject(4, list.get(i).getIdentityCardNo());
                            preparedStatement.setObject(5, 1);
                            preparedStatement.setObject(6, list.get(i).getStartTime());
                            preparedStatement.setObject(7, list.get(i).getEndTime());
                            preparedStatement.setObject(8, list.get(i).getCreateTime());
                            preparedStatement.setObject(9, LocalDateTime.now());
                            preparedStatement.setObject(10, tenantId);
                            preparedStatement.setObject(11, list.get(i).getDeptCode());
                            preparedStatement.setObject(12, list.get(i).getSource());
                            preparedStatement.setObject(13, list.get(i).getDataSource());
                            preparedStatement.setObject(14, list.get(i).getActive());
                            preparedStatement.setObject(15, list.get(i).getCreateTime());
                            preparedStatement.setObject(16, list.get(i).getIndex());
                            preparedStatement.setObject(17, list.get(i).getAccountNo());
                            preparedStatement.setObject(18, list.get(i).getValidStartTime());
                            preparedStatement.setObject(19, list.get(i).getValidEndTime());
                            preparedStatement.setObject(20, list.get(i).getOrphan());
                            preparedStatement.setObject(21, list.get(i).getDataSource());
                            preparedStatement.setObject(22, list.get(i).getSource());
                            preparedStatement.setObject(23, 2);
                            preparedStatement.setObject(24, list.get(i).getPersonId());
                        }

                        @Override
                        public int getBatchSize() {
                            return list.size();
                        }
                    });
                }

                if (occupyMap.containsKey("invalid")) {
                    List<OccupyDto> list = occupyMap.get("invalid");
                    int[] ints = jdbcIGA.batchUpdate(sql, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, list.get(i).getOccupyId());
                            preparedStatement.setObject(2, list.get(i).getPostCode());
                            preparedStatement.setObject(3, list.get(i).getIdentityCardType());
                            preparedStatement.setObject(4, list.get(i).getIdentityCardNo());
                            preparedStatement.setObject(5, list.get(i).getDelMark());
                            preparedStatement.setObject(6, list.get(i).getStartTime());
                            preparedStatement.setObject(7, list.get(i).getEndTime());
                            preparedStatement.setObject(8, list.get(i).getCreateTime());
                            preparedStatement.setObject(9, LocalDateTime.now());
                            preparedStatement.setObject(10, tenantId);
                            preparedStatement.setObject(11, list.get(i).getDeptCode());
                            preparedStatement.setObject(12, list.get(i).getSource());
                            preparedStatement.setObject(13, list.get(i).getDataSource());
                            preparedStatement.setObject(14, list.get(i).getActive());
                            preparedStatement.setObject(15, list.get(i).getCreateTime());
                            preparedStatement.setObject(16, list.get(i).getIndex());
                            preparedStatement.setObject(17, list.get(i).getAccountNo());
                            preparedStatement.setObject(18, list.get(i).getValidStartTime());
                            preparedStatement.setObject(19, list.get(i).getValidEndTime());
                            preparedStatement.setObject(20, list.get(i).getOrphan());
                            preparedStatement.setObject(21, list.get(i).getDataSource());
                            preparedStatement.setObject(22, list.get(i).getSource());
                            preparedStatement.setObject(23, 4);
                            preparedStatement.setObject(24, list.get(i).getPersonId());
                        }

                        @Override
                        public int getBatchSize() {
                            return list.size();
                        }
                    });
                }

                if (!CollectionUtils.isEmpty(attrList)) {
                    // 删除、并重新创建扩展字段
                    String deleteDynamicAttrSql = "delete from dynamic_attr where   type='IDENTITY' and tenant_id = ?";
                    jdbcIGA.update(deleteDynamicAttrSql, tenantId);
                    String addDynamicValueSql = "INSERT INTO dynamic_attr (id, name, code, required, description, tenant_id, create_time, update_time, type, field_type, format, is_search, attr_index) VALUES (?,?,?,?,?,?,?,?,'IDENTITY',?,?,?,?)";
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
                            return attrList.size();
                        }
                    });
                    // 删除value条件
                    String deleteDynamicValueSql = "delete from dynamic_value where  tenant_id = ? and attr_id  in (select id from dynamic_attr where type='IDENTITY' and tenant_id=?  )";
                    jdbcIGA.update(deleteDynamicValueSql, tenantId, tenantId);
                    String valueStr = "INSERT INTO dynamic_value (`id`, `attr_id`, `entity_id`, `value`, `tenant_id`) VALUES (?, ?, ?, ?, ?)";

                    if (!CollectionUtils.isEmpty(dynamicValues)) {
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
                    if (!CollectionUtils.isEmpty(valueInsert)) {
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
                    if (!CollectionUtils.isEmpty(valueUpdate)) {
                        String valueUpdateStr = "update dynamic_value set `value`=? where id= ?";
                        jdbcIGA.batchUpdate(valueUpdateStr, new BatchPreparedStatementSetter() {
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
                }


                return 1;
            } catch (Exception e) {
                e.printStackTrace();
                transactionStatus.setRollbackOnly();
                throw new CustomException(ResultCode.FAILED, "同步终止，人员身份同步异常！");
            }

        });
    }

    @Override
    public void removeData(DomainInfo domain) {
        String sql = " delete from  `occupy_temp` where tenant_id =? ";
        jdbcIGA.update(sql, domain.getId());
    }

    @Override
    public Integer saveToTemp(List<OccupyDto> occupyDtos, DomainInfo domain) {
        String sql = "INSERT INTO occupy_temp " +
                "               (id, user_type, card_type,user_code, del_mark, start_time, end_time, create_time, update_time, tenant_id, dept_code, source, data_source, active, active_time,user_index,post_code,account_no,valid_start_time,valid_end_time,orphan,person_card_no,person_card_type) " +
                "               VALUES (?,?,?,?,0,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        int[] ints = jdbcIGA.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                preparedStatement.setObject(1, occupyDtos.get(i).getOccupyId());
                preparedStatement.setObject(2, occupyDtos.get(i).getPostCode());
                preparedStatement.setObject(3, occupyDtos.get(i).getIdentityCardType());
                preparedStatement.setObject(4, occupyDtos.get(i).getIdentityCardNo());
                preparedStatement.setObject(5, occupyDtos.get(i).getStartTime());
                preparedStatement.setObject(6, occupyDtos.get(i).getEndTime());
                preparedStatement.setObject(7, occupyDtos.get(i).getCreateTime());
                preparedStatement.setObject(8, occupyDtos.get(i).getUpdateTime());
                preparedStatement.setObject(9, domain.getId());
                preparedStatement.setObject(10, occupyDtos.get(i).getDeptCode());
                preparedStatement.setObject(11, occupyDtos.get(i).getSource());
                preparedStatement.setObject(12, occupyDtos.get(i).getDataSource());
                preparedStatement.setObject(13, occupyDtos.get(i).getActive());
                preparedStatement.setObject(14, LocalDateTime.now());
                preparedStatement.setObject(15, occupyDtos.get(i).getIndex());
                preparedStatement.setObject(16, occupyDtos.get(i).getPostCode());
                preparedStatement.setObject(17, occupyDtos.get(i).getAccountNo());
                preparedStatement.setObject(18, occupyDtos.get(i).getValidStartTime());
                preparedStatement.setObject(19, occupyDtos.get(i).getValidEndTime());
                preparedStatement.setObject(20, occupyDtos.get(i).getOrphan());
                preparedStatement.setObject(21, occupyDtos.get(i).getPersonCardNo());
                preparedStatement.setObject(22, occupyDtos.get(i).getPersonCardType());
            }

            @Override
            public int getBatchSize() {
                return occupyDtos.size();
            }
        });
        return null;
    }

    @Override
    public List<OccupyDto> findOccupyTemp(Map<String, Object> arguments, DomainInfo domain) {
        String sql = " SELECT id as occupyId,user_type as postCode ,card_type as identityCardType,user_code as identityCardNo, " +
                " del_mark as delMark,start_time as startTime,end_time as endTime,create_time as createTime , update_time as updateTime ," +
                " dept_code as deptCode ,source  ,data_source as dataSource  ,active  ,account_no as accountNo,person_card_no as personCardNo ,person_card_type as personCardType from occupy_temp where tenant_id=? ";
        //拼接sql
        StringBuffer stb = new StringBuffer(sql);
        //存入参数
        List<Object> param = new ArrayList<>();
        param.add(domain.getId());
        dealData(arguments, stb, param);
        stb.append(" order by create_time desc ");
        //查询所有
        if (null != arguments.get("offset") && null != arguments.get("first")) {

            stb.append(" limit ?,? ");
            param.add(null == arguments.get("offset") ? 0 : arguments.get("offset"));
            param.add(null == arguments.get("first") ? 0 : arguments.get("first"));
        }
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(stb.toString(), param.toArray());
        ArrayList<OccupyDto> list = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {
                OccupyDto occupyDto = new OccupyDto();
                try {
                    MyBeanUtils.populate(occupyDto, map);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
                list.add(occupyDto);
            }
            return list;
        }
        return null;
    }

    @Override
    public Integer findOccupyTempCount(Map<String, Object> arguments, DomainInfo domain) {
        String sql = " SELECT count(*) from occupy_temp where tenant_id=?  ";
        //拼接sql
        StringBuffer stb = new StringBuffer(sql);
        List<Object> param = new ArrayList<>();
        param.add(domain.getId());
        if (null != arguments) {
            dealData(arguments, stb, param);
        }
        Integer integer = jdbcIGA.queryForObject(stb.toString(), param.toArray(), Integer.class);
        return integer;
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
                    if ("name".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if ("like".equals(FilterCodeEnum.getDescByCode(soe.getKey()))) {
                                stb.append("and name ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if ("in".equals(FilterCodeEnum.getDescByCode(soe.getKey())) || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
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
                    if ("username".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if ("like".equals(FilterCodeEnum.getDescByCode(soe.getKey()))) {
                                stb.append("and account_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if ("in".equals(FilterCodeEnum.getDescByCode(soe.getKey())) || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and account_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and account_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
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
                    if ("positionCardNo".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "like")) {
                                stb.append("and user_code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and user_code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and user_code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("cardNo".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "like")) {
                                stb.append("and person_card_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and person_card_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and person_card_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("deptCode".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("like")) {
                                stb.append("and dept_code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and dept_code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and dept_code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("postCode".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("like")) {
                                stb.append("and user_type ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and user_type ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and user_type ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("positionStartTime".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            //判断是否是区间
                            if (null != soe.getValue() && (!"".equals(soe.getValue()))) {
                                if ("gt".equals(soe.getKey()) || "lt".equals(soe.getKey())
                                        || "gte".equals(soe.getKey()) || "lte".equals(soe.getKey()) || "eq".equals(soe.getKey())) {
                                    stb.append("and start_time ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                    param.add(soe.getValue());

                                }
                            }
                        }
                    }
                    if ("positionEndTime".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            //判断是否是区间
                            if (null != soe.getValue() && (!"".equals(soe.getValue()))) {
                                if ("gt".equals(soe.getKey()) || "lt".equals(soe.getKey())
                                        || "gte".equals(soe.getKey()) || "lte".equals(soe.getKey()) || "eq".equals(soe.getKey())) {
                                    stb.append("and end_time ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                    param.add(soe.getValue());

                                }
                            }
                        }
                    }

                }

            }
        }
    }


    @Override
    public Map<String, Object> igaOccupy(Map<String, Object> arguments, Tenant tenant) {
        Object first = arguments.get("first");
        Object offset = arguments.get("offset");

        List<Object> params = new ArrayList<>();

        String queryStr = "SELECT " +
                "                 DISTINCT a.id," +
                "                 a.account_no AS accountNo, " +
                "                 a.name ," +
                "                 a.card_type AS cardType," +
                "                 a.card_no AS cardNo," +
                "                 a.open_id AS openId" +
                "                 FROM identity a INNER JOIN user b on a.id = b.identity_id ";
        String countSql = "SELECT COUNT(DISTINCT a.id) from identity a  INNER JOIN user b on a.id = b.identity_id  ";
        //主体查询语句
        StringBuffer stb = new StringBuffer(queryStr);
        //查询总数语句
        StringBuffer countStb = new StringBuffer(countSql);

        StringBuffer strTemp = new StringBuffer("  ");
        params.add(tenant.getId());
        strTemp = dealDataAndAttr(arguments, strTemp, params, tenant.getId());
        stb.append(strTemp);
        countStb.append(strTemp);
        if (null != first && null != offset) {
            stb.append(" limit ").append(offset).append(",").append(first);
        }

        log.info(stb.toString());
        log.info(countStb.toString());
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(stb.toString(), params.toArray());
        ArrayList<IgaOccupy> list = new ArrayList<>();
        if (!CollectionUtils.isEmpty(mapList)) {

            mapList.forEach(map -> {
                IgaOccupy person = new IgaOccupy();
                try {
                    MyBeanUtils.populate(person, map);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                list.add(person);
            });


        }
        Integer count = jdbcIGA.queryForObject(countStb.toString(), params.toArray(), Integer.class);

        ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<>();
        map.put("count", null == count ? 0 : count);
        map.put("list", list);
        return map;
    }

    @Override
    public List<OccupyDto> findOccupyByIdentityId(Set<String> keySet, Map<String, Object> arguments,Tenant tenant) {
        //根据人员id 和  筛选条件查询 身份并返回
        List<Object> params = new ArrayList<>();

        String queryStr = "SELECT " +
                "                 a.id as occupyId," +
                "                 a.identity_id as personId, " +
                "                 a.user_type AS postCode, " +
                "                 a.user_code AS identityCardNo, " +
                "                 a.name ," +
                "                 a.card_type AS cardType," +
                "                 a.del_mark AS delMark," +
                "                 a.start_time AS startTime," +
                "                 a.end_time AS endTime," +
                "                 a.create_time AS createTime," +
                "                 a.update_time AS updateTime," +
                "                 a.tenant_id AS tenantId," +
                "                 a.dept_code AS deptCode," +
                "                 a.source ," +
                "                 a.data_source AS dataSource," +
                "                 a.active ," +
                "                 a.active_time AS activeTime," +
                "                 a.tags ," +
                "                 a.description ," +
                "                 a.user_index AS 'index'," +
                "                 a.valid_start_time AS validStartTime," +
                "                 a.valid_end_time AS validEndTime," +
                "                 a.orphan ," +
                "                 a.create_data_source AS createDataSource," +
                "                 a.create_source AS createSource," +
                "                 a.sync_state AS syncState" +
                "                 FROM  user  a ";
        StringBuffer stb = new StringBuffer(queryStr);
        StringBuffer strTemp = new StringBuffer("  ");
        params.add(tenant.getId());
        strTemp = dealDataAndAttrOccupy(arguments, strTemp, params, tenant.getId(),keySet);

        if(!CollectionUtils.isEmpty(keySet)){
            strTemp.append(" and  a.identity_id in ( ");
            for (String identityId : keySet) {
                strTemp.append(" ?,");
                params.add(identityId);
            }
            strTemp.replace(strTemp.length() - 1, strTemp.length(), ") ");
        }
        stb.append(strTemp);
        log.info(stb.toString());
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(stb.toString(), params.toArray());
        ArrayList<OccupyDto> list = new ArrayList<>();
        if (!CollectionUtils.isEmpty(mapList)) {

            mapList.forEach(map -> {
                OccupyDto occupyDto = new OccupyDto();
                try {
                    MyBeanUtils.populate(occupyDto, map);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                list.add(occupyDto);
            });


        }
        return list;
    }

    private StringBuffer dealDataAndAttrOccupy(Map<String, Object> arguments, StringBuffer stb, List<Object> param, String tenantId,Set<String> keySet) {

        //拼接查询sql
        StringBuffer sql = new StringBuffer();

        Iterator<Map.Entry<String, Object>> it = arguments.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();

            if ("filter".equals(entry.getKey())) {
                HashMap<String, Object> map = (HashMap<String, Object>) entry.getValue();
                for (Map.Entry<String, Object> str : map.entrySet()) {

                    if ("dept".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if ("like".equals(FilterCodeEnum.getDescByCode(soe.getKey()))) {
                                stb.append("and a.dept_code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if ("in".equals(FilterCodeEnum.getDescByCode(soe.getKey())) || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and a.dept_code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and a.dept_code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }

                    if ("post".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if ("like".equals(FilterCodeEnum.getDescByCode(soe.getKey()))) {
                                stb.append("and a.user_type ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if ("in".equals(FilterCodeEnum.getDescByCode(soe.getKey())) || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and a.user_type ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and a.user_type ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("syncState".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            stb.append("and a.sync_state ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                            param.add(soe.getValue());
                        }
                    }
                    if ("positionActive".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if(Boolean.parseBoolean(soe.getValue().toString())){
                                stb.append("  and ( NOW() BETWEEN a.valid_start_time and a.valid_end_time) ");
                            }else {
                                stb.append("  and  ( NOW() NOT BETWEEN a.valid_start_time and a.valid_end_time)");

                            }
                        }
                    }

                    if ("source".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "like")) {
                                stb.append("and a.source ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and a.source ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and a.source ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("createSource".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "like")) {
                                stb.append("and a.create_source ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and a.create_source ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and a.create_source ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("positionCardNo".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "like")) {
                                stb.append("and a.card_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and a.card_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and a.card_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("startTime".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            //判断是否是区间
                            if ("gt".equals(soe.getKey()) || "lt".equals(soe.getKey())
                                    || "gte".equals(soe.getKey()) || "lte".equals(soe.getKey())) {
                                stb.append("and a.valid_start_time ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());

                            }
                        }
                    }
                    if ("endTime".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            //判断是否是区间
                            if ("gt".equals(soe.getKey()) || "lt".equals(soe.getKey())
                                    || "gte".equals(soe.getKey()) || "lte".equals(soe.getKey())) {
                                stb.append("and a.valid_end_time ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());

                            }
                        }
                    }
                    if ("extension".equals(str.getKey())) {
                        List<HashMap<String, Object>> values = (List<HashMap<String, Object>>) str.getValue();
                        if (!CollectionUtils.isEmpty(values)) {
                            List<DynamicAttr> users = dynamicAttrDao.findAllByTypeIGA("IDENTITY", tenantId);
                            Map<String, String> collect = users.stream().collect(Collectors.toMap(DynamicAttr::getCode, DynamicAttr::getId));
                            char aliasOld = 'c';

                            for (HashMap<String, Object> value : values) {
                                String key = (String) value.get("key");
                                if (collect.containsKey(key)) {
                                    HashMap<String, Object> val = (HashMap<String, Object>) value.get("value");
                                    for (Map.Entry<String, Object> soe : val.entrySet()) {
                                        if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("like")) {
                                            char aliasNew = (char) (aliasOld + 1);
                                            sql.append(" LEFT JOIN dynamic_value ").append(aliasNew).append(" ON ").append(aliasNew).append(".entity_id = ").append(" a.id ");
                                            stb.append(" and (").append(aliasNew).append(".value = ? and ").append(aliasNew).append(".attr_id=? )");
                                            aliasOld = aliasNew;
                                            param.add("%" + soe.getValue() + "%");
                                            param.add(collect.get(key));
                                        } else {
                                            char aliasNew = (char) (aliasOld + 1);

                                            sql.append(" LEFT JOIN dynamic_value ").append(aliasNew).append(" ON ").append(aliasNew).append(".entity_id = ").append(" a.id ");
                                            stb.append(" and (").append(aliasNew).append(".value = ? and ").append(aliasNew).append(".attr_id=? )");
                                            aliasOld = aliasNew;
                                            param.add(soe.getValue());
                                            param.add(collect.get(key));
                                        }
                                    }

                                }


                            }
                        }
                    }

                }

            }
        }

        StringBuffer buffer = new StringBuffer(" WHERE 1=1 AND a.TENANT_ID = ? ");

        if (StringUtils.isNotBlank(sql)) {
            stb = sql.append(buffer).append(stb);
        } else {
            stb = buffer.append(stb);
        }
        return stb;

    }

    private StringBuffer dealDataAndAttr(Map<String, Object> arguments, StringBuffer stb, List<Object> param, String tenantId) {

        //扩展字段查询拼接主体sql
        StringBuffer sql = new StringBuffer();

        boolean activeFlag = true;
        boolean positionActiveFlag = true;

        Iterator<Map.Entry<String, Object>> it = arguments.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            if ("a.id".equals(entry.getKey())) {
                stb.append("and i.id= ? ");
                param.add(entry.getValue());
            }

            if ("filter".equals(entry.getKey())) {
                HashMap<String, Object> map = (HashMap<String, Object>) entry.getValue();
                for (Map.Entry<String, Object> str : map.entrySet()) {
                    if ("name".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if ("like".equals(FilterCodeEnum.getDescByCode(soe.getKey()))) {
                                stb.append("and a.name ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if ("in".equals(FilterCodeEnum.getDescByCode(soe.getKey())) || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and a.name ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and a.name ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("username".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if ("like".equals(FilterCodeEnum.getDescByCode(soe.getKey()))) {
                                stb.append("and a.account_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if ("in".equals(FilterCodeEnum.getDescByCode(soe.getKey())) || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and a.account_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and a.account_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("syncState".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            stb.append("and b.sync_state ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                            param.add(soe.getValue());
                        }
                    }
                    if ("source".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "like")) {
                                stb.append("and b.source ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and b.source ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and b.source ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("createSource".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "like")) {
                                stb.append("and b.create_source ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and b.create_source ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and b.create_source ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }

                    if ("dept".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if ("like".equals(FilterCodeEnum.getDescByCode(soe.getKey()))) {
                                stb.append("and b.dept_code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if ("in".equals(FilterCodeEnum.getDescByCode(soe.getKey())) || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and b.dept_code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and b.dept_code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }

                    if ("post".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if ("like".equals(FilterCodeEnum.getDescByCode(soe.getKey()))) {
                                stb.append("and b.user_type ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if ("in".equals(FilterCodeEnum.getDescByCode(soe.getKey())) || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and b.user_type ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and b.user_type ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("active".equals(str.getKey())) {
                        activeFlag=false;
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if(Boolean.parseBoolean(soe.getValue().toString())){
                                stb.append("  and ( NOW() BETWEEN a.valid_start_time and a.valid_end_time) ");
                            }else {
                                stb.append("  and  ( NOW() NOT BETWEEN a.valid_start_time and a.valid_end_time)");

                            }
                        }
                    }
                    if ("positionActive".equals(str.getKey())) {
                        positionActiveFlag=false;
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if(Boolean.parseBoolean(soe.getValue().toString())){
                                stb.append("  and ( NOW() BETWEEN b.valid_start_time and b.valid_end_time) ");
                            }else {
                                stb.append("  and  ( NOW() NOT BETWEEN b.valid_start_time and b.valid_end_time)");

                            }
                        }
                    }
                    if ("personCardNo".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "like")) {
                                stb.append("and a.card_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and a.card_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and a.card_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("positionCardNo".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "like")) {
                                stb.append("and b.card_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and b.card_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and b.card_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("startTime".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            //判断是否是区间
                            if ("gt".equals(soe.getKey()) || "lt".equals(soe.getKey())
                                    || "gte".equals(soe.getKey()) || "lte".equals(soe.getKey())) {
                                stb.append("and valid_start_time ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());

                            }
                        }
                    }
                    if ("endTime".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            //判断是否是区间
                            if ("gt".equals(soe.getKey()) || "lt".equals(soe.getKey())
                                    || "gte".equals(soe.getKey()) || "lte".equals(soe.getKey())) {
                                stb.append("and valid_end_time ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());

                            }
                        }
                    }
                    if ("extension".equals(str.getKey())) {
                        List<HashMap<String, Object>> values = (List<HashMap<String, Object>>) str.getValue();
                        if (!CollectionUtils.isEmpty(values)) {
                            List<DynamicAttr> users = dynamicAttrDao.findAllByTypeIGA("IDENTITY", tenantId);
                            Map<String, String> collect = users.stream().collect(Collectors.toMap(DynamicAttr::getCode, DynamicAttr::getId));
                            char aliasOld = 'c';

                            for (HashMap<String, Object> value : values) {
                                String key = (String) value.get("key");
                                if (collect.containsKey(key)) {
                                    HashMap<String, Object> val = (HashMap<String, Object>) value.get("value");
                                    for (Map.Entry<String, Object> soe : val.entrySet()) {
                                        if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("like")) {
                                            char aliasNew = (char) (aliasOld + 1);
                                            sql.append(" LEFT JOIN dynamic_value ").append(aliasNew).append(" ON ").append(aliasNew).append(".entity_id = ").append(" b.id ");
                                            stb.append(" and (").append(aliasNew).append(".value = ? and ").append(aliasNew).append(".attr_id=? )");
                                            aliasOld = aliasNew;
                                            param.add("%" + soe.getValue() + "%");
                                            param.add(collect.get(key));
                                        } else {
                                            char aliasNew = (char) (aliasOld + 1);

                                            sql.append(" LEFT JOIN dynamic_value ").append(aliasNew).append(" ON ").append(aliasNew).append(".entity_id = ").append(" b.id ");
                                            stb.append(" and (").append(aliasNew).append(".value = ? and ").append(aliasNew).append(".attr_id=? )");
                                            aliasOld = aliasNew;
                                            param.add(soe.getValue());
                                            param.add(collect.get(key));
                                        }
                                    }

                                }


                            }
                        }
                    }

                }

            }
        }
        StringBuffer buffer = new StringBuffer(" WHERE 1=1 AND a.TENANT_ID = ? ");
        if (StringUtils.isNotBlank(sql)) {
            stb = sql.append(buffer).append(stb);
        } else {
            stb = buffer.append(stb);
        }
        if(activeFlag){
            stb.append("  and ( NOW() BETWEEN a.valid_start_time and a.valid_end_time) ");
        }
        if(positionActiveFlag){
            stb.append("  and ( NOW() BETWEEN b.valid_start_time and b.valid_end_time) ");
        }
        return stb;
    }


}
