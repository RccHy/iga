package com.qtgl.iga.dao;


import com.qtgl.iga.bo.UpStream;

import java.util.List;
import java.util.Map;

public interface UpStreamDao {

    List<UpStream> findAll(Map<String, Object> arguments,String domain);

    UpStream saveUpStream(UpStream upStream,String domain);

    UpStream deleteUpStream(Map<String, Object> arguments,String domain) throws Exception;

    UpStream updateUpStream(UpStream upStream);
}
