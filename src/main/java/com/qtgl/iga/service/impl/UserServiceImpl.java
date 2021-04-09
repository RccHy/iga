package com.qtgl.iga.service.impl;


import com.alibaba.druid.util.StringUtils;
import com.qtgl.iga.bean.UserBean;
import com.qtgl.iga.bo.CardType;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.Tenant;
import com.qtgl.iga.dao.CardTypeDao;
import com.qtgl.iga.dao.TenantDao;
import com.qtgl.iga.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {


    @Autowired
    CardTypeDao cardTypeDao;
    @Autowired
    TenantDao tenantDao;


    /**
     * todo 1: 循环遍历参与治理的所有上游源
     *      2： 拉取上游源数据并根据 证件类型+证件 号码进行和重，并验证证件类型对应的号码是否符合 正则表达式
     *      3： 如果有手动合重规则，运算手动合重规则【待确认】
     */
    @Override
    public void buildUser(DomainInfo domain) throws Exception {

        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        // 所有证件类型
        List<CardType> cardTypes = cardTypeDao.findAll(tenant.getId());
        Map<String, CardType> cardTypeMap = cardTypes.stream().collect(Collectors.toMap(CardType::getCardTypeCode, CardType -> CardType));

        // 自动根据 证件类型+证件号合重
        List<UserBean> userBeanList = new ArrayList<>();

        // 根据证件类型+证件号进行合重
        Map<String, UserBean> userBeanMap = new ConcurrentHashMap<>();
        if (null != userBeanList) {
            for (UserBean userBean : userBeanList) {
                if (StringUtils.isEmpty(userBean.getCardNum()) && StringUtils.isEmpty(userBean.getCardType())) {
                    log.error(userBean.getName() + "证件类型、号码为空");
                    continue;
                    //throw new Exception(userBean.getName() + "证件类型、号码为空");
                }
                if (cardTypeMap.containsKey(userBean.getCardType())) {
                    String cardTypeReg = cardTypeMap.get(userBean.getCardType()).getCardTypeReg();
                    if (!Pattern.matches(cardTypeReg, userBean.getCardNum())) {
                        log.error(userBean.getName() + "证件号码不符合规则");
                        continue;
                        //throw new Exception(userBean.getName() + "证件号码不符合规则");
                    }
                } else {
                    log.error(userBean.getName() + "证件类型无效");
                    continue;
                    //throw new Exception(userBean.getName() + "证件类型无效");
                }
                userBeanMap.put(userBean.getCardType() + ":" + userBean.getCardNum(), userBean);
            }
        }
        //将合重后的数据插入进数据库

    }
}
