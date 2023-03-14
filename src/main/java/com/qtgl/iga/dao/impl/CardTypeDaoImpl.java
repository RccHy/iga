package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.CardType;
import com.qtgl.iga.dao.CardTypeDao;
import com.qtgl.iga.utils.MyBeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
@Component
public class CardTypeDaoImpl implements CardTypeDao {


    @Resource(name = "jdbcSSOAPI")
    JdbcTemplate jdbcSSOAPI;

    @Override
    public List<CardType> findAllUser(String domain) {
        String sql = "select id, card_type_name as cardTypeName, card_type_code as cardTypeCode, card_type_reg as cardTypeReg  from card_type where tenant_id=? and card_type_type='USER'";
        List<Map<String, Object>> listMaps = jdbcSSOAPI.queryForList(sql, domain);
        List<CardType> cardTypes = new ArrayList<>();
        listMaps.forEach(map -> {
            CardType cardType = new CardType();
            try {
                MyBeanUtils.populate(cardType, map);
            } catch (Exception e) {
                e.printStackTrace();
            }
            cardTypes.add(cardType);
        });
        return cardTypes;
    }


    @Override
    public List<CardType> findAllFromIdentity(String domain) {
        String sql = "select id, card_type_name as cardTypeName, card_type_code as cardTypeCode, card_type_reg as cardTypeReg  from card_type where tenant_id=? and card_type_type='IDENTITY'";
        List<Map<String, Object>> listMaps = jdbcSSOAPI.queryForList(sql, domain);
        List<CardType> cardTypes = new ArrayList<>();
        listMaps.forEach(map -> {
            CardType cardType = new CardType();
            try {
                MyBeanUtils.populate(cardType, map);
            } catch (Exception e) {
                e.printStackTrace();
            }
            cardTypes.add(cardType);
        });
        return cardTypes;
    }

    @Override
    public void initialization(String domain) {
        // 根据国标初始化用户证件类型

        String sql = "INSERT INTO card_type (id,  card_type_code, card_type_name,card_type_reg, card_type_type, tenant_id)" +
                "VALUES (UUID(), '1', '居民身份证', null, 'USER', ?)" +
                "     , (UUID(), '2', '军人证件', null, 'USER', ?)" +
                "     , (UUID(), '3', '士兵证', null, 'USER', ?)" +
                "     , (UUID(), '4', '文职干部证', null, 'USER', ?)" +
                "     , (UUID(), '5', '部队离退休证', null, 'USER', ?)" +
                "     , (UUID(), '6', '香港特区护照/身份证明', null, 'USER', ?)" +
                "     , (UUID(), '7', '澳门特区护照/身份证明', null, 'USER', ?)" +
                "     , (UUID(), '8', '台湾居民来往大陆通行证', null, 'USER', ?)" +
                "     , (UUID(), '9', '境外永久居住证', null, 'USER', ?)" +
                "     , (UUID(), 'A', '护照', null, 'USER', ?)" +
                "     , (UUID(), 'B', '户口薄', null, 'USER', ?)" +
                "     , (UUID(), 'Z', '其他证件', null, 'USER', ?);";

        jdbcSSOAPI.update(sql, preparedStatement -> {
            preparedStatement.setObject(1, domain);
            preparedStatement.setObject(2, domain);
            preparedStatement.setObject(3, domain);
            preparedStatement.setObject(4, domain);
            preparedStatement.setObject(5, domain);
            preparedStatement.setObject(6, domain);
            preparedStatement.setObject(7, domain);
            preparedStatement.setObject(8, domain);
            preparedStatement.setObject(9, domain);
            preparedStatement.setObject(10, domain);
            preparedStatement.setObject(11, domain);
            preparedStatement.setObject(12, domain);
        });

    }

}
