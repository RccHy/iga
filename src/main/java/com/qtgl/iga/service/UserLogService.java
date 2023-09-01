package com.qtgl.iga.service;

import com.qtgl.iga.bean.OccupyDto;

import java.util.List;

public interface UserLogService {
    void saveUserLog(List<OccupyDto> occupyDtos, String tenantId);
}
