package com.qtgl.iga.service.impl;

import com.qtgl.iga.bean.OccupyDto;
import com.qtgl.iga.dao.UserLogDao;
import com.qtgl.iga.service.UserLogService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;

@Service
public class UserLogServiceImpl implements UserLogService {
    @Resource
    UserLogDao userLogDao;

    @Override
    public void saveUserLog(ArrayList<OccupyDto> occupyDtos, String tenantId) {
        userLogDao.saveUserLog(occupyDtos, tenantId);
    }
}
