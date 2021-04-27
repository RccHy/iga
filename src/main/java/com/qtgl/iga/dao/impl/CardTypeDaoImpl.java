package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.CardType;
import com.qtgl.iga.dao.CardTypeDao;
import com.qtgl.iga.utils.MyBeanUtils;
import org.apache.commons.beanutils.BeanUtils;
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
    public List<CardType> findAll(String domain) {
        String sql = "select id, card_type_name as cardTypeName, card_type_code as cardTypeCode, card_type_reg as cardTypeReg  from card_type where tenant_id=?";
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
}
