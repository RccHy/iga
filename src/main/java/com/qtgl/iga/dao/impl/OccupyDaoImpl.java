package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bean.OccupyDto;
import com.qtgl.iga.dao.OccupyDao;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@Component
public class OccupyDaoImpl implements OccupyDao {


    @Resource(name = "jdbcSSO")
    JdbcTemplate jdbcSSO;


    @Resource(name = "sso-txTemplate")
    TransactionTemplate txTemplate;


    @Override
    public List<OccupyDto> findAll(String tenantId) {
        // 以人（identity）为主查询 人和身份的关联关系（identity_user）数据。再通过关联数据 查询身份信息详情。过滤掉 关键属性为空的数据行
        String sql = "select" +
                "       iu.identity_id as personId," +
                "       iu.user_id     as occupyId," +
                "       u.user_type               as postCode," +
                "       u.dept_code               as deptCode," +
                "       u.card_type               as identityCardType," +
                "       u.card_no                 as identityCardNo," +
                "       u.del_mark                  as delMark," +
                "       u.start_time                  as startTime," +
                "       u.end_time                  as endTime," +
                "       u.user_index                  as `index`," +
                "       u.source                                ," +
                "       u.data_source               as dataSource," +
                "       u.create_time               as createTime," +
                "       u.update_time               as updateTime" +
                " from identity i" +
                "         left join identity_user iu on i.id = iu.identity_id" +
                "         left join user u on iu.user_id = u.id" +
                " where i.tenant_id = ?" +
                "  and u.dept_code is not null" +
                "  and u.user_type is not null;";


        List<Map<String, Object>> mapList = jdbcSSO.queryForList(sql, tenantId);
        List<OccupyDto> occupies = new ArrayList<>();
        mapList.forEach(map -> {
            OccupyDto occupy = new OccupyDto();
            try {
                BeanUtils.populate(occupy, map);
            } catch (Exception e) {
                e.printStackTrace();
            }
            occupies.add(occupy);
        });
        return occupies;
    }


    @Override
    public Integer saveToSso(Map<String, List<OccupyDto>> occupyMap, String tenantId) {
        return txTemplate.execute(transactionStatus -> {
            try {
                if (occupyMap.containsKey("install")) {
                    List<OccupyDto> list = occupyMap.get("install");
                    String sql = "INSERT INTO user " +
                            "               (id, user_type, card_type, card_no, del_mark, start_time, end_time, create_time, update_time, tenant_id, dept_code, source, data_source, active, active_time,user_index) " +
                            "               VALUES (?,?,?,?,0,?,?,?,?,?,?,?,?,?,?,?);" +
                            "            INSERT INTO identity_user (id, identity_id, user_id) VALUES (?, ?, ?);";
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
                            preparedStatement.setObject(12, "pull");
                            preparedStatement.setObject(13, 1);
                            preparedStatement.setObject(14, LocalDateTime.now());
                            preparedStatement.setObject(15, list.get(i).getIndex());

                            preparedStatement.setObject(16, UUID.randomUUID().toString());
                            preparedStatement.setObject(17, list.get(i).getPersonId());
                            preparedStatement.setObject(18, list.get(i).getOccupyId());

                        }

                        @Override
                        public int getBatchSize() {
                            return list.size();
                        }
                    });
                }

                if (occupyMap.containsKey("update")) {
                    List<OccupyDto> list = occupyMap.get("update");
                    String sql = "UPDATE `user` SET user_type = ?, card_type = ?, card_no = ?, del_mark = ?, start_time = ?, end_time = ?, update_time = ?,dept_code = ?,  " +
                            " source = ?, data_source = 'PULL',  user_index = ?" +
                            " WHERE id = ? and update_time < ? ; ";

                    int[] ints = jdbcSSO.batchUpdate(sql, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, list.get(i).getPostCode());
                            preparedStatement.setObject(2, list.get(i).getIdentityCardType());
                            preparedStatement.setObject(3, list.get(i).getIdentityCardNo());
                            preparedStatement.setObject(4, list.get(i).getDelMark());
                            preparedStatement.setObject(5, list.get(i).getStartTime());
                            preparedStatement.setObject(6, list.get(i).getEndTime());
                            preparedStatement.setObject(7, list.get(i).getEndTime());
                            preparedStatement.setObject(8, list.get(i).getUpdateTime());
                            preparedStatement.setObject(9, list.get(i).getDeptCode());
                            preparedStatement.setObject(10, list.get(i).getSource());
                            preparedStatement.setObject(11, list.get(i).getIndex());
                            preparedStatement.setObject(12, list.get(i).getOccupyId());
                            preparedStatement.setObject(13, list.get(i).getUpdateTime());


                        }

                        @Override
                        public int getBatchSize() {
                            return list.size();
                        }
                    });
                }


                if (occupyMap.containsKey("delete")) {
                    List<OccupyDto> list = occupyMap.get("delete");
                    String sql = "UPDATE `user` SET  del_mark = 1, update_time = ?" +
                            " WHERE id = ? and update_time < ? ; ";
                    int[] ints = jdbcSSO.batchUpdate(sql, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, list.get(i).getPostCode());
                            preparedStatement.setObject(2, list.get(i).getUpdateTime());
                            preparedStatement.setObject(3, list.get(i).getUpdateTime());
                        }

                        @Override
                        public int getBatchSize() {
                            return list.size();
                        }
                    });
                }

                return 1;
            } catch (Exception e) {
                transactionStatus.setRollbackOnly();
                // transactionStatus.rollbackToSavepoint(savepoint);
                return 0;
            }

        });
    }


}
