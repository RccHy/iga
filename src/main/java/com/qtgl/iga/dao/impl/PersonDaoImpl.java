package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.DynamicValue;
import com.qtgl.iga.bo.Person;
import com.qtgl.iga.dao.PersonDao;
import com.qtgl.iga.utils.FilterCodeEnum;
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
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
    public Integer saveToSso(Map<String, List<Person>> personMap, String tenantId, List<DynamicValue> valueUpdate, List<DynamicValue> valueInsert) {

        return txTemplate.execute(transactionStatus -> {
            try {
                if (personMap.containsKey("insert")) {
                    final List<Person> list = personMap.get("insert");


                    String str = "insert into identity (id, `name`, account_no,open_id,  del_mark, create_time, update_time, tenant_id, card_type, card_no, cellphone, email, data_source, tags,  `active`, active_time,`source`,valid_start_time,valid_end_time,freeze_time)" +
                            " values  (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";


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
                            preparedStatement.setObject(13, "PULL");
                            preparedStatement.setObject(14, list.get(i).getTags());
                            preparedStatement.setObject(15, list.get(i).getActive());
                            preparedStatement.setObject(16, list.get(i).getActiveTime());
                            preparedStatement.setObject(17, list.get(i).getSource());
                            preparedStatement.setObject(18, list.get(i).getValidStartTime());
                            preparedStatement.setObject(19, list.get(i).getValidEndTime());
                            preparedStatement.setObject(20, list.get(i).getFreezeTime());
                        }

                        @Override
                        public int getBatchSize() {
                            return list.size();
                        }
                    });
                }
                if (personMap.containsKey("update") || personMap.containsKey("invalid")) {
                    String str = "UPDATE identity set  `name`= ?, account_no=?,  del_mark=?, update_time=?, tenant_id=?,  cellphone=?, email=?,  tags=?,  `active`=?, active_time=? ,`source`= ?,data_source=?,valid_start_time=?,valid_end_time=? ," +
                            " card_type =? ,card_no=? where id=? and update_time< ? ";
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
                            preparedStatement.setObject(12, "PULL");
                            preparedStatement.setObject(13, list.get(i).getValidStartTime());
                            preparedStatement.setObject(14, list.get(i).getValidEndTime());
                            preparedStatement.setObject(15, list.get(i).getCardType());
                            preparedStatement.setObject(16, list.get(i).getCardNo());
                            preparedStatement.setObject(17, list.get(i).getId());
                            preparedStatement.setObject(18, list.get(i).getUpdateTime());
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
                            preparedStatement.setObject(1, "PULL");
                            preparedStatement.setObject(2, list.get(i).getId());
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
                // transactionStatus.rollbackToSavepoint(savepoint);
                throw new CustomException(ResultCode.FAILED, "同步终止，人员同步异常！");
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
                preparedStatement.setObject(13, "PULL");
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
        String sql = " TRUNCATE `person_temp`";
        jdbcIGA.execute(sql);
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

}
