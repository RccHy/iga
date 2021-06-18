package com.qtgl.iga.dao;


import com.qtgl.iga.bo.UserLog;

import java.util.ArrayList;

public interface UserLogDao {


    ArrayList<UserLog> saveUserLog(ArrayList<UserLog> list, String tenantId);

}
