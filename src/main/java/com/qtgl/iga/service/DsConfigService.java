package com.qtgl.iga.service;

import com.qtgl.iga.bo.DsConfig;

import java.util.List;

/**
 * <FileName> DsConfigService
 * <Desc>
 *
 * @author HP
 */
public interface DsConfigService {

    List<DsConfig> findAll();

    String getSyncWay(DsConfig dsConfig);
}
