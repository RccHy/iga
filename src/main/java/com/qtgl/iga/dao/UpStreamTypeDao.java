package com.qtgl.iga.dao;


import com.qtgl.iga.bo.UpStream;
import com.qtgl.iga.bo.UpStreamType;

import java.util.List;
import java.util.Map;

public interface UpStreamTypeDao {

    List<UpStreamType> findAll(Map<String, Object> arguments, String domain);

    UpStreamType saveUpStreamType(UpStreamType upStreamType, String domain);

    UpStreamType deleteUpStreamType(Map<String, Object> arguments, String domain) throws Exception;

    UpStreamType updateUpStreamType(UpStreamType upStreamType);
}
