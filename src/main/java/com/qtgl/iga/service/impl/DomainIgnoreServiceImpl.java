package com.qtgl.iga.service.impl;

import com.qtgl.iga.bo.DomainIgnore;
import com.qtgl.iga.dao.DomainIgnoreDao;
import com.qtgl.iga.service.DomainIgnoreService;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.relational.core.sql.In;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;

@Service
@Slf4j
public class DomainIgnoreServiceImpl implements DomainIgnoreService {
    @Resource
    DomainIgnoreDao domainIgnoreDao;

    @Override
    public List<DomainIgnore> findByDomain(String domainId) {
        return domainIgnoreDao.findByDomain(domainId);
    }

    @Override
    public DomainIgnore save(DomainIgnore domainIgnore) {
        //校验重复
        List<DomainIgnore> ignores = domainIgnoreDao.findByParam(domainIgnore);
        if (CollectionUtils.isEmpty(ignores)) {
            return domainIgnoreDao.save(domainIgnore);

        }
        return null;
    }

    @Override
    public DomainIgnore recoverUpstreamOrRule(DomainIgnore domainIgnore, String domainId) {
        if (null != domainIgnore) {
            if (null != domainIgnore.getUpstreamId()) {
                return domainIgnoreDao.deleteByUpstreamIdAndDomain(domainIgnore.getUpstreamId(), domainId);
            } else if (null != domainIgnore.getNodeRuleId()) {
                return domainIgnoreDao.deleteByNodeRuleIdAndDomain(domainIgnore.getNodeRuleId(), domainId);
            } else {
                throw new CustomException(ResultCode.FAILED, "不存在传入标识,请检查参数");
            }
        } else {
            throw new CustomException(ResultCode.FAILED, "不存在传入标识,请检查参数");
        }
    }

    @Override
    public Integer deleteByUpstreamId(String upstreamId) {
        return domainIgnoreDao.deleteByUpstreamId(upstreamId);
    }
}
