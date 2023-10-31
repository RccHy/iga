package com.qtgl.iga.service.impl;

import com.qtgl.iga.bean.OccupyConnection;
import com.qtgl.iga.bean.OccupyEdge;
import com.qtgl.iga.bean.PersonConnection;
import com.qtgl.iga.bean.PersonEdge;
import com.qtgl.iga.dao.OccupyDao;
import com.qtgl.iga.dao.PersonDao;
import com.qtgl.iga.service.UpstreamDataStateService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 获取 上游数据 通过治理后的结果数据。 上游数据+入库状态
 * @author cc
 */
@Service
public class UpstreamDataStateServiceImpl implements UpstreamDataStateService {

    @Resource
    PersonDao personDao;
    @Resource
    OccupyDao occupyDao;


    @Override
    public PersonConnection personUpstreamDataState(Map<String, Object> arguments, String domain) {
        Map<String, Object> args=new HashMap<>();
        args.putAll(arguments);
        PersonConnection personConnection = new PersonConnection();
        // 根据条件&分页查询
        List<PersonEdge> upstreamDataStatus = personDao.findUpstreamDataState(arguments, domain);
        //
        if (null != args.get("offset") && null != args.get("first")) {
            args.remove("offset");
            args.remove("first");
        }
        List<PersonEdge> countList = personDao.findUpstreamDataState(args, domain);
        personConnection.setEdges(upstreamDataStatus);
        personConnection.setTotalCount(countList.size());

        return personConnection;
    }

    @Override
    public OccupyConnection occupyUpstreamDataState(Map<String, Object> arguments, String domain) {
        OccupyConnection occupyConnection = new OccupyConnection();
        // 根据条件&分页查询
        List<OccupyEdge> upstreamDataStatus = occupyDao.findUpstreamDataState(arguments, domain);
        Integer count = occupyDao.findOccupyTempCount(arguments, domain);
        occupyConnection.setEdges(upstreamDataStatus);
        occupyConnection.setTotalCount(count);

        return occupyConnection;
    }
}
