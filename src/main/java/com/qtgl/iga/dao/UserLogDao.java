package com.qtgl.iga.dao;


import com.qtgl.iga.bean.OccupyDto;

import java.util.List;

public interface UserLogDao {


    List<OccupyDto> saveUserLog(List<OccupyDto> list, String tenantId);

}
