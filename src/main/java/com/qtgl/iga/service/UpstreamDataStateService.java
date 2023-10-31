package com.qtgl.iga.service;


import com.qtgl.iga.bean.OccupyConnection;
import com.qtgl.iga.bean.PersonConnection;

import java.util.Map;

public interface UpstreamDataStateService {



    PersonConnection personUpstreamDataState(Map<String, Object> arguments, String domain);
    OccupyConnection occupyUpstreamDataState(Map<String, Object> arguments, String domain);

}
