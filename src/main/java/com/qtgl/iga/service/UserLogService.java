package com.qtgl.iga.service;

import com.qtgl.iga.bean.OccupyDto;

import java.util.ArrayList;

public interface UserLogService {
    void saveUserLog(ArrayList<OccupyDto> occupyDtos, String tenantId);
}
