package com.qtgl.iga.service;

import com.qtgl.iga.bean.DeptBean;
import com.qtgl.iga.bo.DomainInfo;

import java.util.List;
import java.util.Map;

/**
 * <FileName> UserTypeService
 * <Desc>
 *
 * @author HP*/
public interface UserTypeService {
    List<DeptBean> findUserType( DomainInfo domain) throws Exception;
}
