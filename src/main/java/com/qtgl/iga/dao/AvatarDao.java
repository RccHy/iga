package com.qtgl.iga.dao;

import com.qtgl.iga.bo.Avatar;

import java.util.List;
import java.util.Map;

public interface AvatarDao {
    List<Avatar> findAll(String tenantId);

    Integer saveToSso(Map<String, List<Avatar>> avatarResult, String tenantId);

    Integer saveToSso(List<Avatar> avatarList, String tenantId);
}
