package com.qtgl.iga.service.impl;


import com.qtgl.iga.bean.UpstreamDto;
import com.qtgl.iga.bo.NodeRules;
import com.qtgl.iga.bo.Upstream;
import com.qtgl.iga.bo.UpstreamType;
import com.qtgl.iga.dao.NodeRulesDao;
import com.qtgl.iga.dao.UpstreamDao;
import com.qtgl.iga.dao.impl.UpstreamTypeDaoImpl;
import com.qtgl.iga.service.UpstreamService;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class UpstreamServiceImpl implements UpstreamService {

    @Autowired
    UpstreamDao upstreamDao;
    @Autowired
    UpstreamTypeDaoImpl upstreamTypeDao;
    @Autowired
    NodeRulesDao nodeRulesDao;

    @Override
    public List<Upstream> findAll(Map<String, Object> arguments, String domain) {
        return upstreamDao.findAll(arguments, domain);
    }

    @Override
    public Upstream deleteUpstream(Map<String, Object> arguments, String domain) throws Exception {
//        //查询权威源启用状态
//        ArrayList<Upstream> upstreamList = upstreamDao.getUpstreams((String) arguments.get("id"), domain);
//        if (null == upstreamList || upstreamList.size() > 1 || upstreamList.size() == 0) {
//            throw new RuntimeException("数据异常，删除失败");
//        }
//        Upstream upstream = upstreamList.get(0);
//        if (null != upstream.getActive() && upstream.getActive()) {
//            throw new RuntimeException("权威源已启用,不能进行删除操作");
//        }
        //查看是否有关联node_rules
        List<UpstreamType> byUpstreamId = upstreamTypeDao.findByUpstreamId((String) arguments.get("id"));
        if (null != byUpstreamId && byUpstreamId.size() > 0) {
            for (UpstreamType upstreamType : byUpstreamId) {
                List<NodeRules> nodeRules = nodeRulesDao.findNodeRulesByUpStreamTypeId(upstreamType.getId(), null);
                List<NodeRules> oldNodeRules = nodeRulesDao.findNodeRulesByUpStreamTypeId(upstreamType.getId(), 2);
                //编辑和正式的规则提示
                if (null != nodeRules && nodeRules.size() > 0) {
                    throw new CustomException(ResultCode.FAILED, "有绑定的nodeRules规则,请查看后再删除");
                }
                //历史版本,提示
                if (null != oldNodeRules && oldNodeRules.size() > 0) {
                    throw new CustomException(ResultCode.FAILED, "有历史版本的nodeRules规则");
                }

            }
        }


        //删除权威源数据类型
        if (null != byUpstreamId && byUpstreamId.size() > 0) {
            Integer integer = upstreamTypeDao.deleteByUpstreamId((String) arguments.get("id"), domain);
            if (integer < 0) {

                throw new CustomException(ResultCode.FAILED, "删除权威源类型失败");
            }

        }
        //删除权威源数据
        Integer flag = upstreamDao.deleteUpstream((String) arguments.get("id"));
        if (flag > 0) {
            return new Upstream();
        } else {
            throw new CustomException(ResultCode.FAILED, "删除权威源失败");
        }

    }

    @Override
    public Upstream saveUpstream(Upstream upstream, String domain) throws Exception {
        return upstreamDao.saveUpstream(upstream, domain);
    }

    @Override
    public Upstream updateUpstream(Upstream upstream) throws Exception {
//        if (null != upstream.getActive() && !upstream.getActive()) {
//            //判断类型是否都未启用
//            List<UpstreamType> byUpstreamId = upstreamTypeDao.findByUpstreamId(upstream.getId());
//            if (null != byUpstreamId && byUpstreamId.size() != 0) {
//                throw new Exception("权威源修改失败,请检查相关权威源类型状态");
//            }
//        }
        return upstreamDao.updateUpstream(upstream);
    }

    @Override
    public UpstreamDto saveUpstreamAndTypes(UpstreamDto upstreamDto, String id) throws Exception {
        // 添加权威源
        Upstream upstream = upstreamDao.saveUpstream(upstreamDto, id);
        UpstreamDto upstreamDb = new UpstreamDto(upstream);
        //添加权威源类型
        if (null == upstream) {
            throw new CustomException(ResultCode.ADD_UPSTREAM_ERROR, null, null, upstreamDto.getAppName());
        }
        ArrayList<UpstreamType> list = new ArrayList<>();
        List<UpstreamType> upstreamTypes = upstreamDto.getUpstreamTypes();
        if (null != upstreamTypes) {
            for (UpstreamType upstreamType : upstreamTypes) {
                upstreamType.setUpstreamId(upstream.getId());
                //校验名称重复
                List<UpstreamType> upstreamTypeList = upstreamTypeDao.findByUpstreamIdAndDescription(upstreamType);
                if (null != upstreamTypeList && upstreamTypeList.size() > 0) {
                    throw new CustomException(ResultCode.FAILED, "权威源类型描述重复");
                }
                UpstreamType upstreamTypeDb = upstreamTypeDao.saveUpstreamType(upstreamType, id);
                if (null != upstreamTypeDb) {
                    list.add(upstreamTypeDb);
                } else {
                    throw new CustomException(ResultCode.ADD_UPSTREAM_ERROR, null, null, upstreamDto.getAppName());
                }
            }
        }
        upstreamDb.setUpstreamTypes(list);

        return upstreamDb;
    }

    /**
     * @param arguments
     * @param id
     * @Description: 查询权威源及类型
     * @return: java.util.List<com.qtgl.iga.bean.UpstreamDto>
     */
    @Override
    public List<UpstreamDto> upstreamsAndTypes(Map<String, Object> arguments, String id) {
        ArrayList<UpstreamDto> upstreamDtos = new ArrayList<>();
        //查询权威源
        List<Upstream> upstreamList = upstreamDao.findAll(arguments, id);
        //查询权威源类型
        if (null != upstreamList && upstreamList.size() > 0) {
            for (Upstream upstream : upstreamList) {
                UpstreamDto upstreamDto = new UpstreamDto(upstream);
                List<UpstreamType> byUpstreamId = upstreamTypeDao.findByUpstreamId(upstream.getId());
                upstreamDto.setUpstreamTypes(byUpstreamId);
                upstreamDtos.add(upstreamDto);
            }
        }


        return upstreamDtos;
    }

    @Override
    public UpstreamDto updateUpstreamAndTypes(UpstreamDto upstreamDto) throws Exception {

//        // 是否禁用
//        if (null != upstreamDto.getActive() && !upstreamDto.getActive()) {
//            //判断类型是否都未启用
//            List<UpstreamType> byUpstreamId = upstreamTypeDao.findByUpstreamId(upstreamDto.getId());
//            if (null != byUpstreamId && byUpstreamId.size() != 0) {
//                throw new Exception("权威源操作失败,请检查相关权威源启用状态");
//            }
//        }

        //修改权威源类型
        //1.判断node绑定状态
        if (null != upstreamDto && null != upstreamDto.getUpstreamTypes() && upstreamDto.getUpstreamTypes().size() > 0) {
            for (UpstreamType upstreamType : upstreamDto.getUpstreamTypes()) {
                //查看是否有关联node_rules
                List<NodeRules> nodeRules = nodeRulesDao.findNodeRulesByUpStreamTypeId(upstreamType.getId(), null);
                if (null != nodeRules && nodeRules.size() > 0) {
                    throw new CustomException(ResultCode.FAILED, "有绑定的node规则,请查看后再操作");
                }
            }
        }

        Upstream upstream = upstreamDao.updateUpstream(upstreamDto);

        if (null != upstream) {
            UpstreamDto upstreamVo = new UpstreamDto(upstream);
            //修改权威源
            ArrayList<UpstreamType> list = new ArrayList<>();
            List<UpstreamType> upstreamTypes = upstreamDto.getUpstreamTypes();
            for (UpstreamType upstreamType : upstreamTypes) {
                UpstreamType upstreamResult = null;
                //校验名称重复
                List<UpstreamType> upstreamTypeList = upstreamTypeDao.findByUpstreamIdAndDescription(upstreamType);
                if (null != upstreamTypeList && upstreamTypeList.size() > 0) {
                    throw new CustomException(ResultCode.FAILED, "权威源类型描述重复");
                }
                if (null != upstreamType.getId()) {
                    upstreamResult = upstreamTypeDao.updateUpstreamType(upstreamType);
                } else {
                    upstreamResult = upstreamTypeDao.saveUpstreamType(upstreamType, upstreamDto.getDomain());
                }
                if (null != upstreamResult) {
                    list.add(upstreamResult);
                } else {
                    throw new CustomException(ResultCode.UPDATE_UPSTREAM_ERROR, null, null, upstreamDto.getAppCode());
                }
            }
            upstreamVo.setUpstreamTypes(list);
            return upstreamVo;
        } else {

            throw new CustomException(ResultCode.UPDATE_UPSTREAM_ERROR, null, null, upstreamDto.getAppCode());
        }

    }

}
