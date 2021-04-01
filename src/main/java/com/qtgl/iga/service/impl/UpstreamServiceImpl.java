package com.qtgl.iga.service.impl;


import com.qtgl.iga.bean.UpstreamDto;
import com.qtgl.iga.bo.Upstream;
import com.qtgl.iga.bo.UpstreamType;
import com.qtgl.iga.dao.UpstreamDao;
import com.qtgl.iga.dao.impl.UpstreamTypeDaoImpl;
import com.qtgl.iga.service.UpstreamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class UpstreamServiceImpl implements UpstreamService {

    @Autowired
    UpstreamDao upstreamDao;
    @Autowired
    UpstreamTypeDaoImpl upstreamTypeDao;

    @Override
    public List<Upstream> findAll(Map<String, Object> arguments, String domain) {
        return upstreamDao.findAll(arguments, domain);
    }

    @Override
    public Upstream deleteUpstream(Map<String, Object> arguments, String domain) throws Exception {
        return upstreamDao.deleteUpstream(arguments, domain);
    }

    @Override
    public Upstream saveUpstream(Upstream upstream, String domain) throws Exception {
        return upstreamDao.saveUpstream(upstream, domain);
    }

    @Override
    public Upstream updateUpstream(Upstream upstream) throws Exception {
        return upstreamDao.updateUpstream(upstream);
    }

    @Override
    public UpstreamDto saveUpstreamAndTypes(UpstreamDto upstreamDto, String id) throws Exception {
        // 添加上游源
        Upstream upstream = upstreamDao.saveUpstream(upstreamDto, id);
        UpstreamDto upstreamDb = new UpstreamDto(upstream);
        //添加上游源类型
        if (null == upstream) {
            throw new Exception("添加上游源失败" + upstreamDto.getAppName());
        }
        ArrayList<UpstreamType> list = new ArrayList<>();
        List<UpstreamType> upstreamTypes = upstreamDto.getUpstreamTypes();
        if (null != upstreamTypes) {
            for (UpstreamType upstreamType : upstreamTypes) {
                upstreamType.setUpstreamId(upstream.getId());
                UpstreamType upstreamTypeDb = upstreamTypeDao.saveUpstreamType(upstreamType, id);
                if (null != upstreamTypeDb) {
                    list.add(upstreamTypeDb);
                } else {
                    throw new Exception("添加上游源类型失败" + upstreamDto.getAppName());
                }
            }
        }
        upstreamDb.setUpstreamTypes(list);

        return upstreamDb;
    }

    @Override
    public List<UpstreamDto> upstreamsAndTypes(Map<String, Object> arguments, String id) {
        ArrayList<UpstreamDto> upstreamDtos = new ArrayList<>();
        //查询上游源
        List<Upstream> upstreamList = upstreamDao.findAll(arguments, id);
        //查询上游源类型
        for (Upstream upstream : upstreamList) {
            UpstreamDto upstreamDto = new UpstreamDto(upstream);
            List<UpstreamType> byUpstreamId = upstreamTypeDao.findByUpstreamId(upstream.getId());
            upstreamDto.setUpstreamTypes(byUpstreamId);
            upstreamDtos.add(upstreamDto);
        }

        return upstreamDtos;
    }

}
