package com.qtgl.iga.dao.impl;

import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.MergeAttrRule;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.dao.DynamicAttrDao;
import com.qtgl.iga.dao.PersonDao;
import com.qtgl.iga.service.MergeAttrRuleService;
import com.qtgl.iga.service.impl.OccupyServiceImpl;
import com.qtgl.iga.utils.MyBeanUtils;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.enums.FilterCodeEnum;
import com.qtgl.iga.utils.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


@Repository
@Component
@Slf4j
public class PersonDaoImpl implements PersonDao {

    //todo 人员预览(已弃用) 超级租户处理
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

    @Resource
    MergeAttrRuleService mergeAttrRuleService;

    List<String> columnBlacklist = Arrays.asList("id", "open_id", "del_mark", "active", "tenant_id", "create_time", "update_time", "source", "data_source", "freeze_time", "valid_start_time", "valid_end_time");


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
    public Integer saveToSso(Map<String, List<Person>> personMap, String tenantId, List<DynamicValue> valueUpdate, List<DynamicValue> valueInsert, List<Certificate> certificates, List<MergeAttrRule> mergeAttrRules) {

        return txTemplate.execute(transactionStatus -> {
            try {
                if (personMap.containsKey("insert")) {
                    final List<Person> list = personMap.get("insert");
                    String str = "insert into identity (id, `name`, account_no,open_id,  del_mark, create_time, update_time, tenant_id, card_type, card_no, cellphone, email, data_source, tags,  `active`, active_time,`source`,valid_start_time,valid_end_time,freeze_time," +
                            "create_data_source,create_source,birthday,sex)" +
                            " values  (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

                    // 对 list 分批执行，每批次5000条,不足5000按足量执行
                    int batchSize = 5000;
                    int batchCount = (list.size() + batchSize - 1) / batchSize;
                    for (int i = 0; i < batchCount; i++) {
                        int fromIndex = i * batchSize;
                        int toIndex = Math.min((i + 1) * batchSize, list.size());
                        log.info("from:" + fromIndex + "to:" + toIndex);
                        List<Person> subList = list.subList(fromIndex, toIndex);
                        int[] ints = jdbcSSO.batchUpdate(str, new BatchPreparedStatementSetter() {
                            @Override
                            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                                preparedStatement.setObject(1, subList.get(i).getId());
                                preparedStatement.setObject(2, subList.get(i).getName());
                                preparedStatement.setObject(3, subList.get(i).getAccountNo());
                                preparedStatement.setObject(4, subList.get(i).getOpenId());
                                preparedStatement.setObject(5, subList.get(i).getDelMark());
                                preparedStatement.setObject(6, subList.get(i).getCreateTime());
                                preparedStatement.setObject(7, subList.get(i).getUpdateTime());
                                preparedStatement.setObject(8, tenantId);
                                preparedStatement.setObject(9, subList.get(i).getCardType());
                                preparedStatement.setObject(10, subList.get(i).getCardNo());
                                preparedStatement.setObject(11, subList.get(i).getCellphone());
                                preparedStatement.setObject(12, subList.get(i).getEmail());
                                preparedStatement.setObject(13, subList.get(i).getDataSource());
                                preparedStatement.setObject(14, subList.get(i).getTags());
                                preparedStatement.setObject(15, subList.get(i).getActive());
                                preparedStatement.setObject(16, subList.get(i).getActiveTime());
                                preparedStatement.setObject(17, subList.get(i).getSource());
                                preparedStatement.setObject(18, subList.get(i).getValidStartTime());
                                preparedStatement.setObject(19, subList.get(i).getValidEndTime());
                                preparedStatement.setObject(20, subList.get(i).getFreezeTime());
                                preparedStatement.setObject(21, subList.get(i).getDataSource());
                                preparedStatement.setObject(22, subList.get(i).getSource());
                                preparedStatement.setObject(23, subList.get(i).getBirthday());
                                preparedStatement.setObject(24, subList.get(i).getSex());
                            }

                            @Override
                            public int getBatchSize() {
                                return subList.size();
                            }
                        });
                    }

                }
                if (personMap.containsKey("update") || personMap.containsKey("invalid")) {
                    String str = "UPDATE identity set  `name`= ?, account_no=?,  del_mark=?, update_time=?, tenant_id=?,  cellphone=?, email=?,  tags=?,  `active`=?, active_time=? ,`source`= ?,data_source=?,valid_start_time=?,valid_end_time=? ," +
                            " card_type =? ,card_no=?,birthday=?,sex=? where id=? and update_time<= ? ";
                    List<Person> list = new ArrayList<>();
                    List<Person> update = personMap.get("update");
                    List<Person> invalid = personMap.get("invalid");
                    if (null != invalid) {
                        list.addAll(invalid);
                    }
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
                        List<Person> subList = list.subList(fromIndex, toIndex);

                        int[] ints = jdbcSSO.batchUpdate(str, new BatchPreparedStatementSetter() {
                            @Override
                            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                                preparedStatement.setObject(1, subList.get(i).getName());
                                preparedStatement.setObject(2, subList.get(i).getAccountNo());
                                preparedStatement.setObject(3, subList.get(i).getDelMark() == null ? 0 : subList.get(i).getDelMark());
                                preparedStatement.setObject(4, subList.get(i).getUpdateTime());
                                preparedStatement.setObject(5, tenantId);
                                preparedStatement.setObject(6, subList.get(i).getCellphone());
                                preparedStatement.setObject(7, subList.get(i).getEmail());
                                preparedStatement.setObject(8, subList.get(i).getTags());
                                preparedStatement.setObject(9, subList.get(i).getActive());
                                preparedStatement.setObject(10, subList.get(i).getActiveTime());
                                preparedStatement.setObject(11, subList.get(i).getSource());
                                preparedStatement.setObject(12, subList.get(i).getDataSource());
                                preparedStatement.setObject(13, subList.get(i).getValidStartTime());
                                preparedStatement.setObject(14, subList.get(i).getValidEndTime());
                                preparedStatement.setObject(15, subList.get(i).getCardType());
                                preparedStatement.setObject(16, subList.get(i).getCardNo());
                                preparedStatement.setObject(17, subList.get(i).getBirthday());
                                preparedStatement.setObject(18, subList.get(i).getSex());
                                preparedStatement.setObject(19, subList.get(i).getId());
                                preparedStatement.setObject(20, subList.get(i).getUpdateTime());
                            }

                            @Override
                            public int getBatchSize() {
                                return subList.size();
                            }
                        });

                    }

                }
                if (personMap.containsKey("delete")) {
                    final List<Person> list = personMap.get("delete");

                    String str = "UPDATE identity set  del_mark= 1 , update_time=now() , data_source=?,active=0,valid_start_time=?,valid_end_time=? " +
                            " where id= ?  ";

                    // 对 list 分批执行，每批次5000条,不足5000按足量执行
                    int batchSize = 5000;
                    int batchCount = (list.size() + batchSize - 1) / batchSize;
                    for (int i = 0; i < batchCount; i++) {
                        int fromIndex = i * batchSize;
                        int toIndex = Math.min((i + 1) * batchSize, list.size());
                        log.info("from:" + fromIndex + "to:" + toIndex);
                        List<Person> subList = list.subList(fromIndex, toIndex);
                        int[] ints = jdbcSSO.batchUpdate(str, new BatchPreparedStatementSetter() {
                            @Override
                            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                                preparedStatement.setObject(1, subList.get(i).getDataSource());
                                preparedStatement.setObject(2, OccupyServiceImpl.DEFAULT_START_TIME);
                                preparedStatement.setObject(3, OccupyServiceImpl.DEFAULT_START_TIME);
                                preparedStatement.setObject(4, subList.get(i).getId());
                            }

                            @Override
                            public int getBatchSize() {
                                return subList.size();
                            }
                        });
                    }

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
                    // 对 list 分批执行，每批次5000条,不足5000按足量执行
                    int batchSize = 5000;
                    int batchCount = (list.size() + batchSize - 1) / batchSize;
                    for (int i = 0; i < batchCount; i++) {
                        int fromIndex = i * batchSize;
                        int toIndex = Math.min((i + 1) * batchSize, list.size());
                        log.info("from:" + fromIndex + "to:" + toIndex);
                        List<Person> subList = list.subList(fromIndex, toIndex);
                        String str = "INSERT INTO password(id,account_id,password,create_time,update_time,del_mark )" +
                                " SELECT " +
                                " uuid(),?,?,now(),now(),0 " +
                                " FROM DUAL " +
                                " WHERE NOT  EXISTS(select * from password where account_id=?)";


                        int[] ints = jdbcSSO.batchUpdate(str, new BatchPreparedStatementSetter() {
                            @Override
                            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                                preparedStatement.setObject(1, subList.get(i).getId());
                                preparedStatement.setObject(2, subList.get(i).getPassword());
                                preparedStatement.setObject(3, subList.get(i).getId());
                            }

                            @Override
                            public int getBatchSize() {
                                return subList.size();
                            }
                        });
                    }
                }

                if (!CollectionUtils.isEmpty(valueInsert)) {

                    String str = "INSERT INTO dynamic_value (`id`, `attr_id`, `entity_id`, `value`, `tenant_id`) VALUES (?, ?, ?, ?, ?)";
                    // 对 valueInsert 分批执行，每批次5000条,不足5000按足量执行
                    int batchSize = 5000;
                    int batchCount = (valueInsert.size() + batchSize - 1) / batchSize;
                    for (int i = 0; i < batchCount; i++) {
                        int fromIndex = i * batchSize;
                        int toIndex = Math.min((i + 1) * batchSize, valueInsert.size());
                        log.info("from:" + fromIndex + "to:" + toIndex);
                        List<DynamicValue> subList = valueInsert.subList(fromIndex, toIndex);

                        int[] ints = jdbcSSO.batchUpdate(str, new BatchPreparedStatementSetter() {
                            @Override
                            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                                preparedStatement.setObject(1, subList.get(i).getId());
                                preparedStatement.setObject(2, subList.get(i).getAttrId());
                                preparedStatement.setObject(3, subList.get(i).getEntityId());
                                preparedStatement.setObject(4, subList.get(i).getValue());
                                preparedStatement.setObject(5, tenantId);
                            }

                            @Override
                            public int getBatchSize() {
                                return subList.size();
                            }
                        });
                    }
                }

                if (!CollectionUtils.isEmpty(valueUpdate)) {
                    String valueStr = "update dynamic_value set `value`=? where id= ?";
                    // 对 valueStr 分批执行，每批次5000条,不足5000按足量执行
                    int batchSize = 5000;
                    int batchCount = (valueUpdate.size() + batchSize - 1) / batchSize;
                    for (int i = 0; i < batchCount; i++) {
                        int fromIndex = i * batchSize;
                        int toIndex = Math.min((i + 1) * batchSize, valueUpdate.size());
                        log.info("from:" + fromIndex + "to:" + toIndex);
                        List<DynamicValue> subList = valueUpdate.subList(fromIndex, toIndex);

                        int[] ints = jdbcSSO.batchUpdate(valueStr, new BatchPreparedStatementSetter() {
                            @Override
                            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                                preparedStatement.setObject(1, subList.get(i).getValue());
                                preparedStatement.setObject(2, subList.get(i).getId());
                            }

                            @Override
                            public int getBatchSize() {
                                return subList.size();
                            }
                        });
                    }

                }

                if (!CollectionUtils.isEmpty(certificates)) {
                    String certificateStr = "update certificate set card_type=? , card_no =? ,update_time=?  where id= ?";
                    // 对 certificateStr 分批执行，每批次5000条,不足5000按足量执行
                    int batchSize = 5000;
                    int batchCount = (certificates.size() + batchSize - 1) / batchSize;
                    for (int i = 0; i < batchCount; i++) {
                        int fromIndex = i * batchSize;
                        int toIndex = Math.min((i + 1) * batchSize, certificates.size());
                        log.info("from:" + fromIndex + "to:" + toIndex);
                        List<Certificate> subList = certificates.subList(fromIndex, toIndex);

                        int[] ints = jdbcSSO.batchUpdate(certificateStr, new BatchPreparedStatementSetter() {
                            @Override
                            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                                preparedStatement.setObject(1, subList.get(i).getCardType());
                                preparedStatement.setObject(2, subList.get(i).getCardNo());
                                preparedStatement.setObject(3, subList.get(i).getUpdateTime());
                                preparedStatement.setObject(4, subList.get(i).getId());
                            }

                            @Override
                            public int getBatchSize() {
                                return subList.size();
                            }
                        });
                    }

                }

                // 运行属性合重规则
                if (!CollectionUtils.isEmpty(mergeAttrRules)) {
                    for (MergeAttrRule mergeAttrRule : mergeAttrRules) {
                        if (StringUtils.isNotBlank(mergeAttrRule.getDynamicAttrId())) {
                            String sql = "update  dynamic_value set value=(select a.`value` from (SELECT `value` FROM dynamic_value WHERE entity_id = ? and attr_id=?)  a) where entity_id=? and attr_id=?";
                            int update = jdbcSSO.update(sql, new Object[]{mergeAttrRule.getFromEntityId(), mergeAttrRule.getDynamicAttrId(), mergeAttrRule.getEntityId(), mergeAttrRule.getDynamicAttrId()});
                            if (update <= 0) {
                                //修改操作无数据则新增
                                String insertSql = "INSERT INTO `dynamic_value` ( `id`, `attr_id`, `entity_id`, `value`, `tenant_id` ) VALUES" +
                                        " (uuid( ),?,?,( SELECT a.`value` FROM ( SELECT `value` FROM dynamic_value WHERE entity_id = ? AND attr_id = ? ) a ), " +
                                        " ?)";
                                jdbcSSO.update(insertSql, mergeAttrRule.getDynamicAttrId(), mergeAttrRule.getEntityId(), mergeAttrRule.getFromEntityId(), mergeAttrRule.getDynamicAttrId(), tenantId);
                            }

                        } else {
                            // 非动态属性
                            if (columnBlacklist.contains(mergeAttrRule.getAttrName())) {
                                // 如果是黑名单属性，不进行合重
                                continue;
                            }
                            String sql = "update  identity set " + mergeAttrRule.getAttrName() + "=( select a." + mergeAttrRule.getAttrName() + " from (select " + mergeAttrRule.getAttrName() + " from identity where id=?) a ) where id=?";
                            jdbcSSO.update(sql, new Object[]{mergeAttrRule.getFromEntityId(), mergeAttrRule.getEntityId()});
                        }
                    }


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
    public Integer saveToSsoTest(Map<String, List<Person>> personMap, String tenantId, List<DynamicValue> valueUpdate, List<DynamicValue> valueInsert, List<DynamicAttr> attrList, List<Certificate> certificates, List<DynamicValue> dynamicValues) {
        log.info("-----开始删除原有的人员数据-----");
        // 删除租户下所有数据
        deletePersonData(tenantId);
        log.info("-----原有的人员数据删除完毕-----");

        String str = "insert into identity (id, `name`, account_no,open_id,  del_mark, create_time, update_time, tenant_id, card_type, card_no, cellphone, email, data_source, tags,  `active`, active_time,`source`,valid_start_time,valid_end_time,freeze_time," +
                "create_data_source,create_source,sex,birthday,avatar,sync_state)" +
                " values  (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        return igaTemplate.execute(transactionStatus -> {
            try {


                if (personMap.containsKey("keep")) {
                    final List<Person> list = personMap.get("keep");
                    if (!CollectionUtils.isEmpty(list)) {

                        int batchSize = 5000;
                        int batchCount = (list.size() + batchSize - 1) / batchSize;
                        for (int i = 0; i < batchCount; i++) {
                            int fromIndex = i * batchSize;
                            int toIndex = Math.min((i + 1) * batchSize, list.size());
                            log.info("from:" + fromIndex + "to:" + toIndex);
                            List<Person> subList = list.subList(fromIndex, toIndex);

                            int[] ints = jdbcIGA.batchUpdate(str, new BatchPreparedStatementSetter() {
                                @Override
                                public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                                    preparedStatement.setObject(1, subList.get(i).getId());
                                    preparedStatement.setObject(2, subList.get(i).getName());
                                    preparedStatement.setObject(3, subList.get(i).getAccountNo());
                                    preparedStatement.setObject(4, subList.get(i).getOpenId());
                                    preparedStatement.setObject(5, subList.get(i).getDelMark());
                                    preparedStatement.setObject(6, subList.get(i).getCreateTime());
                                    preparedStatement.setObject(7, subList.get(i).getUpdateTime());
                                    preparedStatement.setObject(8, tenantId);
                                    preparedStatement.setObject(9, subList.get(i).getCardType());
                                    preparedStatement.setObject(10, subList.get(i).getCardNo());
                                    preparedStatement.setObject(11, subList.get(i).getCellphone());
                                    preparedStatement.setObject(12, subList.get(i).getEmail());
                                    preparedStatement.setObject(13, subList.get(i).getDataSource());
                                    preparedStatement.setObject(14, subList.get(i).getTags());
                                    preparedStatement.setObject(15, subList.get(i).getActive());
                                    preparedStatement.setObject(16, subList.get(i).getActiveTime());
                                    preparedStatement.setObject(17, subList.get(i).getSource());
                                    preparedStatement.setObject(18, subList.get(i).getValidStartTime());
                                    preparedStatement.setObject(19, subList.get(i).getValidEndTime());
                                    preparedStatement.setObject(20, subList.get(i).getFreezeTime());
                                    preparedStatement.setObject(21, subList.get(i).getDataSource());
                                    preparedStatement.setObject(22, subList.get(i).getSource());
                                    preparedStatement.setObject(23, subList.get(i).getSex());
                                    preparedStatement.setObject(24, subList.get(i).getBirthday());
                                    preparedStatement.setObject(25, subList.get(i).getAvatar());
                                    preparedStatement.setObject(26, 0);
                                }

                                @Override
                                public int getBatchSize() {
                                    return subList.size();
                                }
                            });

                        }
                    }
                }


                if (personMap.containsKey("insert")) {
                    final List<Person> list = personMap.get("insert");

                    if (!CollectionUtils.isEmpty(list)) {

                        int batchSize = 5000;
                        int batchCount = (list.size() + batchSize - 1) / batchSize;
                        for (int i = 0; i < batchCount; i++) {
                            int fromIndex = i * batchSize;
                            int toIndex = Math.min((i + 1) * batchSize, list.size());
                            log.info("from:" + fromIndex + "to:" + toIndex);
                            List<Person> subList = list.subList(fromIndex, toIndex);

                            int[] ints = jdbcIGA.batchUpdate(str, new BatchPreparedStatementSetter() {
                                @Override
                                public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                                    preparedStatement.setObject(1, subList.get(i).getId());
                                    preparedStatement.setObject(2, subList.get(i).getName());
                                    preparedStatement.setObject(3, subList.get(i).getAccountNo());
                                    preparedStatement.setObject(4, subList.get(i).getOpenId());
                                    preparedStatement.setObject(5, subList.get(i).getDelMark());
                                    preparedStatement.setObject(6, subList.get(i).getCreateTime());
                                    preparedStatement.setObject(7, subList.get(i).getUpdateTime());
                                    preparedStatement.setObject(8, tenantId);
                                    preparedStatement.setObject(9, subList.get(i).getCardType());
                                    preparedStatement.setObject(10, subList.get(i).getCardNo());
                                    preparedStatement.setObject(11, subList.get(i).getCellphone());
                                    preparedStatement.setObject(12, subList.get(i).getEmail());
                                    preparedStatement.setObject(13, subList.get(i).getDataSource());
                                    preparedStatement.setObject(14, subList.get(i).getTags());
                                    preparedStatement.setObject(15, subList.get(i).getActive());
                                    preparedStatement.setObject(16, subList.get(i).getActiveTime());
                                    preparedStatement.setObject(17, subList.get(i).getSource());
                                    preparedStatement.setObject(18, subList.get(i).getValidStartTime());
                                    preparedStatement.setObject(19, subList.get(i).getValidEndTime());
                                    preparedStatement.setObject(20, subList.get(i).getFreezeTime());
                                    preparedStatement.setObject(21, subList.get(i).getDataSource());
                                    preparedStatement.setObject(22, subList.get(i).getSource());
                                    preparedStatement.setObject(23, subList.get(i).getSex());
                                    preparedStatement.setObject(24, subList.get(i).getBirthday());
                                    preparedStatement.setObject(25, subList.get(i).getAvatar());
                                    preparedStatement.setObject(26, 1);
                                }

                                @Override
                                public int getBatchSize() {
                                    return subList.size();
                                }
                            });

                        }
                    }
                }
                if (personMap.containsKey("update")) {
                    final List<Person> list = personMap.get("update");
                    if (!CollectionUtils.isEmpty(list)) {

                        int batchSize = 5000;
                        int batchCount = (list.size() + batchSize - 1) / batchSize;
                        for (int i = 0; i < batchCount; i++) {
                            int fromIndex = i * batchSize;
                            int toIndex = Math.min((i + 1) * batchSize, list.size());
                            log.info("from:" + fromIndex + "to:" + toIndex);
                            List<Person> subList = list.subList(fromIndex, toIndex);

                            int[] ints = jdbcIGA.batchUpdate(str, new BatchPreparedStatementSetter() {
                                @Override
                                public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                                    preparedStatement.setObject(1, subList.get(i).getId());
                                    preparedStatement.setObject(2, subList.get(i).getName());
                                    preparedStatement.setObject(3, subList.get(i).getAccountNo());
                                    preparedStatement.setObject(4, subList.get(i).getOpenId());
                                    preparedStatement.setObject(5, subList.get(i).getDelMark());
                                    preparedStatement.setObject(6, subList.get(i).getCreateTime());
                                    preparedStatement.setObject(7, subList.get(i).getUpdateTime());
                                    preparedStatement.setObject(8, tenantId);
                                    preparedStatement.setObject(9, subList.get(i).getCardType());
                                    preparedStatement.setObject(10, subList.get(i).getCardNo());
                                    preparedStatement.setObject(11, subList.get(i).getCellphone());
                                    preparedStatement.setObject(12, subList.get(i).getEmail());
                                    preparedStatement.setObject(13, subList.get(i).getDataSource());
                                    preparedStatement.setObject(14, subList.get(i).getTags());
                                    preparedStatement.setObject(15, subList.get(i).getActive());
                                    preparedStatement.setObject(16, subList.get(i).getActiveTime());
                                    preparedStatement.setObject(17, subList.get(i).getSource());
                                    preparedStatement.setObject(18, subList.get(i).getValidStartTime());
                                    preparedStatement.setObject(19, subList.get(i).getValidEndTime());
                                    preparedStatement.setObject(20, subList.get(i).getFreezeTime());
                                    preparedStatement.setObject(21, subList.get(i).getDataSource());
                                    preparedStatement.setObject(22, subList.get(i).getSource());
                                    preparedStatement.setObject(23, subList.get(i).getSex());
                                    preparedStatement.setObject(24, subList.get(i).getBirthday());
                                    preparedStatement.setObject(25, subList.get(i).getAvatar());
                                    preparedStatement.setObject(26, 3);
                                }

                                @Override
                                public int getBatchSize() {
                                    return subList.size();
                                }
                            });

                        }
                    }
                }

                if (personMap.containsKey("delete")) {
                    final List<Person> list = personMap.get("delete");
                    if (!CollectionUtils.isEmpty(list)) {

                        int batchSize = 5000;
                        int batchCount = (list.size() + batchSize - 1) / batchSize;
                        for (int i = 0; i < batchCount; i++) {
                            int fromIndex = i * batchSize;
                            int toIndex = Math.min((i + 1) * batchSize, list.size());
                            log.info("from:" + fromIndex + "to:" + toIndex);
                            List<Person> subList = list.subList(fromIndex, toIndex);

                            int[] ints = jdbcIGA.batchUpdate(str, new BatchPreparedStatementSetter() {
                                @Override
                                public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                                    preparedStatement.setObject(1, subList.get(i).getId());
                                    preparedStatement.setObject(2, subList.get(i).getName());
                                    preparedStatement.setObject(3, subList.get(i).getAccountNo());
                                    preparedStatement.setObject(4, subList.get(i).getOpenId());
                                    preparedStatement.setObject(5, subList.get(i).getDelMark());
                                    preparedStatement.setObject(6, subList.get(i).getCreateTime());
                                    preparedStatement.setObject(7, subList.get(i).getUpdateTime());
                                    preparedStatement.setObject(8, tenantId);
                                    preparedStatement.setObject(9, subList.get(i).getCardType());
                                    preparedStatement.setObject(10, subList.get(i).getCardNo());
                                    preparedStatement.setObject(11, subList.get(i).getCellphone());
                                    preparedStatement.setObject(12, subList.get(i).getEmail());
                                    preparedStatement.setObject(13, subList.get(i).getDataSource());
                                    preparedStatement.setObject(14, subList.get(i).getTags());
                                    preparedStatement.setObject(15, subList.get(i).getActive());
                                    preparedStatement.setObject(16, subList.get(i).getActiveTime());
                                    preparedStatement.setObject(17, subList.get(i).getSource());
                                    preparedStatement.setObject(18, subList.get(i).getValidStartTime());
                                    preparedStatement.setObject(19, subList.get(i).getValidEndTime());
                                    preparedStatement.setObject(20, subList.get(i).getFreezeTime());
                                    preparedStatement.setObject(21, subList.get(i).getDataSource());
                                    preparedStatement.setObject(22, subList.get(i).getSource());
                                    preparedStatement.setObject(23, subList.get(i).getSex());
                                    preparedStatement.setObject(24, subList.get(i).getBirthday());
                                    preparedStatement.setObject(25, subList.get(i).getAvatar());
                                    preparedStatement.setObject(26, 2);
                                }

                                @Override
                                public int getBatchSize() {
                                    return subList.size();
                                }
                            });

                        }
                    }

                }


                if (personMap.containsKey("invalid")) {
                    final List<Person> list = personMap.get("invalid");
                    if (!CollectionUtils.isEmpty(list)) {

                        int batchSize = 5000;
                        int batchCount = (list.size() + batchSize - 1) / batchSize;
                        for (int i = 0; i < batchCount; i++) {
                            int fromIndex = i * batchSize;
                            int toIndex = Math.min((i + 1) * batchSize, list.size());
                            log.info("from:" + fromIndex + "to:" + toIndex);
                            List<Person> subList = list.subList(fromIndex, toIndex);

                            int[] ints = jdbcIGA.batchUpdate(str, new BatchPreparedStatementSetter() {
                                @Override
                                public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                                    preparedStatement.setObject(1, subList.get(i).getId());
                                    preparedStatement.setObject(2, subList.get(i).getName());
                                    preparedStatement.setObject(3, subList.get(i).getAccountNo());
                                    preparedStatement.setObject(4, subList.get(i).getOpenId());
                                    preparedStatement.setObject(5, subList.get(i).getDelMark());
                                    preparedStatement.setObject(6, subList.get(i).getCreateTime());
                                    preparedStatement.setObject(7, subList.get(i).getUpdateTime());
                                    preparedStatement.setObject(8, tenantId);
                                    preparedStatement.setObject(9, subList.get(i).getCardType());
                                    preparedStatement.setObject(10, subList.get(i).getCardNo());
                                    preparedStatement.setObject(11, subList.get(i).getCellphone());
                                    preparedStatement.setObject(12, subList.get(i).getEmail());
                                    preparedStatement.setObject(13, subList.get(i).getDataSource());
                                    preparedStatement.setObject(14, subList.get(i).getTags());
                                    preparedStatement.setObject(15, subList.get(i).getActive());
                                    preparedStatement.setObject(16, subList.get(i).getActiveTime());
                                    preparedStatement.setObject(17, subList.get(i).getSource());
                                    preparedStatement.setObject(18, subList.get(i).getValidStartTime());
                                    preparedStatement.setObject(19, subList.get(i).getValidEndTime());
                                    preparedStatement.setObject(20, subList.get(i).getFreezeTime());
                                    preparedStatement.setObject(21, subList.get(i).getDataSource());
                                    preparedStatement.setObject(22, subList.get(i).getSource());
                                    preparedStatement.setObject(23, subList.get(i).getSex());
                                    preparedStatement.setObject(24, subList.get(i).getBirthday());
                                    preparedStatement.setObject(25, subList.get(i).getAvatar());
                                    preparedStatement.setObject(26, 4);
                                }

                                @Override
                                public int getBatchSize() {
                                    return subList.size();
                                }
                            });

                        }
                    }
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

                //
                //List<DynamicValue> dynamicValues = new ArrayList<>();
                //if (!CollectionUtils.isEmpty(valueInsert)) {
                //    dynamicValues.addAll(valueInsert);
                //}
                //if (!CollectionUtils.isEmpty(valueUpdate)) {
                //    dynamicValues.addAll(valueUpdate);
                //}


                if (!CollectionUtils.isEmpty(attrList)) {
                    // 删除、并重新创建扩展字段
                    String deleteDynamicAttrSql = "delete from dynamic_attr where   type='USER' and tenant_id = ?";
                    jdbcIGA.update(deleteDynamicAttrSql, tenantId);
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
                    // 删除value条件
                    String deleteDynamicValueSql = "delete from dynamic_value where  tenant_id = ? and attr_id  in (select id from dynamic_attr where type='USER' and tenant_id=?  )";
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


                // 运行属性合重规则，先查询出所有合重属性规则
                List<MergeAttrRule> mergeAttrRules = mergeAttrRuleService.findOriginalMergeAttrRulesByTenantId(tenantId);

                if (!CollectionUtils.isEmpty(mergeAttrRules)) {
                    for (MergeAttrRule mergeAttrRule : mergeAttrRules) {
                        if (StringUtils.isNotBlank(mergeAttrRule.getDynamicAttrId())) {
                            String sql = "update from dynamic_value set value=(select value from dynamic_value where entity_id=?) where entity_id=?";
                            jdbcSSO.update(sql, mergeAttrRule.getFromEntityId(), mergeAttrRule.getEntityId());

                        } else {
                            // 非动态属性
                            if (columnBlacklist.contains(mergeAttrRule.getAttrName())) {
                                // 如果是黑名单属性，不进行合重
                                continue;
                            }
                            String sql = "update from identity set " + mergeAttrRule.getAttrName() + "=(select " + mergeAttrRule.getAttrName() + " from identity where id=?) where id=?";
                            jdbcSSO.update(sql, mergeAttrRule.getFromEntityId(), mergeAttrRule.getEntityId());
                        }
                    }


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

    private void deletePersonData(String tenantId) {

        String deleteSql = "delete from identity where tenant_id = ? limit 5000";
        int update = jdbcIGA.update(deleteSql, tenantId);
        if (update >= 5000) {
            deletePersonData(tenantId);
        }
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
        String sql = " SELECT id from person_temp i where i.tenant_id=? ";
        //拼接sql
        StringBuffer stb = new StringBuffer(sql);
        //存入参数
        List<Object> param = new ArrayList<>();
        param.add(domain.getId());
        StringBuffer strTemp = new StringBuffer();
        dealData(arguments, strTemp, param);
        stb.append(strTemp);

        stb.append(" order by i.create_time desc ");
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
        String sql = " delete from  `person_temp` where tenant_id =? ";
        jdbcIGA.update(sql, domain.getId());
    }

    @Override
    public Integer findPersonTempCount(Map<String, Object> arguments, DomainInfo domain) {
        String sql = " SELECT count(*) from person_temp i where i.tenant_id=?  ";
        //拼接sql
        StringBuffer stb = new StringBuffer(sql);
        List<Object> param = new ArrayList<>();
        param.add(domain.getId());
        if (null != arguments) {
            StringBuffer strTemp = new StringBuffer();
            dealData(arguments, strTemp, param);
            stb.append(strTemp);
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
            if ("i.id".equals(entry.getKey())) {
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
                                stb.append("and i.name ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if ("in".equals(FilterCodeEnum.getDescByCode(soe.getKey())) || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and i.name ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and i.name ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("username".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if ("like".equals(FilterCodeEnum.getDescByCode(soe.getKey()))) {
                                stb.append("and i.account_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if ("in".equals(FilterCodeEnum.getDescByCode(soe.getKey())) || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and i.account_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and i.account_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }

                    if ("openid".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if ("like".equals(FilterCodeEnum.getDescByCode(soe.getKey()))) {
                                stb.append("and i.open_id ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if ("in".equals(FilterCodeEnum.getDescByCode(soe.getKey())) || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and i.open_id ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and i.open_id ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }

                    if ("phone".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if ("like".equals(FilterCodeEnum.getDescByCode(soe.getKey()))) {
                                stb.append("and i.cellphone ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if ("in".equals(FilterCodeEnum.getDescByCode(soe.getKey())) || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and i.cellphone ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and i.cellphone ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("active".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            stb.append("and i.active ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                            param.add(soe.getValue());
                        }
                    }
                    if ("cardNo".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "like")) {
                                stb.append("and i.card_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and i.card_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and i.card_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("email".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("like")) {
                                stb.append("and i.email ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and i.email ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and i.email ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
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

    @Override
    public List<Person> findRepeatPerson(String tenantId, String dataSource) {
        String sql = " SELECT " +
                " i.id, " +
                " i.account_no AS accountNo, " +
                " i.card_no AS cardNo, " +
                " i.card_type AS cardType, " +
                " i.source, " +
                " i.data_source AS dataSource, " +
                " i.update_time AS updateTime " +
                " FROM " +
                " identity i " +
                " WHERE " +
                " i.tenant_id = ? " +
                " and data_source = ? " +
                " GROUP BY " +
                " i.account_no,i.card_no,i.card_type HAVING count(1)>1 ";
        List<Map<String, Object>> maps = jdbcSSO.queryForList(sql, tenantId, "PULL");

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
    public List<Person> findPersonByDataSource(String tenantId, String dataSource) {
        String sql = " SELECT " +
                " i.id, " +
                " i.account_no AS accountNo, " +
                " i.card_no AS cardNo, " +
                " i.card_type AS cardType, " +
                " i.source, " +
                " i.data_source AS dataSource, " +
                " i.update_time AS updateTime " +
                " FROM " +
                " identity i " +
                " WHERE " +
                " i.tenant_id = ? " +
                " and data_source = ? " +
                " ORDER BY " +
                " i.update_time";
        List<Map<String, Object>> maps = jdbcSSO.queryForList(sql, tenantId, "PULL");

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
    public JSONObject dealWithPeople(ArrayList<Person> resultPeople) {
        JSONObject jsonObject = new JSONObject();
        txTemplate.execute(transactionStatus -> {
            try {
                if (!CollectionUtils.isEmpty(resultPeople)) {
                    StringBuffer buffer = new StringBuffer();
                    //删除人员
                    StringBuffer str = new StringBuffer("delete from identity where id in( ");
                    //存入参数
                    List<Object> param = new ArrayList<>();
                    for (Person resultPerson : resultPeople) {
                        str.append("?,");
                        param.add(resultPerson.getId());
                    }
                    str.replace(str.length() - 1, str.length(), ")");
                    int update = jdbcSSO.update(str.toString(), param.toArray());
                    buffer = buffer.append("重复人员删除数量:").append(update);

                    //删除中间表
                    StringBuffer identityUserSql = new StringBuffer("delete from identity_user where identity_id  not in (select id from identity ) ");
                    jdbcSSO.update(identityUserSql.toString());

                    //删除人员身份
                    StringBuffer userSql = new StringBuffer("delete from user where id  not in (select user_id from identity_user ) ");
                    int userCount = jdbcSSO.update(userSql.toString());


                    buffer = buffer.append("因重复人员删除身份数量:").append(userCount);
                    jsonObject.put("code", "SUCCESS");
                    jsonObject.put("message", buffer.toString());

                } else {
                    jsonObject.put("code", "SUCCESS");
                    jsonObject.put("message", "当前租户无重复标识人员");
                }
                return jsonObject;
            } catch (Exception e) {
                e.printStackTrace();
                transactionStatus.setRollbackOnly();
                // transactionStatus.rollbackToSavepoint(savepoint);
                throw new CustomException(ResultCode.FAILED, "删除重复标识人员失败");
            }
        });

        return jsonObject;
    }

    @Override
    public Map<String, Object> findTestPersons(Map<String, Object> arguments, Tenant tenant) {

        Object first = arguments.get("first");
        Object offset = arguments.get("offset");

        List<Object> params = new ArrayList<>();

        String queryStr = "SELECT " +
                "                 i.id," +
                "                 i.NAME as name, " +
                "                 i.tags, " +
                "                 i.open_id AS openId, " +
                "                 i.account_no AS accountNo, " +
                "                 i.card_type AS cardType, " +
                "                 i.card_no AS cardNo, " +
                "                 i.cellphone, " +
                "                 i.email, " +
                "                 i.source, " +
                "                 i.sex, " +
                "                 i.birthday, " +
                "                 i.data_source AS dataSource, " +
                "                 i.create_source AS createSource, " +
                "                 i.create_data_source AS createDataSource, " +
                "                 i.active, " +
                "                 i.active_time AS activeTime, " +
                "                 i.create_time AS createTime, " +
                "                 i.update_time AS updateTime, " +
                "                 i.del_mark AS delMark," +
                "                 i.valid_start_time AS validStartTime, " +
                "                 i.sync_state AS syncState, " +
                "                 i.valid_end_time AS validEndTime FROM identity i ";
        String countSql = "SELECT count(*) from identity i   ";
        //主体查询语句
        StringBuffer stb = new StringBuffer(queryStr);
        //查询总数语句
        StringBuffer countStb = new StringBuffer(countSql);

        StringBuffer strTemp = new StringBuffer("  ");
        params.add(tenant.getId());
        strTemp = dealDataAndAttr(arguments, strTemp, params, tenant.getId());
        stb.append(strTemp);
        countStb.append(strTemp);
        stb.append(" order by i.update_time desc");
        if (null != first && null != offset) {
            stb.append(" limit ").append(offset).append(",").append(first);
        }

        log.info(stb.toString());
        log.info(countStb.toString());
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(stb.toString(), params.toArray());
        ArrayList<Person> list = new ArrayList<>();
        if (!CollectionUtils.isEmpty(mapList)) {

            mapList.forEach(map -> {
                Person person = new Person();
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


    private StringBuffer dealDataAndAttr(Map<String, Object> arguments, StringBuffer stb, List<Object> param, String tenantId) {

        //扩展字段查询拼接主体sql
        StringBuffer sql = new StringBuffer();
        boolean activeFlag = true;
        Iterator<Map.Entry<String, Object>> it = arguments.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            if ("i.id".equals(entry.getKey())) {
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
                                stb.append("and i.name ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if ("in".equals(FilterCodeEnum.getDescByCode(soe.getKey())) || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and i.name ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and i.name ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("username".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if ("like".equals(FilterCodeEnum.getDescByCode(soe.getKey()))) {
                                stb.append("and i.account_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if ("in".equals(FilterCodeEnum.getDescByCode(soe.getKey())) || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and i.account_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and i.account_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }

                    if ("openid".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if ("like".equals(FilterCodeEnum.getDescByCode(soe.getKey()))) {
                                stb.append("and i.open_id ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if ("in".equals(FilterCodeEnum.getDescByCode(soe.getKey())) || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and i.open_id ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and i.open_id ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }

                    if ("phone".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if ("like".equals(FilterCodeEnum.getDescByCode(soe.getKey()))) {
                                stb.append("and i.cellphone ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if ("in".equals(FilterCodeEnum.getDescByCode(soe.getKey())) || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and i.cellphone ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and i.cellphone ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("active".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        activeFlag = false;
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if ("eq".equals(soe.getKey())) {
                                if (Boolean.parseBoolean(soe.getValue().toString())) {
                                    stb.append("  and ( NOW() BETWEEN i.valid_start_time and i.valid_end_time) ");
                                } else {
                                    stb.append("  and  ( NOW() NOT BETWEEN i.valid_start_time and i.valid_end_time)");

                                }
                            } else if ("in".equals(FilterCodeEnum.getDescByCode(soe.getKey()))) {
                                Boolean trueFlag = false;
                                Boolean falseFlag = false;
                                ArrayList<Boolean> value1 = (ArrayList<Boolean>) soe.getValue();
                                StringBuffer temp = new StringBuffer();
                                temp.append(" and ( ");
                                for (Boolean b : value1) {
                                    if (b) {
                                        trueFlag = true;
                                        temp.append(" ( NOW() BETWEEN i.valid_start_time and i.valid_end_time) or");
                                    } else {
                                        falseFlag = true;
                                        temp.append(" ( NOW() NOT BETWEEN i.valid_start_time and i.valid_end_time) or");
                                    }
                                    if (trueFlag && falseFlag) {
                                        break;
                                    }
                                }
                                if (trueFlag && falseFlag) {

                                } else {
                                    temp.replace(temp.length() - 2, temp.length(), ")");
                                    stb.append(temp);
                                }
                            }


                        }
                    }
                    if ("cardNo".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "like")) {
                                stb.append("and i.card_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and i.card_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and i.card_no ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("email".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("like")) {
                                stb.append("and i.email ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and i.email ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and i.email ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("syncState".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if ("eq".equals(soe.getKey())) {
                                stb.append("and i.sync_state ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }

                    if ("source".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "like")) {
                                stb.append("and i.source ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and i.source ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and i.source ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("createSource".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "like")) {
                                stb.append("and i.create_source ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and i.create_source ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and i.create_source ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("dataSource".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "like")) {
                                stb.append("and i.data_source ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and i.data_source ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and i.data_source ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("createDataSource".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "like")) {
                                stb.append("and i.create_data_source ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and i.create_data_source ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and i.create_data_source ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }

                    if ("extension".equals(str.getKey())) {
                        List<HashMap<String, Object>> values = (List<HashMap<String, Object>>) str.getValue();
                        if (!CollectionUtils.isEmpty(values)) {
                            List<DynamicAttr> users = dynamicAttrDao.findAllByTypeIGA("USER", tenantId);
                            Map<String, String> collect = users.stream().collect(Collectors.toMap(DynamicAttr::getCode, DynamicAttr::getId));
                            char aliasOld = 'j';

                            for (HashMap<String, Object> value : values) {
                                String key = (String) value.get("key");
                                if (collect.containsKey(key)) {
                                    HashMap<String, Object> val = (HashMap<String, Object>) value.get("value");
                                    if (!CollectionUtils.isEmpty(val)) {
                                        for (Map.Entry<String, Object> soe : val.entrySet()) {
                                            if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("like")) {
                                                char aliasNew = (char) (aliasOld + 1);
                                                sql.append(" LEFT JOIN dynamic_value ").append(aliasNew).append(" ON ").append(aliasNew).append(".entity_id = ").append(" i.id ");
                                                stb.append(" and (").append(aliasNew).append(".value like ? and ").append(aliasNew).append(".attr_id=? )");
                                                aliasOld = aliasNew;
                                                param.add("%" + soe.getValue() + "%");
                                                param.add(collect.get(key));
                                            } else {
                                                char aliasNew = (char) (aliasOld + 1);

                                                sql.append(" LEFT JOIN dynamic_value ").append(aliasNew).append(" ON ").append(aliasNew).append(".entity_id = ").append(" i.id ");
                                                stb.append(" and (").append(aliasNew).append(".value = ? and ").append(aliasNew).append(".attr_id=? )");
                                                aliasOld = aliasNew;
                                                param.add(soe.getValue());
                                                param.add(collect.get(key));
                                            }
                                        }
                                    } else {
                                        HashMap<String, Object> timeVal = (HashMap<String, Object>) value.get("timestampValue");
                                        if (!CollectionUtils.isEmpty(timeVal)) {
                                            for (Map.Entry<String, Object> soe : timeVal.entrySet()) {
                                                if ("gt".equals(soe.getKey()) || "lt".equals(soe.getKey())
                                                        || "gte".equals(soe.getKey()) || "lte".equals(soe.getKey())) {
                                                    char aliasNew = (char) (aliasOld + 1);
                                                    sql.append(" LEFT JOIN dynamic_value ").append(aliasNew).append(" ON ").append(aliasNew).append(".entity_id = ").append(" i.id ");
                                                    stb.append(" and (").append(aliasNew).append(".value").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? and ").append(aliasNew).append(".attr_id=? )");
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

            }
        }
        StringBuffer buffer = new StringBuffer(" WHERE 1=1 AND i.TENANT_ID = ? ");
        if (StringUtils.isNotBlank(sql)) {
            stb = sql.append(buffer).append(stb);
        } else {
            stb = buffer.append(stb);
        }
        if (activeFlag) {
            stb.append("  and ( NOW() BETWEEN i.valid_start_time and i.valid_end_time) ");
        }
        return stb;
    }

}
