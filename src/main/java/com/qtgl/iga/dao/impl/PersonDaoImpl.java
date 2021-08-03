package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.Person;
import com.qtgl.iga.dao.PersonDao;
import com.qtgl.iga.utils.MyBeanUtils;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


@Repository
@Component
public class PersonDaoImpl implements PersonDao {


    @Resource(name = "jdbcSSO")
    JdbcTemplate jdbcSSO;

    @Resource(name = "sso-txTemplate")
    TransactionTemplate txTemplate;

    @Override
    public List<Person> getAll(String tenantId) {
        String sql = "select id,name,tags,open_id as openId,account_no as accountNo,card_type as cardType," +
                "card_no  as cardNo, cellphone,email,source,data_source as dataSource,active," +
                "active_time as activeTime,create_time as createTime,update_time as updateTime,del_mark as delMark,valid_start_time as validStartTime , valid_end_time as validEndTime " +
                "  from identity  where tenant_id=? and del_mark=0 order by update_time";
        List<Map<String, Object>> maps = jdbcSSO.queryForList(sql, tenantId);
        List<Person> personList = new ArrayList<>();
        maps.forEach(map -> {
            Person person = new Person();
            try {
                MyBeanUtils.populate(person, map);
            } catch (Exception e) {
                e.printStackTrace();
            }
            personList.add(person);
        });
        return personList;
    }


    @Override
    public List<Person> savePerson(List<Person> list, String tenantId) {
        String str = "insert into identity (id, name, account_no,open_id,  del_mark, create_time, update_time, tenant_id, card_type, card_no, cellphone, email, data_source, tags,  active, active_time,source)" +
                "values  (?, ?, ?,?,?,now(),now(),?,?,?,?,?,?,?,?,?);";
        boolean contains = false;
        int[] ints = jdbcSSO.batchUpdate(str, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                preparedStatement.setObject(1, UUID.randomUUID().toString().replace("-", ""));
                preparedStatement.setObject(2, list.get(i).getName());
                preparedStatement.setObject(3, list.get(i).getAccountNo());
                preparedStatement.setObject(4, list.get(i).getOpenId());
                preparedStatement.setObject(3, list.get(i).getDelMark() == null ? 0 : list.get(i).getDelMark());
                preparedStatement.setObject(6, tenantId);
                preparedStatement.setObject(7, list.get(i).getCardType());
                preparedStatement.setObject(8, list.get(i).getCardNo());
                preparedStatement.setObject(9, list.get(i).getCellphone());
                preparedStatement.setObject(10, list.get(i).getEmail());
                preparedStatement.setObject(11, list.get(i).getDataSource());
                preparedStatement.setObject(12, list.get(i).getTags());
                preparedStatement.setObject(13, list.get(i).getActive());
                preparedStatement.setObject(14, list.get(i).getActiveTime());
                preparedStatement.setObject(15, "PULL");
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
    public List<Person> updatePerson(List<Person> list, String tenantId) {
        String str = "UPDATE identity set  name= ?, account_no=?,  del_mark=?, update_time=now(), tenant_id=?,  cellphone=?, email=?, data_source=?, tags=?,  active=?, active_time=? ,source= ?,data_source=?" +
                " where card_type=? and  card_no= ? and update_time< ? and tenant_id=?";
        boolean contains = false;
        int[] ints = jdbcSSO.batchUpdate(str, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                preparedStatement.setObject(1, list.get(i).getName());
                preparedStatement.setObject(2, list.get(i).getAccountNo());
                preparedStatement.setObject(3, list.get(i).getDelMark() == null ? 0 : list.get(i).getDelMark());
                preparedStatement.setObject(4, tenantId);
                preparedStatement.setObject(5, list.get(i).getCellphone());
                preparedStatement.setObject(6, list.get(i).getEmail());
                preparedStatement.setObject(7, list.get(i).getDataSource());
                preparedStatement.setObject(8, list.get(i).getTags());
                preparedStatement.setObject(9, list.get(i).getActive());
                preparedStatement.setObject(10, list.get(i).getActiveTime());
                preparedStatement.setObject(11, "PULL");
                preparedStatement.setObject(12, list.get(i).getDataSource());
                preparedStatement.setObject(13, list.get(i).getCardType());
                preparedStatement.setObject(14, list.get(i).getCardNo());
                preparedStatement.setObject(15, list.get(i).getUpdateTime());
                preparedStatement.setObject(16, tenantId);
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
    public List<Person> deletePerson(List<Person> list, String tenantId) {
        String str = "UPDATE identity set  del_mark= 1 and update_time=now()" +
                " where id=?  and tenant_id=?";
        boolean contains = false;
        int[] ints = jdbcSSO.batchUpdate(str, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                preparedStatement.setObject(1, list.get(i).getCardType());
                preparedStatement.setObject(2, list.get(i).getCardNo());
                preparedStatement.setObject(3, list.get(i).getUpdateTime());
                preparedStatement.setObject(4, tenantId);
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
    public Integer saveToSso(Map<String, List<Person>> personMap, String tenantId) {

        return txTemplate.execute(transactionStatus -> {
            try {
                if (personMap.containsKey("insert")) {
                    final List<Person> list = personMap.get("insert");


                    String str = "insert into identity (id, `name`, account_no,open_id,  del_mark, create_time, update_time, tenant_id, card_type, card_no, cellphone, email, data_source, tags,  `active`, active_time,`source`,valid_start_time,valid_end_time)" +
                            " values  (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";


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
                            preparedStatement.setObject(17, "PULL");
                            preparedStatement.setObject(18, list.get(i).getValidStartTime());
                            preparedStatement.setObject(19, list.get(i).getValidEndTime());
                        }

                        @Override
                        public int getBatchSize() {
                            return list.size();
                        }
                    });
                }
                if (personMap.containsKey("update")) {
//                    final List<Person> list = personMap.get("update");
                    String str = "UPDATE identity set  `name`= ?, account_no=?,  del_mark=?, update_time=?, tenant_id=?,  cellphone=?, email=?, data_source=?, tags=?,  `active`=?, active_time=? ,`source`= ?,data_source=?,valid_start_time=?,valid_end_time=? " +
                            " where id=? and update_time< ? ";
                    List<Person> list = new ArrayList<>();
                    List<Person> update = personMap.get("update");
                    List<Person> invalid = personMap.get("invalid");
//                    List<OccupyDto> recover = occupyMap.get("recover");
                    if (null != update) {
                        list.addAll(update);
                    }
                    if (null != invalid) {
                        list.addAll(invalid);
                    }
//                    if (null != recover) {
//                        list.addAll(recover);
//                    }
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
                            preparedStatement.setObject(8, list.get(i).getDataSource());
                            preparedStatement.setObject(9, list.get(i).getTags());
                            preparedStatement.setObject(10, list.get(i).getActive());
                            preparedStatement.setObject(11, list.get(i).getActiveTime());
                            preparedStatement.setObject(12, "PULL");
                            preparedStatement.setObject(13, list.get(i).getDataSource());
                            preparedStatement.setObject(14, list.get(i).getValidStartTime());
                            preparedStatement.setObject(15, list.get(i).getValidEndTime());
                            preparedStatement.setObject(16, list.get(i).getId());
                            preparedStatement.setObject(17, list.get(i).getUpdateTime());
                        }

                        @Override
                        public int getBatchSize() {
                            return list.size();
                        }
                    });

                }
                if (personMap.containsKey("delete")) {
                    final List<Person> list = personMap.get("delete");

                    String str = "UPDATE identity set  del_mark= 1 and update_time=now()" +
                            " where id=?  ";

                    int[] ints = jdbcSSO.batchUpdate(str, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                            preparedStatement.setObject(1, list.get(i).getCardType());
                            preparedStatement.setObject(2, list.get(i).getCardNo());
                            preparedStatement.setObject(3, list.get(i).getUpdateTime());

                        }

                        @Override
                        public int getBatchSize() {
                            return list.size();
                        }
                    });

                    /*List<String> ids = personList.stream().map(Person::getId).collect(Collectors.toList());
                    // 查IdentityAccount关联表
                    List<String> accounts = personDao.getAccountByIdentityId(ids);
                    // 根据关联关系 删除 account表
                    personDao.deleteAccount(accounts);
                    // 查IdentityUser关联表
                    List<String> occupies = personDao.getOccupyByIdentityId(ids);
                    // 根据关联关系 删除 account表
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

                return 1;
            } catch (Exception e) {
                e.printStackTrace();
                transactionStatus.setRollbackOnly();
                // transactionStatus.rollbackToSavepoint(savepoint);
                throw new CustomException(ResultCode.FAILED, "同步终止，人员同步异常！");
            }
        });


    }

    @Override
    public List<Person> findActiveDataByTenantId(String tenantId) {
        String sql = "select id,name,tags,open_id as openId,account_no as accountNo,card_type as cardType," +
                "card_no  as cardNo, cellphone,email,source,data_source as dataSource,active," +
                "active_time as activeTime,create_time as createTime,update_time as updateTime,del_mark as delMark" +
                "  from identity  where tenant_id=? and active=true and del_mark=false   order by update_time ";
        List<Map<String, Object>> maps = jdbcSSO.queryForList(sql, tenantId);
        List<Person> personList = new ArrayList<>();
        maps.forEach(map -> {
            Person person = new Person();
            try {
                MyBeanUtils.populate(person, map);
            } catch (Exception e) {
                e.printStackTrace();
            }
            personList.add(person);
        });
        return personList;
    }


    /**
     * 根据人员id 查 帐号信息
     *
     * @param ids
     * @return
     */
    @Override
    public List<String> getAccountByIdentityId(List<String> ids) {
        String sql = "select account_id from identity_account where identity_id in (?)";
        List<String> data = jdbcSSO.query(sql, new RowMapper<String>() {
            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getString(1);
            }
        }, ids);
        return data;
    }

    @Override
    public Integer deleteAccount(List<String> ids) {
        String str = "UPDATE account set  del_mark= 1 and update_time=now()" +
                " where id in (?) ";
        final int count = jdbcSSO.update(str, ids);
        return count;
    }

    /**
     * 根据人员id 查 三元组信息
     *
     * @param ids
     * @return
     */
    @Override
    public List<String> getOccupyByIdentityId(List<String> ids) {
        String sql = "select user_id from identity_user where identity_id in (?)";
        List<String> data = jdbcSSO.query(sql, new RowMapper<String>() {
            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getString(1);
            }
        }, ids);
        return data;
    }

    @Override
    public Integer deleteOccupy(List<String> ids) {
        String str = "UPDATE user set  del_mark= 1 and update_time=now()" +
                " where id in (?) ";
        final int count = jdbcSSO.update(str, ids);
        return count;
    }

}
