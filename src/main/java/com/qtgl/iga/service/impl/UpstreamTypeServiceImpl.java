package com.qtgl.iga.service.impl;


import com.qtgl.iga.AutoUpRunner;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.dao.UpstreamTypeDao;
import com.qtgl.iga.service.*;
import com.qtgl.iga.utils.DataBusUtil;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import com.qtgl.iga.vo.UpstreamTypeVo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class UpstreamTypeServiceImpl implements UpstreamTypeService {

    @Resource
    UpstreamTypeDao upstreamTypeDao;
    @Resource
    NodeRulesService nodeRulesService;
    @Resource
    UpstreamService upstreamService;
    @Resource
    DataBusUtil dataBusUtil;
    @Resource
    NodeRulesRangeService nodeRulesRangeService;
    @Resource
    DeptTypeService deptTypeService;
    @Resource
    DeptTreeTypeService deptTreeTypeService;
    @Resource
    TaskLogService taskLogService;

    public static Logger logger = LoggerFactory.getLogger(UpstreamTypeServiceImpl.class);

    @Override
    public List<UpstreamTypeVo> findAll(Map<String, Object> arguments, String domain) {
        ArrayList<UpstreamTypeVo> resultUpstreamTypeVos = new ArrayList<>();

        //查询当前租户的权威源类型
        List<UpstreamTypeVo> upstreamTypeVos = upstreamTypeDao.findAll(arguments, domain,true);
        //查询超级租户的权威源类型
        if (!StringUtils.isBlank(AutoUpRunner.superDomainId)){
            List<UpstreamTypeVo> superUpstreamTypeVos = upstreamTypeDao.findAll(arguments,domain,false);
            if(!CollectionUtils.isEmpty(superUpstreamTypeVos)){
                dealWithUpstreamTypes(resultUpstreamTypeVos, superUpstreamTypeVos,false);

            }
        }

        if (!CollectionUtils.isEmpty(upstreamTypeVos)) {
            dealWithUpstreamTypes(resultUpstreamTypeVos, upstreamTypeVos,true);
        }

        return resultUpstreamTypeVos;
    }

    private void dealWithUpstreamTypes(ArrayList<UpstreamTypeVo> resultUpstreamTypeVos, List<UpstreamTypeVo> superUpstreamTypeVos,Boolean isLocal) {
        for (UpstreamTypeVo upstreamTypeVo : superUpstreamTypeVos) {
            upstreamTypeVo.setLocal(isLocal);
            //映射字段
            ArrayList<UpstreamTypeField> upstreamTypeFields = nodeRulesRangeService.getByUpstreamTypeId(upstreamTypeVo.getId());
            upstreamTypeVo.setUpstreamTypeFields(upstreamTypeFields);
            //权威源查询

            Upstream upstream = upstreamService.findById(upstreamTypeVo.getUpstreamId());
            if (null == upstream) {
                logger.error("权威源规则无有效权威源数据");
                throw new CustomException(ResultCode.FAILED, "权威源规则无对应有效权威源");
            }
            upstreamTypeVo.setUpstream(upstream);
            //source赋值
            upstreamTypeVo.setSource(upstream.getAppName() + "(" + upstream.getAppCode() + ")");
            //组织机构类型查询
            DeptType deptType = deptTypeService.findById(upstreamTypeVo.getDeptTypeId());
            upstreamTypeVo.setDeptType(deptType);

            //组织机构类型树查询
            DeptTreeType byId = deptTreeTypeService.findById(upstreamTypeVo.getDeptTreeTypeId());
            upstreamTypeVo.setDeptTreeType(byId);
            //发布状态的nodeRules 通过同步方式,serviceKey以及发布状态获取nodeRule
            List<NodeRules> nodeRules = nodeRulesService.findNodeRulesByServiceKey(upstreamTypeVo.getId(), 0, upstreamTypeVo.getSynWay());

            if (!CollectionUtils.isEmpty(nodeRules)) {
                upstreamTypeVo.setNodeRules(nodeRules);
                upstreamTypeVo.setHasRules(true);
            } else {
                upstreamTypeVo.setHasRules(false);
            }
            resultUpstreamTypeVos.add(upstreamTypeVo);
        }
    }


    @Override
    public UpstreamType deleteUpstreamType(Map<String, Object> arguments, String domain) throws Exception {
        //查看是否有关联node_rules
        List<NodeRules> nodeRules = nodeRulesService.findNodeRulesByUpStreamTypeId((String) arguments.get("id"), null);
        if (null != nodeRules && nodeRules.size() > 0) {
            throw new CustomException(ResultCode.FAILED, "删除权威源类型失败,有绑定的node规则,请查看后再删除");
        }
        List<NodeRules> oldRules = nodeRulesService.findNodeRulesByUpStreamTypeId((String) arguments.get("id"), 2);
        if (null != oldRules && oldRules.size() > 0) {
            //删除历史版本
            nodeRulesService.deleteBatchRules(oldRules, domain);
            //throw new CustomException(ResultCode.FAILED, "删除权威源类型失败,有绑定的历史node规则");
        }
        return upstreamTypeDao.deleteUpstreamType((String) arguments.get("id"), domain);
    }

    @Override
    public UpstreamType saveUpstreamType(UpstreamType upstreamType, String domain) {
        if (null == upstreamType.getUpstreamId()) {
            throw new CustomException(ResultCode.FAILED, "请选择或先添加权威源");
        }
        //校验名称重复
        List<UpstreamType> upstreamTypeList = upstreamTypeDao.findByUpstreamIdAndDescription(upstreamType,domain);
        if (null != upstreamTypeList && upstreamTypeList.size() > 0) {
            throw new CustomException(ResultCode.FAILED, "权威源类型描述重复");
        }
        return upstreamTypeDao.saveUpstreamType(upstreamType, domain);
    }

    @Override
    public UpstreamType updateUpstreamType(UpstreamType upstreamType) throws Exception {
        ////查看是否有关联node_rules
        //List<NodeRules> nodeRules = nodeRulesDao.findNodeRulesByUpStreamTypeId(upstreamType.getId(), null);
        //if (null != nodeRules && nodeRules.size() > 0) {
        //    throw new CustomException(ResultCode.FAILED, "有绑定的node规则,请查看后再操作");
        //}
        //查询是否在同步中

        List<TaskLog> logList = taskLogService.findByStatus(upstreamType.getDomain());
        if (null != logList && logList.size() > 0) {
            if ("doing".equals(logList.get(0).getStatus())) {
                throw new CustomException(ResultCode.FAILED, "数据正在同步,修改权威源类型配置失败,请稍后再试");

            }

        }
        //校验名称重复
        List<UpstreamType> upstreamTypeList = upstreamTypeDao.findByUpstreamIdAndDescription(upstreamType,upstreamType.getDomain());
        if (null != upstreamTypeList && upstreamTypeList.size() > 0) {
            throw new CustomException(ResultCode.FAILED, "权威源类型描述重复");
        }
        return upstreamTypeDao.updateUpstreamType(upstreamType);
    }

    @Override
    public UpstreamType findByCode(String code) {
        return upstreamTypeDao.findByCode(code);
    }

    @Override
    public List<UpstreamTypeField> findFields(String url) {
        return upstreamTypeDao.findFields(url);
    }

    @Override
    public HashMap<Object, Object> upstreamTypesData(Map<String, Object> arguments, String domainName) throws Exception {
        UpstreamType id = null;
        try {
            id = upstreamTypeDao.findById((String) arguments.get("id"));
            HashMap<Object, Object> map = new HashMap<>();
            map.put("data", dataBusUtil.getDataByBus(id, domainName));
            return map;
        } catch (CustomException e) {
            logger.error("当前类型{}中的{},获取数据失败", id.getDescription(), e.getMessage());
            throw e;
        }


    }

    @Override
    public List<UpstreamType> findByUpstreamIds(List<String> ids, String domain) {
        return upstreamTypeDao.findByUpstreamIds(ids, domain);
    }

    @Override
    public void deleteUpstreamTypeByCods(List<String> codes, String domain) {
        upstreamTypeDao.deleteUpstreamTypeByCods(codes, domain);
    }

}

