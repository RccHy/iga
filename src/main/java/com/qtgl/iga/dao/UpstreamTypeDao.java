package com.qtgl.iga.dao;


import com.qtgl.iga.bo.UpstreamType;
import com.qtgl.iga.bo.UpstreamTypeField;
import com.qtgl.iga.vo.UpstreamTypeVo;

import java.util.List;
import java.util.Map;

public interface UpstreamTypeDao {

    List<UpstreamTypeVo> findAll(Map<String, Object> arguments, String domain,Boolean isLocal);

    UpstreamType saveUpstreamType(UpstreamType upstreamType, String domain);

    UpstreamType deleteUpstreamType(String id, String domain) throws Exception;

    UpstreamType updateUpstreamType(UpstreamType upstreamType) throws Exception;

    List<UpstreamType> findByUpstreamId(String id);

    UpstreamType findById(String id);

    UpstreamType findByCode(String code);

    List<UpstreamTypeField> findFields(String upstreamId);

    Integer deleteByUpstreamId(String id, String domain);

    List<UpstreamType> findByUpstreamIdAndDescription(UpstreamType upstreamType,String domain);

    List<UpstreamType> findByUpstreamIds(List<String> ids,String domain);
    void deleteUpstreamTypeByCods(List<String> codes,String domain);
}
