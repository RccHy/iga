package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.*;
import com.qtgl.iga.dao.PersonDao;
import com.qtgl.iga.utils.FilterCodeEnum;
import com.qtgl.iga.utils.MyBeanUtils;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import org.apache.commons.lang3.StringUtils;
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
import java.util.*;


@Repository
@Component
public class PersonDaoImpl implements PersonDao {


    @Resource(name = "jdbcSSO")
    JdbcTemplate jdbcSSO;
    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;

    @Resource(name = "sso-txTemplate")
    TransactionTemplate txTemplate;


    @Resource(name = "iga-txTemplate")
    TransactionTemplate igaTemplate;

    @Override
    public List<Person> getAll(String tenantId) {
        String sql = " SELECT " +
                " i.id, " +
                " i.NAME as name, " +
                " i.tags, " +
                " i.open_id AS openId, " +
                " i.account_no AS accountNo, " +
                " i.card_type AS cardType, " +
                " i.card_no AS cardNo, " +
                " i.cellphone, " +
                " i.email, " +
                " i.source, " +
                " i.sex, " +
                " i.birthday, " +
                " i.data_source AS dataSource, " +
                " i.active, " +
                " i.active_time AS activeTime, " +
                " i.create_time AS createTime, " +
                " i.update_time AS updateTime, " +
                " i.del_mark AS delMark," +
                " i.valid_start_time AS validStartTime, " +
                " i.valid_end_time AS validEndTime, " +
                " p.`password` AS `password`  " +
                " FROM " +
                " identity i " +
                " LEFT JOIN `password` p ON p.account_id = i.id  " +
                " WHERE " +
                " i.tenant_id = ?  " +
                " AND i.del_mark = 0 " +
                " ORDER BY " +
                " i.update_time";
        List<Map<String, Object>> maps = jdbcSSO.queryForList(sql, tenantId);

        List<Person> personList = new ArrayList<>();

        if (null != maps && maps.size() > 0) {
            maps.forEach(map -> {
                Person person = new Person();
                try {
                    MyBeanUtils.populate(person, map);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                personList.add(person);
            });

        }
        return personList;
    }


    @Override
    public Integer saveToSso(Map<String, List<Person>> personMap, String tenantId, List<DynamicValue> valueUpdate, List<DynamicValue> valueInsert, ArrayList<Certificate> certificates) {

        return txTemplate.execute(transactionStatus -> {
            try {
                if (personMap.containsKey("insert")) {
                    final List<Person> list = personMap.get("insert");


                    String str = "insert into identity (id, `name`, account_no,open_id,  del_mark, create_time, update_time, tenant_id, card_type, card_no, cellphone, email, data_source, tags,  `active`, active_time,`source`,valid_start_time,valid_end_time,freeze_time," +
                            "create_data_source,create_source,birthday)" +
                            " values  (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";


                    int[] ints = jdbcSSO.batchUpdate(str, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, list.get(i).getId());
                            preparedStatement.setObject(2, list.get(i).getName());
                            preparedStatement.setObject(3, list.get(i).getAccountNo());
                            preparedStatement.setObject(4, list.get(i).getOpenId());
                            preparedStatement.setObject(5, list.get(i).getDelMark());
                            preparedStatement.setObject(6, list.get(i).getCreateTime());
                            preparedStatement.setObject(7, list.get(i).getUpdateTime());
                            preparedStatement.setObject(8, tenantId);
                            preparedStatement.setObject(9, list.get(i).getCardType());
                            preparedStatement.setObject(10, list.get(i).getCardNo());
                            preparedStatement.setObject(11, list.get(i).getCellphone());
                            preparedStatement.setObject(12, list.get(i).getEmail());
                            preparedStatement.setObject(13, list.get(i).getDataSource());
                            preparedStatement.setObject(14, list.get(i).getTags());
                            preparedStatement.setObject(15, list.get(i).getActive());
                            preparedStatement.setObject(16, list.get(i).getActiveTime());
                            preparedStatement.setObject(17, list.get(i).getSource());
                            preparedStatement.setObject(18, list.get(i).getValidStartTime());
                            preparedStatement.setObject(19, list.get(i).getValidEndTime());
                            preparedStatement.setObject(20, list.get(i).getFreezeTime());
                            preparedStatement.setObject(21, list.get(i).getDataSource());
                            preparedStatement.setObject(22, list.get(i).getSource());
                            preparedStatement.setObject(23, list.get(i).getBirthday());
                        }

                        @Override
                        public int getBatchSize() {
                            return list.size();
                        }
                    });
                }
                if (personMap.containsKey("update") || personMap.containsKey("invalid")) {
                    String str = "UPDATE identity set  `name`= ?, account_no=?,  del_mark=?, update_time=?, tenant_id=?,  cellphone=?, email=?,  tags=?,  `active`=?, active_time=? ,`source`= ?,data_source=?,valid_start_time=?,valid_end_time=? ," +
                            " card_type =? ,card_no=?,birthday=? where id=? and update_time<= ? ";
                    List<Person> list = new ArrayList<>();
                    List<Person> update = personMap.get("update");
                    List<Person> invalid = personMap.get("invalid");
                    if (null != invalid) {
                        list.addAll(invalid);
                    }
                    if (null != update) {
                        list.addAll(update);
                    }

                    int[] ints = jdbcSSO.batchUpdate(str, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, list.get(i).getName());
                            preparedStatement.setObject(2, list.get(i).getAccountNo());
                            preparedStatement.setObject(3, list.get(i).getDelMark() == null ? 0 : list.get(i).getDelMark());
                            preparedStatement.setObject(4, list.get(i).getUpdateTime());
                            preparedStatement.setObject(5, tenantId);
                            preparedStatement.setObject(6, list.get(i).getCellphone());
                            preparedStatement.setObject(7, list.get(i).getEmail());
                            preparedStatement.setObject(8, list.get(i).getTags());
                            preparedStatement.setObject(9, list.get(i).getActive());
                            preparedStatement.setObject(10, list.get(i).getActiveTime());
                            preparedStatement.setObject(11, list.get(i).getSource());
                            preparedStatement.setObject(12, list.get(i).getDataSource());
                            preparedStatement.setObject(13, list.get(i).getValidStartTime());
                            preparedStatement.setObject(14, list.get(i).getValidEndTime());
                            preparedStatement.setObject(15, list.get(i).getCardType());
                            preparedStatement.setObject(16, list.get(i).getCardNo());
                            preparedStatement.setObject(17, list.get(i).getBirthday());
                            preparedStatement.setObject(18, list.get(i).getId());
                            preparedStatement.setObject(19, list.get(i).getUpdateTime());
                        }

                        @Override
                        public int getBatchSize() {
                            return list.size();
                        }
                    });

                }
                if (personMap.containsKey("delete")) {
                    final List<Person> list = personMap.get("delete");

                    String str = "UPDATE identity set  del_mark= 1 , update_time=now() , data_source=? " +
                            " where id= ?  ";

                    int[] ints = jdbcSSO.batchUpdate(str, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, list.get(i).getDataSource());
                            preparedStatement.setObject(2, list.get(i).getId());
                        }

                        @Override
                        public int getBatchSize() {
                            return list.size();
                        }
                    });

                    /*List<String> ids = personList.stream().map(Person::getId).collect(Collectors.toList());
                    // ???IdentityAccount?????????
                    List<String> accounts = personDao.getAccountByIdentityId(ids);
                    // ?????????????????? ?????? account???
                    personDao.deleteAccount(accounts);
                    // ???IdentityUser?????????
                    List<String> occupies = personDao.getOccupyByIdentityId(ids);
                    // ?????????????????? ?????? account???
                    personDao.deleteOccupy(occupies);*/

                }

                if (personMap.containsKey("password")) {
                    final List<Person> list = personMap.get("password");

                    String str = "INSERT INTO password(id,account_id,password,create_time,update_time,del_mark )" +
                            " SELECT " +
                            " uuid(),?,?,now(),now(),0 " +
                            " FROM DUAL " +
                            " WHERE NOT  EXISTS(select * from password where account_id=?)";


                    int[] ints = jdbcSSO.batchUpdate(str, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, list.get(i).getId());
                            preparedStatement.setObject(2, list.get(i).getPassword());
                            preparedStatement.setObject(3, list.get(i).getId());
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

                if (!CollectionUtils.isEmpty(certificates)) {
                    String certificateStr = "update certificate set card_type=? , card_no =? ,update_time=?  where id= ?";
                    jdbcSSO.batchUpdate(certificateStr, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, certificates.get(i).getCardType());
                            preparedStatement.setObject(2, certificates.get(i).getCardNo());
                            preparedStatement.setObject(3, certificates.get(i).getUpdateTime());
                            preparedStatement.setObject(4, certificates.get(i).getId());
                        }

                        @Override
                        public int getBatchSize() {
                            return certificates.size();
                        }
                    });
                }


                return 1;
            } catch (Exception e) {
                e.printStackTrace();
                transactionStatus.setRollbackOnly();
                // transactionStatus.rollbackToSavepoint(savepoint);
                throw new CustomException(ResultCode.FAILED, "????????????????????????????????????");
            }
        });
    }


    @Override
    public Integer saveToSsoTest(Map<String, List<Person>> personMap, String tenantId, List<DynamicValue> valueUpdate, List<DynamicValue> valueInsert,List<DynamicAttr> attrList, ArrayList<Certificate> certificates) {
        // ???????????????????????????
        String deleteSql = "delete from identity where tenant_id = ? ";
        jdbcIGA.update(deleteSql,tenantId);

        String str = "insert into identity (id, `name`, account_no,open_id,  del_mark, create_time, update_time, tenant_id, card_type, card_no, cellphone, email, data_source, tags,  `active`, active_time,`source`,valid_start_time,valid_end_time,freeze_time," +
                "create_data_source,create_source,sex,birthday,avatar,sync_state)" +
                " values  (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        return igaTemplate.execute(transactionStatus -> {
            try {
                if (personMap.containsKey("keep")) {
                    final List<Person> list = personMap.get("keep");
                    int[] ints = jdbcIGA.batchUpdate(str, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, list.get(i).getId());
                            preparedStatement.setObject(2, list.get(i).getName());
                            preparedStatement.setObject(3, list.get(i).getAccountNo());
                            preparedStatement.setObject(4, list.get(i).getOpenId());
                            preparedStatement.setObject(5, list.get(i).getDelMark());
                            preparedStatement.setObject(6, list.get(i).getCreateTime());
                            preparedStatement.setObject(7, list.get(i).getUpdateTime());
                            preparedStatement.setObject(8, tenantId);
                            preparedStatement.setObject(9, list.get(i).getCardType());
                            preparedStatement.setObject(10, list.get(i).getCardNo());
                            preparedStatement.setObject(11, list.get(i).getCellphone());
                            preparedStatement.setObject(12, list.get(i).getEmail());
                            preparedStatement.setObject(13, list.get(i).getDataSource());
                            preparedStatement.setObject(14, list.get(i).getTags());
                            preparedStatement.setObject(15, list.get(i).getActive());
                            preparedStatement.setObject(16, list.get(i).getActiveTime());
                            preparedStatement.setObject(17, list.get(i).getSource());
                            preparedStatement.setObject(18, list.get(i).getValidStartTime());
                            preparedStatement.setObject(19, list.get(i).getValidEndTime());
                            preparedStatement.setObject(20, list.get(i).getFreezeTime());
                            preparedStatement.setObject(21, list.get(i).getDataSource());
                            preparedStatement.setObject(22, list.get(i).getSource());

                            preparedStatement.setObject(23, list.get(i).getSex());
                            preparedStatement.setObject(24, list.get(i).getBirthday());
                            preparedStatement.setObject(25, list.get(i).getAvatar());
                            preparedStatement.setObject(26, 0);
                        }

                        @Override
                        public int getBatchSize() {
                            return list.size();
                        }
                    });
                }



                if (personMap.containsKey("insert")) {
                    final List<Person> list = personMap.get("insert");
                    int[] ints = jdbcIGA.batchUpdate(str, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, list.get(i).getId());
                            preparedStatement.setObject(2, list.get(i).getName());
                            preparedStatement.setObject(3, list.get(i).getAccountNo());
                            preparedStatement.setObject(4, list.get(i).getOpenId());
                            preparedStatement.setObject(5, list.get(i).getDelMark());
                            preparedStatement.setObject(6, list.get(i).getCreateTime());
                            preparedStatement.setObject(7, list.get(i).getUpdateTime());
                            preparedStatement.setObject(8, tenantId);
                            preparedStatement.setObject(9, list.get(i).getCardType());
                            preparedStatement.setObject(10, list.get(i).getCardNo());
                            preparedStatement.setObject(11, list.get(i).getCellphone());
                            preparedStatement.setObject(12, list.get(i).getEmail());
                            preparedStatement.setObject(13, list.get(i).getDataSource());
                            preparedStatement.setObject(14, list.get(i).getTags());
                            preparedStatement.setObject(15, list.get(i).getActive());
                            preparedStatement.setObject(16, list.get(i).getActiveTime());
                            preparedStatement.setObject(17, list.get(i).getSource());
                            preparedStatement.setObject(18, list.get(i).getValidStartTime());
                            preparedStatement.setObject(19, list.get(i).getValidEndTime());
                            preparedStatement.setObject(20, list.get(i).getFreezeTime());
                            preparedStatement.setObject(21, list.get(i).getDataSource());
                            preparedStatement.setObject(22, list.get(i).getSource());
                            preparedStatement.setObject(23, list.get(i).getSex());
                            preparedStatement.setObject(24, list.get(i).getBirthday());
                            preparedStatement.setObject(25, list.get(i).getAvatar());
                            preparedStatement.setObject(26, 1);
                        }

                        @Override
                        public int getBatchSize() {
                            return list.size();
                        }
                    });
                }
                if (personMap.containsKey("update")) {
                    final List<Person> list = personMap.get("update");
                    int[] ints = jdbcIGA.batchUpdate(str, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, list.get(i).getId());
                            preparedStatement.setObject(2, list.get(i).getName());
                            preparedStatement.setObject(3, list.get(i).getAccountNo());
                            preparedStatement.setObject(4, list.get(i).getOpenId());
                            preparedStatement.setObject(5, list.get(i).getDelMark());
                            preparedStatement.setObject(6, list.get(i).getCreateTime());
                            preparedStatement.setObject(7, LocalDateTime.now());
                            preparedStatement.setObject(8, tenantId);
                            preparedStatement.setObject(9, list.get(i).getCardType());
                            preparedStatement.setObject(10, list.get(i).getCardNo());
                            preparedStatement.setObject(11, list.get(i).getCellphone());
                            preparedStatement.setObject(12, list.get(i).getEmail());
                            preparedStatement.setObject(13, list.get(i).getDataSource());
                            preparedStatement.setObject(14, list.get(i).getTags());
                            preparedStatement.setObject(15, list.get(i).getActive());
                            preparedStatement.setObject(16, list.get(i).getActiveTime());
                            preparedStatement.setObject(17, list.get(i).getSource());
                            preparedStatement.setObject(18, list.get(i).getValidStartTime());
                            preparedStatement.setObject(19, list.get(i).getValidEndTime());
                            preparedStatement.setObject(20, list.get(i).getFreezeTime());
                            preparedStatement.setObject(21, list.get(i).getDataSource());
                            preparedStatement.setObject(22, list.get(i).getSource());
                            preparedStatement.setObject(23, list.get(i).getSex());
                            preparedStatement.setObject(24, list.get(i).getBirthday());
                            preparedStatement.setObject(25, list.get(i).getAvatar());
                            preparedStatement.setObject(26, 3);
                        }

                        @Override
                        public int getBatchSize() {
                            return list.size();
                        }
                    });
                }

                if (personMap.containsKey("delete")) {
                    final List<Person> list = personMap.get("delete");
                    int[] ints = jdbcIGA.batchUpdate(str, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, list.get(i).getId());
                            preparedStatement.setObject(2, list.get(i).getName());
                            preparedStatement.setObject(3, list.get(i).getAccountNo());
                            preparedStatement.setObject(4, list.get(i).getOpenId());
                            preparedStatement.setObject(5, 1);
                            preparedStatement.setObject(6, list.get(i).getCreateTime());
                            preparedStatement.setObject(7, LocalDateTime.now());
                            preparedStatement.setObject(8, tenantId);
                            preparedStatement.setObject(9, list.get(i).getCardType());
                            preparedStatement.setObject(10, list.get(i).getCardNo());
                            preparedStatement.setObject(11, list.get(i).getCellphone());
                            preparedStatement.setObject(12, list.get(i).getEmail());
                            preparedStatement.setObject(13, list.get(i).getDataSource());
                            preparedStatement.setObject(14, list.get(i).getTags());
                            preparedStatement.setObject(15, list.get(i).getActive());
                            preparedStatement.setObject(16, list.get(i).getActiveTime());
                            preparedStatement.setObject(17, list.get(i).getSource());
                            preparedStatement.setObject(18, list.get(i).getValidStartTime());
                            preparedStatement.setObject(19, list.get(i).getValidEndTime());
                            preparedStatement.setObject(20, list.get(i).getFreezeTime());
                            preparedStatement.setObject(21, list.get(i).getDataSource());
                            preparedStatement.setObject(22, list.get(i).getSource());
                            preparedStatement.setObject(23, list.get(i).getSex());
                            preparedStatement.setObject(24, list.get(i).getBirthday());
                            preparedStatement.setObject(25, list.get(i).getAvatar());
                            preparedStatement.setObject(26, 2);
                        }

                        @Override
                        public int getBatchSize() {
                            return list.size();
                        }
                    });

                }


                if ( personMap.containsKey("invalid")) {
                    final List<Person> list = personMap.get("invalid");
                    int[] ints = jdbcIGA.batchUpdate(str, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, list.get(i).getId());
                            preparedStatement.setObject(2, list.get(i).getName());
                            preparedStatement.setObject(3, list.get(i).getAccountNo());
                            preparedStatement.setObject(4, list.get(i).getOpenId());
                            preparedStatement.setObject(5, list.get(i).getDelMark());
                            preparedStatement.setObject(6, list.get(i).getCreateTime());
                            preparedStatement.setObject(7, LocalDateTime.now());
                            preparedStatement.setObject(8, tenantId);
                            preparedStatement.setObject(9, list.get(i).getCardType());
                            preparedStatement.setObject(10, list.get(i).getCardNo());
                            preparedStatement.setObject(11, list.get(i).getCellphone());
                            preparedStatement.setObject(12, list.get(i).getEmail());
                            preparedStatement.setObject(13, list.get(i).getDataSource());
                            preparedStatement.setObject(14, list.get(i).getTags());
                            preparedStatement.setObject(15, list.get(i).getActive());
                            preparedStatement.setObject(16, list.get(i).getActiveTime());
                            preparedStatement.setObject(17, list.get(i).getSource());
                            preparedStatement.setObject(18, list.get(i).getValidStartTime());
                            preparedStatement.setObject(19, list.get(i).getValidEndTime());
                            preparedStatement.setObject(20, list.get(i).getFreezeTime());
                            preparedStatement.setObject(21, list.get(i).getDataSource());
                            preparedStatement.setObject(22, list.get(i).getSource());
                            preparedStatement.setObject(23, list.get(i).getSex());
                            preparedStatement.setObject(24, list.get(i).getBirthday());
                            preparedStatement.setObject(25, list.get(i).getAvatar());
                            preparedStatement.setObject(26, 4);
                        }

                        @Override
                        public int getBatchSize() {
                            return list.size();
                        }
                    });
                }






               /* if (personMap.containsKey("password")) {
                    final List<Person> list = personMap.get("password");

                    String str = "INSERT INTO password(id,account_id,password,create_time,update_time,del_mark )" +
                            " SELECT " +
                            " uuid(),?,?,now(),now(),0 " +
                            " FROM DUAL " +
                            " WHERE NOT  EXISTS(select * from password where account_id=?)";


                    int[] ints = jdbcIGA.batchUpdate(str, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, list.get(i).getId());
                            preparedStatement.setObject(2, list.get(i).getPassword());
                            preparedStatement.setObject(3, list.get(i).getId());
                        }

                        @Override
                        public int getBatchSize() {
                            return list.size();
                        }
                    });
                }*/




                List<DynamicValue> dynamicValues = new ArrayList<>();
                if (!CollectionUtils.isEmpty(valueInsert)) {
                    dynamicValues.addAll(valueInsert);
                }
                if (!CollectionUtils.isEmpty(valueUpdate)) {
                    dynamicValues.addAll(valueUpdate);
                }


                if (!CollectionUtils.isEmpty(dynamicValues)) {

                    // ????????????????????????????????????
                    String deleteDynamicAttrSql = "delete from dynamic_attr where   type='USER' and tenant_id = ?";
                    jdbcIGA.update(deleteDynamicAttrSql, new Object[]{new String(tenantId)});

                    String deleteDynamicValueSql = "delete from dynamic_value where  tenant_id = ? and attr_id not in (select id from dynamic_attr )";
                    jdbcIGA.update(deleteDynamicValueSql, new Object[]{new String(tenantId)});

                    String addDynamicValueSql = "INSERT INTO dynamic_attr (id, name, code, required, description, tenant_id, create_time, update_time, type, field_type, format, is_search, attr_index) VALUES (?,?,?,?,?,?,?,?,'USER',?,?,?,?)";
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

                if (!CollectionUtils.isEmpty(certificates)) {
                    String certificateStr = "update certificate set card_type=? , card_no =? ,update_time=?  where id= ?";
                    jdbcIGA.batchUpdate(certificateStr, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, certificates.get(i).getCardType());
                            preparedStatement.setObject(2, certificates.get(i).getCardNo());
                            preparedStatement.setObject(3, certificates.get(i).getUpdateTime());
                            preparedStatement.setObject(4, certificates.get(i).getId());
                        }

                        @Override
                        public int getBatchSize() {
                            return certificates.size();
                        }
                    });
                }


                return 1;
            } catch (Exception e) {
                e.printStackTrace();
                transactionStatus.setRollbackOnly();
                // transactionStatus.rollbackToSavepoint(savepoint);
                throw new CustomException(ResultCode.FAILED, "????????????????????????????????????");
            }
        });
    }


    @Override
    public Integer saveToTemp(List<Person> personList, DomainInfo domainInfo) {
        String str = "insert into person_temp (id, `name`, account_no,open_id,  del_mark, create_time, update_time, tenant_id, card_type, card_no, cellphone, email, data_source, tags,  `active`, active_time,`source`,valid_start_time,valid_end_time,freeze_time)" +
                " values  (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";


        int[] ints = jdbcIGA.batchUpdate(str, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                preparedStatement.setObject(1, personList.get(i).getId());
                preparedStatement.setObject(2, personList.get(i).getName());
                preparedStatement.setObject(3, personList.get(i).getAccountNo());
                preparedStatement.setObject(4, personList.get(i).getOpenId());
                preparedStatement.setObject(5, personList.get(i).getDelMark());
                preparedStatement.setObject(6, personList.get(i).getCreateTime());
                preparedStatement.setObject(7, personList.get(i).getUpdateTime());
                preparedStatement.setObject(8, domainInfo.getId());
                preparedStatement.setObject(9, personList.get(i).getCardType());
                preparedStatement.setObject(10, personList.get(i).getCardNo());
                preparedStatement.setObject(11, personList.get(i).getCellphone());
                preparedStatement.setObject(12, personList.get(i).getEmail());
                preparedStatement.setObject(13, personList.get(i).getDataSource());
                preparedStatement.setObject(14, personList.get(i).getTags());
                preparedStatement.setObject(15, personList.get(i).getActive());
                preparedStatement.setObject(16, personList.get(i).getActiveTime());
                preparedStatement.setObject(17, personList.get(i).getSource());
                preparedStatement.setObject(18, personList.get(i).getValidStartTime());
                preparedStatement.setObject(19, personList.get(i).getValidEndTime());
                preparedStatement.setObject(20, personList.get(i).getFreezeTime());
            }

            @Override
            public int getBatchSize() {
                return personList.size();
            }
        });
        return null;
    }

    @Override
    public List<Person> findPersonTemp(Map<String, Object> arguments, DomainInfo domain) {
        String sql = " SELECT id from person_temp where tenant_id=? ";
        //??????sql
        StringBuffer stb = new StringBuffer(sql);
        //????????????
        List<Object> param = new ArrayList<>();
        param.add(domain.getId());
        dealData(arguments, stb, param);
        stb.append(" order by create_time desc ");
        //????????????
        if (null != arguments.get("offset") && null != arguments.get("first")) {

            stb.append(" limit ?,? ");
            param.add(null == arguments.get("offset") ? 0 : arguments.get("offset"));
            param.add(null == arguments.get("first") ? 0 : arguments.get("first"));
        }
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(stb.toString(), param.toArray());
        ArrayList<Person> list = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {
                Person person = new Person();
                BeanMap beanMap = BeanMap.create(person);
                beanMap.putAll(map);
                list.add(person);
            }
            return list;
        }
        return null;
    }

    @Override
    public void removeData(DomainInfo domain) {
        String sql = " delete from  `person_temp` where tenant_id =? ";
        jdbcIGA.update(sql, domain.getId());
    }

    @Override
    public Integer findPersonTempCount(Map<String, Object> arguments, DomainInfo domain) {
        String sql = " SELECT count(*) from person_temp where tenant_id=?  ";
        //??????sql
        StringBuffer stb = new StringBuffer(sql);
        List<Object> param = new ArrayList<>();
        param.add(domain.getId());
        if (null != arguments) {
            dealData(arguments, stb, param);
        }
        Integer integer = jdbcIGA.queryForObject(stb.toString(), param.toArray(), Integer.class);
        return integer;
    }

    @Override
    public List<Person> findDistinctPerson(String tenantId) {
        String sql = " SELECT " +
                "i.id, " +
                " i.NAME as name, " +
                " i.tags, " +
                " i.open_id AS openId, " +
                " i.account_no AS accountNo, " +
                " i.card_type AS cardType, " +
                " i.card_no AS cardNo, " +
                " i.cellphone, " +
                " i.email, " +
                " i.source, " +
                " i.sex, " +
                " i.birthday, " +
                " i.data_source AS dataSource, " +
                " i.active, " +
                " i.active_time AS activeTime, " +
                " i.create_time AS createTime, " +
                " i.update_time AS updateTime, " +
                " i.del_mark AS delMark," +
                " i.valid_start_time AS validStartTime, " +
                " i.valid_end_time AS validEndTime " +
                " FROM " +
                " identity i " +
                " INNER JOIN `certificate` c ON c.from_identity_id = i.id  " +
                " WHERE " +
                " i.tenant_id = ?  " +
                " AND c.del_mark = 0  " +
                " AND i.del_mark = 1";
        List<Map<String, Object>> maps = jdbcSSO.queryForList(sql, tenantId);

        List<Person> personList = new ArrayList<>();

        if (null != maps && maps.size() > 0) {
            maps.forEach(map -> {
                Person person = new Person();
                try {
                    MyBeanUtils.populate(person, map);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                personList.add(person);
            });

        }
        return personList;
    }

    @Override
    public List<Certificate> getAllCard(String tenantId) {
        String sql = "SELECT " +
                " c.id, " +
                " c.card_type AS cardType, " +
                " c.identity_id AS identityId,  " +
                " c.card_no AS cardNo, " +
                " c.source, " +
                " c.data_source AS dataSource, " +
                " c.active, " +
                " c.active_time AS activeTime, " +
                " c.create_time AS createTime, " +
                " c.update_time AS updateTime, " +
                " c.del_mark AS delMark," +
                " c.from_identity_id AS fromIdentityId " +
                " FROM " +
                " certificate c " +
                " LEFT JOIN identity i ON c.identity_id = i.id " +
                " WHERE " +
                " c.from_identity_id IS NOT NULL  " +
                " AND i.tenant_id = ?  " +
                " AND i.del_mark = 0  " +
                " ORDER BY " +
                " c.update_time";
        List<Map<String, Object>> maps = jdbcSSO.queryForList(sql, tenantId);

        List<Certificate> certificates = new ArrayList<>();

        if (null != maps && maps.size() > 0) {
            maps.forEach(map -> {
                Certificate certificate = new Certificate();
                try {
                    MyBeanUtils.populate(certificate, map);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                certificates.add(certificate);
            });

        }
        return certificates;
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
                    if ("phone".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if ("like".equals(FilterCodeEnum.getDescByCode(soe.getKey()))) {
                                stb.append("and cellphone ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if ("in".equals(FilterCodeEnum.getDescByCode(soe.getKey())) || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and cellphone ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and cellphone ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
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
                    if ("cardNo".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "like")) {
                                stb.append("and card_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and card_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and card_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("email".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("like")) {
                                stb.append("and email ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and email ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and email ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }

                }

            }
        }
    }

    @Override
    public List<Person> mergeCharacteristicPerson(String tenantId) {
        String sql = "select c.identity_id as id,i.open_id as openid,c.card_type,c.card_no " +
                " from  certificate as c " +
                " inner join identity i on c.identity_id = i.id " +
                " where c.active=1 and i.tenant_id=? ";

        List<Map<String, Object>> maps = jdbcSSO.queryForList(sql, tenantId);
        List<Person> personList = new ArrayList<>();
        for (Map<String, Object> map : maps) {
            Person person = new Person();
            person.setId(map.get("id").toString());
            person.setOpenId(map.get("openid").toString());
            if (map.get("card_type").toString().equals("CELLPHONE")) {
                person.setCellphone(map.get("card_no").toString());
                continue;
            }
            if (map.get("card_type").toString().equals("EMAIL")) {
                person.setEmail(map.get("card_no").toString());
                continue;
            }
            if (map.get("card_type").toString().equals("USERNAME")) {
                person.setAccountNo(map.get("card_no").toString());
                continue;
            }
            if (map.get("card_type").toString().equals("CARD_NO")) {
                person.setCardNo(map.get("card_no").toString());
                continue;
            }
            if (StringUtils.isNotBlank(map.get("card_type").toString()) && StringUtils.isNotBlank(map.get("card_no").toString())) {
                person.setCardType(map.get("card_type").toString());
                person.setCardNo(map.get("card_no").toString());
            }
            personList.add(person);

        }
        return personList;
    }

    @Override
    public List<Person> getDelMarkPeople(String tenantId) {
        String sql = " SELECT " +
                " i.id, " +
                " i.NAME as name, " +
                " i.tags, " +
                " i.open_id AS openId, " +
                " i.account_no AS accountNo, " +
                " i.card_type AS cardType, " +
                " i.card_no AS cardNo, " +
                " i.cellphone, " +
                " i.email, " +
                " i.source, " +
                " i.data_source AS dataSource, " +
                " i.active, " +
                " i.active_time AS activeTime, " +
                " i.create_time AS createTime, " +
                " i.update_time AS updateTime, " +
                " i.del_mark AS delMark," +
                " i.valid_start_time AS validStartTime, " +
                " i.valid_end_time AS validEndTime, " +
                " p.`password` AS `password`  " +
                " FROM " +
                " identity i " +
                " LEFT JOIN `password` p ON p.account_id = i.id  " +
                " WHERE " +
                " i.tenant_id = ?  " +
                " AND i.del_mark = 1 " +
                " ORDER BY " +
                " i.update_time";
        List<Map<String, Object>> maps = jdbcSSO.queryForList(sql, tenantId);

        List<Person> personList = new ArrayList<>();

        if (null != maps && maps.size() > 0) {
            maps.forEach(map -> {
                Person person = new Person();
                try {
                    MyBeanUtils.populate(person, map);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                personList.add(person);
            });

        }
        return personList;
    }


}
