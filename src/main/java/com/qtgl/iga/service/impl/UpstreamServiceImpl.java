package com.qtgl.iga.service.impl;


import com.qtgl.iga.bo.Upstream;
import com.qtgl.iga.dao.UpstreamDao;
import com.qtgl.iga.service.UpstreamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class UpstreamServiceImpl implements UpstreamService {

    @Autowired
    UpstreamDao upstreamDao;

    @Override
    public List<Upstream> findAll(Map<String, Object> arguments, String domain) {
        return upstreamDao.findAll(arguments, domain);
    }

    @Override
    public Upstream deleteUpstream(Map<String, Object> arguments, String domain) throws Exception {
        return upstreamDao.deleteUpstream(arguments, domain);
    }

    @Override
    public Upstream saveUpstream(Upstream upstream, String domain) {
        return upstreamDao.saveUpstream(upstream, domain);
    }

    @Override
    public Upstream updateUpstream(Upstream upstream) {
        return upstreamDao.updateUpstream(upstream);
    }

}
