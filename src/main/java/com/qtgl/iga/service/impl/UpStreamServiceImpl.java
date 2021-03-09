package com.qtgl.iga.service.impl;

import com.qtgl.iga.bo.UpStream;
import com.qtgl.iga.bo.UpStreamType;
import com.qtgl.iga.dao.UpStreamDao;
import com.qtgl.iga.dao.UpStreamTypeDao;
import com.qtgl.iga.service.UpStreamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class UpStreamServiceImpl implements UpStreamService {

    @Autowired
    UpStreamDao upStreamDao;

    @Override
    public List<UpStream> findAll(Map<String, Object> arguments, String domain) {
        return upStreamDao.findAll(arguments, domain);
    }

    @Override
    public UpStream deleteUpStream(Map<String, Object> arguments, String domain) throws Exception {
        return upStreamDao.deleteUpStream(arguments, domain);
    }

    @Override
    public UpStream saveUpStream(UpStream upStream, String domain) {
        return upStreamDao.saveUpStream(upStream, domain);
    }

    @Override
    public UpStream updateUpStream(UpStream upStream) {
        return upStreamDao.updateUpStream(upStream);
    }

}
