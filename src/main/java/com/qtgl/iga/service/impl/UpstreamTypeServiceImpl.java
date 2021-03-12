package com.qtgl.iga.service.impl;


import com.qtgl.iga.bo.UpstreamType;
import com.qtgl.iga.dao.UpstreamTypeDao;
import com.qtgl.iga.service.UpstreamTypeService;
import com.qtgl.iga.vo.UpstreamTypeVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class UpstreamTypeServiceImpl implements UpstreamTypeService {

    @Autowired
    UpstreamTypeDao upstreamTypeDao;

    @Override
    public List<UpstreamTypeVo> findAll(Map<String, Object> arguments, String domain) {
        return upstreamTypeDao.findAll(arguments, domain);
    }

    @Override
    public UpstreamType deleteUpstreamType(Map<String, Object> arguments, String domain) throws Exception {
        return upstreamTypeDao.deleteUpstreamType(arguments, domain);
    }

    @Override
    public UpstreamType saveUpstreamType(UpstreamType upstreamType, String domain) {
        return upstreamTypeDao.saveUpstreamType(upstreamType, domain);
    }

    @Override
    public UpstreamType updateUpstreamType(UpstreamType upstreamType) {
        return upstreamTypeDao.updateUpstreamType(upstreamType);
    }

}
