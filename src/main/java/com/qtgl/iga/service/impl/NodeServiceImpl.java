package com.qtgl.iga.service.impl;

import com.qtgl.iga.bo.Node;
import com.qtgl.iga.dao.NodeDao;
import com.qtgl.iga.service.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional

public class NodeServiceImpl implements NodeService {



    @Autowired
    NodeDao nodeDao;

    @Override
    public Node save(Node node) {
        return null;
    }

    @Override
    public Node getRoot(String domain) {
        return nodeDao.getByCode(domain,"");
    }

    @Override
    public Node getByCode(String domain,String nodeCode) {
        return nodeDao.getByCode(domain,nodeCode );
    }



}
