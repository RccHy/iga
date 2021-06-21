package com.qtgl.iga.service.impl;


import com.qtgl.iga.bo.NodeRules;
import com.qtgl.iga.bo.UpstreamType;
import com.qtgl.iga.bo.UpstreamTypeField;
import com.qtgl.iga.dao.NodeRulesDao;
import com.qtgl.iga.dao.UpstreamDao;
import com.qtgl.iga.dao.UpstreamTypeDao;
import com.qtgl.iga.service.UpstreamTypeService;
import com.qtgl.iga.utils.DataBusUtil;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import com.qtgl.iga.vo.UpstreamTypeVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class UpstreamTypeServiceImpl implements UpstreamTypeService {

    @Autowired
    UpstreamTypeDao upstreamTypeDao;
    @Autowired
    NodeRulesDao nodeRulesDao;
    @Autowired
    UpstreamDao upstreamDao;
    @Autowired
    DataBusUtil dataBusUtil;
    public static Logger logger = LoggerFactory.getLogger(UpstreamTypeServiceImpl.class);

    @Override
    public List<UpstreamTypeVo> findAll(Map<String, Object> arguments, String domain) {
        return upstreamTypeDao.findAll(arguments, domain);
    }

    @Override
    public UpstreamType deleteUpstreamType(Map<String, Object> arguments, String domain) throws Exception {
        //查看是否有关联node_rules
        List<NodeRules> nodeRules = nodeRulesDao.findNodeRulesByUpStreamTypeId((String) arguments.get("id"), null);
        if (null != nodeRules && nodeRules.size() > 0) {
            throw new CustomException(ResultCode.FAILED, "删除权威源类型失败,有绑定的node规则,请查看后再删除");
        }
        List<NodeRules> oldRules = nodeRulesDao.findNodeRulesByUpStreamTypeId((String) arguments.get("id"), 2);
        if (null != oldRules && oldRules.size() > 0) {
            throw new CustomException(ResultCode.FAILED, "删除权威源类型失败,有绑定的历史node规则");
        }
        return upstreamTypeDao.deleteUpstreamType((String) arguments.get("id"), domain);
    }

    @Override
    public UpstreamType saveUpstreamType(UpstreamType upstreamType, String domain) throws Exception {
        if (null == upstreamType.getUpstreamId()) {
            throw new CustomException(ResultCode.FAILED, "请选择或先添加权威源");
        }
        //校验名称重复
        List<UpstreamType> upstreamTypeList = upstreamTypeDao.findByUpstreamIdAndDescription(upstreamType);
        if(null!=upstreamTypeList&&upstreamTypeList.size()>0){
            throw new CustomException(ResultCode.FAILED,"权威源类型描述重复");
        }
        return upstreamTypeDao.saveUpstreamType(upstreamType, domain);
    }

    @Override
    public UpstreamType updateUpstreamType(UpstreamType upstreamType) throws Exception {
        //查看是否有关联node_rules
        List<NodeRules> nodeRules = nodeRulesDao.findNodeRulesByUpStreamTypeId(upstreamType.getId(), null);
        if (null != nodeRules && nodeRules.size() > 0) {
            throw new CustomException(ResultCode.FAILED, "有绑定的node规则,请查看后再操作");
        }
        //校验名称重复
        List<UpstreamType> upstreamTypeList = upstreamTypeDao.findByUpstreamIdAndDescription(upstreamType);
        if(null!=upstreamTypeList&&upstreamTypeList.size()>0){
            throw new CustomException(ResultCode.FAILED,"权威源类型描述重复");
        }
        return upstreamTypeDao.updateUpstreamType(upstreamType);
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
//            throw new Exception("当前类型" + id.getDescription() + ",获取数据失败");
//            throw new CustomException(ResultCode.GET_DATA_ERROR, null, null, id.getDescription(), e.getMessage());
            throw e;
        }


    }

}
