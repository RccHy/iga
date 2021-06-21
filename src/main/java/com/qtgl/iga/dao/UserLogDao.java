package com.qtgl.iga.dao;


import com.qtgl.iga.bean.OccupyDto;

import java.util.ArrayList;

public interface UserLogDao {


    ArrayList<OccupyDto> saveUserLog(ArrayList<OccupyDto> list, String tenantId);

}
