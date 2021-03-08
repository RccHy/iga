package com.qtgl.iga.service;


import com.qtgl.iga.bo.UpStream;

import java.util.List;
import java.util.Map;

public interface UpStreamService {


    List<UpStream> findAll(Map<String, Object> arguments);

    UpStream deleteUpStream(Map<String, Object> arguments) throws Exception;

    UpStream saveUpStream(UpStream upStream);

    UpStream updateUpStream(UpStream upStream);
}
