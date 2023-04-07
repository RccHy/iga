package com.qtgl.iga.dao;

import com.qtgl.iga.bo.ShadowCopy;

public interface ShadowCopyDao {

    ShadowCopy save(ShadowCopy shadowCopy);

    ShadowCopy findByUpstreamTypeAndType(String upstreamTypeId, String type, String domain);

    ShadowCopy findDataByUpstreamTypeAndType(String upstreamTypeId, String type, String domain);
}
