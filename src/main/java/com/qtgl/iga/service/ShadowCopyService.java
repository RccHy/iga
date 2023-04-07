package com.qtgl.iga.service;

import com.alibaba.fastjson.JSONArray;
import com.qtgl.iga.bo.ShadowCopy;

public interface ShadowCopyService {

    ShadowCopy save(ShadowCopy shadowCopy);

    JSONArray findDataByUpstreamTypeAndType(String upstreamTypeId, String type,String domain);


}
