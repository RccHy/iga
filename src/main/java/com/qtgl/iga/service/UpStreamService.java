package com.qtgl.iga.service;


import com.qtgl.iga.bo.UpStream;

import java.util.List;
import java.util.Map;

public interface UpStreamService {


    List<UpStream> findAll(Map<String, Object> arguments,String domain);

    UpStream deleteUpStream(Map<String, Object> arguments,String domain) throws Exception;

    UpStream saveUpStream(UpStream upStream,String domain);

    UpStream updateUpStream(UpStream upStream);
}
