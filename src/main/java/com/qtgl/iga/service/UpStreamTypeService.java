package com.qtgl.iga.service;


import com.qtgl.iga.bo.UpStream;
import com.qtgl.iga.bo.UpStreamType;

import java.util.List;
import java.util.Map;

public interface UpStreamTypeService {


    List<UpStreamType> findAll(Map<String, Object> arguments, String domain);

    UpStreamType deleteUpStreamType(Map<String, Object> arguments, String domain) throws Exception;

    UpStreamType saveUpStreamType(UpStreamType upStreamType, String domain);

    UpStreamType updateUpStreamType(UpStreamType upStreamType);
}
