package com.qtgl.iga.service.impl;

import com.qtgl.iga.bean.PersonConnection;
import com.qtgl.iga.bean.PersonEdge;
import com.qtgl.iga.dao.PersonDao;
import com.qtgl.iga.service.UpstreamDataStateService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
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


    @Override
    public PersonConnection personUpstreamDataState(Map<String, Object> arguments, String domain) {
        PersonConnection personConnection = new PersonConnection();
        // 根据条件&分页查询
        List<PersonEdge> upstreamDataStatus = personDao.findUpstreamDataState(arguments, domain);
        // 根据条件查询总数
        if (null != arguments.get("offset") && null != arguments.get("first")) {
            arguments.remove("offset");
            arguments.remove("first");
        }
        List<PersonEdge> countList = personDao.findUpstreamDataState(arguments, domain);
        personConnection.setEdges(upstreamDataStatus);
        personConnection.setTotalCount(countList.size());

        return personConnection;
    }
}
