package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.Avatar;
import com.qtgl.iga.dao.AvatarDao;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

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
@Slf4j
public class AvatarDaoImpl implements AvatarDao {


    @Resource(name = "jdbcSSO")
    JdbcTemplate jdbcSSO;
    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;

    @Resource(name = "sso-txTemplate")
    TransactionTemplate txTemplate;


    @Resource(name = "iga-txTemplate")
    TransactionTemplate igaTemplate;

    @Override
    public List<Avatar> findAll(String tenantId) {

        String sql = " SELECT " +
                " a.id, " +
                " a.avatar , " +
                " a.avatar_url as avatarUrl , " +
                " a.avatar_hash_code AS avatarHashCode, " +
                " a.avatar_update_time AS avatarUpdateTime, " +
                " a.identity_id AS identityId, " +
                " a.tenant_id AS tenantId " +
                " FROM " +
                " avatar a " +
                " WHERE " +
                " a.tenant_id =?  ";
        List<Map<String, Object>> maps = jdbcSSO.queryForList(sql, tenantId);

        List<Avatar> avatarList = new ArrayList<>();

        if (!CollectionUtils.isEmpty(maps)) {
            maps.forEach(map -> {
                Avatar avatar = new Avatar();
                BeanMap beanMap = BeanMap.create(avatar);
                beanMap.putAll(map);
                avatarList.add(avatar);
            });

        }
        return avatarList;
    }

    @Override
    public Integer saveToSso(Map<String, List<Avatar>> avatarResult, String tenantId) {

        return txTemplate.execute(transactionStatus -> {
            try {
                //LocalDateTime now = LocalDateTime.now();
                //if (avatarResult.containsKey("insert")) {
                //    final List<Avatar> list = avatarResult.get("insert");
                //    if (!CollectionUtils.isEmpty(list)) {
                //        String str = "INSERT INTO `avatar`(`id`, `avatar`, `avatar_url`, `avatar_hash_code`, `avatar_update_time`, `identity_id`, `tenant_id`) VALUES (?,?,?,?,?,?,?)";
                //
                //        // 对 list 分批执行，每批次5000条,不足5000按足量执行
                //        int batchSize = 5000;
                //        int batchCount = (list.size() + batchSize - 1) / batchSize;
                //        for (int i = 0; i < batchCount; i++) {
                //            int fromIndex = i * batchSize;
                //            int toIndex = Math.min((i + 1) * batchSize, list.size());
                //            log.info("from:" + fromIndex + "to:" + toIndex);
                //            List<Avatar> subList = list.subList(fromIndex, toIndex);
                //            int[] ints = jdbcSSO.batchUpdate(str, new BatchPreparedStatementSetter() {
                //                @Override
                //                public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                //                    preparedStatement.setObject(1, subList.get(i).getId());
                //                    preparedStatement.setObject(2, subList.get(i).getAvatar());
                //                    preparedStatement.setObject(3, subList.get(i).getAvatarUrl());
                //                    preparedStatement.setObject(4, subList.get(i).getAvatarHashCode());
                //                    preparedStatement.setObject(5, now);
                //                    preparedStatement.setObject(6, subList.get(i).getIdentityId());
                //                    preparedStatement.setObject(7, tenantId);
                //                }
                //
                //                @Override
                //                public int getBatchSize() {
                //                    return subList.size();
                //                }
                //            });
                //        }
                //    }
                //
                //}
                //if (avatarResult.containsKey("update")) {
                //    String str = "UPDATE avatar set  `avatar`= ?, avatar_url=?,  avatar_hash_code=?, avatar_update_time=?, identity_id=? where id=? and tenant_id = ? ";
                //    List<Avatar> update = avatarResult.get("update");
                //    if (!CollectionUtils.isEmpty(update)) {
                //
                //        // 对 list 分批执行，每批次5000条,不足5000按足量执行
                //        int batchSize = 5000;
                //        int batchCount = (update.size() + batchSize - 1) / batchSize;
                //        for (int i = 0; i < batchCount; i++) {
                //            int fromIndex = i * batchSize;
                //            int toIndex = Math.min((i + 1) * batchSize, update.size());
                //            log.info("from:" + fromIndex + "to:" + toIndex);
                //            List<Avatar> subList = update.subList(fromIndex, toIndex);
                //
                //            int[] ints = jdbcSSO.batchUpdate(str, new BatchPreparedStatementSetter() {
                //                @Override
                //                public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                //                    preparedStatement.setObject(1, subList.get(i).getAvatar());
                //                    preparedStatement.setObject(2, subList.get(i).getAvatarUrl());
                //                    preparedStatement.setObject(3, subList.get(i).getAvatarHashCode());
                //                    preparedStatement.setObject(4, now);
                //                    preparedStatement.setObject(5, subList.get(i).getIdentityId());
                //                    preparedStatement.setObject(6, subList.get(i).getId());
                //                    preparedStatement.setObject(7, tenantId);
                //                }
                //
                //                @Override
                //                public int getBatchSize() {
                //                    return subList.size();
                //                }
                //            });
                //
                //        }
                //    }
                //
                //}
                if (avatarResult.containsKey("delete")) {
                    String str = "delete from avatar  where id=? and tenant_id = ? ";
                    List<Avatar> list = new ArrayList<>();
                    List<Avatar> update = avatarResult.get("delete");
                    if (null != update) {
                        list.addAll(update);
                    }
                    // 对 list 分批执行，每批次5000条,不足5000按足量执行
                    int batchSize = 5000;
                    int batchCount = (list.size() + batchSize - 1) / batchSize;
                    for (int i = 0; i < batchCount; i++) {
                        int fromIndex = i * batchSize;
                        int toIndex = Math.min((i + 1) * batchSize, list.size());
                        log.info("from:" + fromIndex + "to:" + toIndex);
                        List<Avatar> subList = list.subList(fromIndex, toIndex);

                        int[] ints = jdbcSSO.batchUpdate(str, new BatchPreparedStatementSetter() {
                            @Override
                            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                                preparedStatement.setObject(1, subList.get(i).getId());
                                preparedStatement.setObject(2, tenantId);
                            }

                            @Override
                            public int getBatchSize() {
                                return subList.size();
                            }
                        });

                    }

                }
                return 1;
            } catch (Exception e) {
                e.printStackTrace();
                transactionStatus.setRollbackOnly();
                // transactionStatus.rollbackToSavepoint(savepoint);
                throw new CustomException(ResultCode.FAILED, "同步失败，头像同步异常！");
            }
        });


    }

    @Override
    public Integer saveToSso(List<Avatar> avatarList, String tenantId) {
        return txTemplate.execute(transactionStatus -> {
            try {
                LocalDateTime now = LocalDateTime.now();
                if (!CollectionUtils.isEmpty(avatarList)) {
                    String str = "INSERT INTO `avatar`(`id`, `avatar`, `avatar_url`, `avatar_hash_code`, `avatar_update_time`, `identity_id`, `tenant_id`) VALUES (?,?,?,?,?,?,?)" +
                            " ON DUPLICATE KEY UPDATE " +
                            "         id =VALUES(id) ,avatar = VALUES(avatar),avatar_url =VALUES(avatar_url),avatar_hash_code =VALUES(avatar_hash_code),  " +
                            "         avatar_update_time = VALUES(avatar_update_time),identity_id = VALUES(identity_id),tenant_id= VALUES(tenant_id)";

                    int[] ints = jdbcSSO.batchUpdate(str, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, null == avatarList.get(i).getId() ? UUID.randomUUID().toString() : avatarList.get(i).getId());
                            preparedStatement.setObject(2, avatarList.get(i).getAvatar());
                            preparedStatement.setObject(3, avatarList.get(i).getAvatarUrl());
                            preparedStatement.setObject(4, avatarList.get(i).getAvatarHashCode());
                            preparedStatement.setObject(5, now);
                            preparedStatement.setObject(6, avatarList.get(i).getIdentityId());
                            preparedStatement.setObject(7, tenantId);
                        }

                        @Override
                        public int getBatchSize() {
                            return avatarList.size();
                        }
                    });

                }

                return 1;
            } catch (Exception e) {
                e.printStackTrace();
                transactionStatus.setRollbackOnly();
                // transactionStatus.rollbackToSavepoint(savepoint);
                throw new CustomException(ResultCode.FAILED, "同步失败，头像同步异常！");
            }
        });
    }
}
