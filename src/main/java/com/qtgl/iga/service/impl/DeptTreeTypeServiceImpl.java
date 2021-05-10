package com.qtgl.iga.service.impl;

import com.qtgl.iga.bo.DeptTreeType;
import com.qtgl.iga.bo.Node;
import com.qtgl.iga.dao.DeptTreeTypeDao;
import com.qtgl.iga.dao.NodeDao;
import com.qtgl.iga.service.DeptTreeTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class DeptTreeTypeServiceImpl implements DeptTreeTypeService {

    @Autowired
    DeptTreeTypeDao deptTreeTypeDao;
    @Autowired
    NodeDao nodeDao;

    /**
     * @param arguments
     * @param domain
     * @Description: 获取所有组织机构树
     * @return: java.util.List<com.qtgl.iga.bo.DeptTreeType>
     */
    @Override
    public List<DeptTreeType> findAll(Map<String, Object> arguments, String domain) {
        return deptTreeTypeDao.findAll(arguments, domain);
    }

    /**
     * @param arguments
     * @param domain
     * @Description: 根据租户删除组织机构树
     * @return: com.qtgl.iga.bo.DeptTreeType
     */
    @Override
    public DeptTreeType deleteDeptTreeType(Map<String, Object> arguments, String domain) throws Exception {
        //查询是否有绑定的node
        List<Node> nodeList = nodeDao.findByTreeTypeId((String) arguments.get("id"), 0);
        if (null != nodeList && nodeList.size() > 0) {
            throw new Exception("删除组织机构树类型失败,有绑定的node,请查看后再删除");
        }
        return deptTreeTypeDao.deleteDeptTreeType((String) arguments.get("id"), domain);
    }

    /**
     * @param deptTreeType
     * @param domain
     * @Description: 保存组织机构树
     * @return: com.qtgl.iga.bo.DeptTreeType
     */
    @Override
    public DeptTreeType saveDeptTreeType(DeptTreeType deptTreeType, String domain) throws Exception {
        return deptTreeTypeDao.saveDeptTreeType(deptTreeType, domain);
    }

    /**
     * @param deptTreeType
     * @Description: 修改组织机构树
     * @return: com.qtgl.iga.bo.DeptTreeType
     */
    @Override
    public DeptTreeType updateDeptTreeType(DeptTreeType deptTreeType) throws Exception {
        return deptTreeTypeDao.updateDeptTreeType(deptTreeType);
    }

}
