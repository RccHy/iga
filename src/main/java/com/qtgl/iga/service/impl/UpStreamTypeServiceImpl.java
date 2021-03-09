package com.qtgl.iga.service.impl;

import com.qtgl.iga.bo.UpStream;
import com.qtgl.iga.bo.UpStreamType;
import com.qtgl.iga.dao.UpStreamDao;
import com.qtgl.iga.dao.UpStreamTypeDao;
import com.qtgl.iga.service.UpStreamService;
import com.qtgl.iga.service.UpStreamTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class UpStreamTypeServiceImpl implements UpStreamTypeService {

    @Autowired
    UpStreamTypeDao upStreamTypeDao;

    @Override
    public List<UpStreamType> findAll(Map<String, Object> arguments, String domain) {
        return upStreamTypeDao.findAll(arguments, domain);
    }

    @Override
    public UpStreamType deleteUpStreamType(Map<String, Object> arguments, String domain) throws Exception {
        return upStreamTypeDao.deleteUpStreamType(arguments, domain);
    }

    @Override
    public UpStreamType saveUpStreamType(UpStreamType upStreamType, String domain) {
        return upStreamTypeDao.saveUpStreamType(upStreamType, domain);
    }

    @Override
    public UpStreamType updateUpStreamType(UpStreamType upStreamType) {
        return upStreamTypeDao.updateUpStreamType(upStreamType);
    }

}
