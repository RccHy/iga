package com.qtgl.iga.task;


import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.Tenant;
import com.qtgl.iga.dao.DeptDao;
import com.qtgl.iga.dao.PostDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class OrphanTask {


    @Resource
    DeptDao deptDao;
    @Resource
    PostDao postDao;
    @Resource
    OccupyOrphanTask occupyOrphanTask;

    public static ConcurrentHashMap<String, List<TreeBean>> deptResult = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<String, List<TreeBean>> postResult = new ConcurrentHashMap<>();

    /**
     * 定时检查孤儿节点
     */
    public void orphanTask(Tenant tenant) {
        log.info("租户:{}---------本次孤儿节点监控任务开始----------------", tenant.getDomain());


        //处理岗位
        this.handleUserType(tenant);

        //处理部门
        this.handleDept(tenant);

        //批量修改
        if (null != deptResult.get(tenant.getId()) && deptResult.get(tenant.getId()).size() > 0) {
            deptDao.updateDeptBatch(deptResult.get(tenant.getId()), tenant.getId());
        }
        if (null != postResult.get(tenant.getId()) && postResult.get(tenant.getId()).size() > 0) {
            postDao.updatePostBatch(postResult.get(tenant.getId()), tenant.getId());
        }


        log.info("租户:{}--------本次孤儿节点监控任务结束----------------", tenant.getDomain());
        occupyOrphanTask.occupyOrphanTask(tenant);

    }

    public void handleUserType(Tenant tenant) {
        //查询最新数据库数据
        List<TreeBean> userTypeList = postDao.findByTenantIdAndDelMarkIsFalse(tenant.getId());

        if (null != userTypeList && userTypeList.size() > 0) {

            //结果存储容器
            ConcurrentHashMap<String, TreeBean> result = new ConcurrentHashMap<>();
            //计数
            Integer count = 0;
            Map<String, TreeBean> collect = userTypeList.stream().collect(Collectors.toMap(TreeBean::getCode, userType -> userType));
            Map<String, List<TreeBean>> parentCollect = userTypeList.stream().collect(Collectors.groupingBy(TreeBean::getParentCode));
            for (TreeBean userType : userTypeList) {
                //过滤根节点
                if (StringUtils.isNotBlank(userType.getParentCode())) {
                    //查询是否为循环节点
                    if (collect.containsKey(userType.getParentCode())) {
                        TreeBean userTypeByParent = collect.get(userType.getParentCode());
                        if ((userTypeByParent.getParentCode()).equals(userType.getParentCode())) {
                            log.error("本次处理租户{}中,岗位数据{} 与 {} 循环依赖,跳过本次监控", tenant, userType, userTypeByParent);
                            break;
                        }
                    }
                    //孤儿节点恢复逻辑
                    if (1 == userType.getOrphan()) {
                        if (null != collect.get(userType.getParentCode()) && (0 == collect.get(userType.getParentCode()).getOrphan())) {
                            userType.setOrphan(0);
                            userType.setUpdateTime(LocalDateTime.now());
                            userType.setActive(1);
                            userType.setActiveTime(LocalDateTime.now());
                            result.put(userType.getCode(), userType);
                            collect.put(userType.getCode(), userType);
                            log.info("本次处理租户{}中,岗位中孤儿节点恢复{}", tenant.getId(), userType);
                        }
                    } else {
                        //孤儿判定逻辑
                        count += deptExtracted(result, collect, userType, tenant);
                        count = getInteger(result, count, collect, parentCollect, userType, tenant);
                    }

                }
            }
            postResult.put(tenant.getId(), new ArrayList<>(result.values()));
            log.info("本次处理租户{}中岗位孤儿节点共计{}个", tenant.getId(), count);
        } else {
            log.info("本次处理租户{}中没有有效的未删除岗位", tenant.getId());
        }
    }

    //private Integer getInteger(ConcurrentHashMap<String, UserType> result, Integer count, Map<String, UserType> collect, Map<String, List<UserType>> parentCollect, UserType userType) {
    //    if (null != parentCollect.get(userType.getUserType()) && parentCollect.get(userType.getUserType()).size() > 0) {
    //        for (UserType type : parentCollect.get(userType.getUserType())) {
    //            count += postExtracted(result, collect, type);
    //            getInteger(result, count, collect, parentCollect, type);
    //        }
    //
    //    }
    //    return count;
    //}

    //private int postExtracted(ConcurrentHashMap<String, UserType> result, Map<String, UserType> collect, UserType userType) {
    //    if (null == collect.get(userType.getParentCode()) || collect.get(userType.getParentCode()).getOrphan()) {
    //        userType.setOrphan(true);
    //        userType.setUpdateTime(new Date());
    //        userType.setActive(false);
    //        userType.setActiveTime(new Date());
    //        result.put(userType.getUserType(), userType);
    //        collect.put(userType.getUserType(), userType);
    //        log.info("本次处理租户{}中,岗位中孤儿节点{}", userType.getTenantId(), userType);
    //        return 1;
    //    }
    //    return 0;
    //}

    public void handleDept(Tenant tenant) {

        //查询最新数据库数据
        List<TreeBean> deptList = deptDao.findByTenantIdAndDelMarkIsFalse(tenant.getId());

        if (null != deptList && deptList.size() > 0) {
            //结果存储容器
            ConcurrentHashMap<String, TreeBean> result = new ConcurrentHashMap<>();
            Integer count = 0;
            Map<String, TreeBean> collect = deptList.stream().collect(Collectors.toMap(TreeBean::getCode, dept -> dept));
            Map<String, List<TreeBean>> parentCollect = deptList.stream().collect(Collectors.groupingBy(TreeBean::getParentCode));

            for (TreeBean dept : deptList) {
                //过滤根节点
                if (StringUtils.isNotBlank(dept.getParentCode())) {
                    //查询是否为循环节点
                    if (collect.containsKey(dept.getParentCode())) {
                        TreeBean deptByParent = collect.get(dept.getParentCode());
                        if ((deptByParent.getParentCode()).equals(dept.getCode())) {
                            log.error("本次处理租户{}中,部门数据{} 与 {} 循环依赖,跳过本次监控", tenant.getId(), dept, deptByParent);
                            break;
                        }
                    }
                    //孤儿节点恢复逻辑
                    if (1 == dept.getOrphan()) {
                        if (null != collect.get(dept.getParentCode()) && (0 == collect.get(dept.getParentCode()).getOrphan())) {
                            dept.setOrphan(0);
                            dept.setUpdateTime(LocalDateTime.now());
                            dept.setActive(1);
                            dept.setActiveTime(LocalDateTime.now());
                            //将处理的数据放入结果操作集
                            result.put(dept.getCode(), dept);
                            //更新操作缓存
                            collect.put(dept.getCode(), dept);
                            log.info("本次处理租户{}中,部门中孤儿节点恢复{}", tenant.getId(), dept);
                        }
                    } else {
                        //孤儿判定逻辑
                        count += deptExtracted(result, collect, dept, tenant);

                        count = getInteger(result, count, collect, parentCollect, dept, tenant);
                    }

                }
            }
            deptResult.put(tenant.getId(), new ArrayList<>(result.values()));
            log.info("本次处理租户{}中部门孤儿节点共计{}个", tenant.getId(), count);
        } else {

            log.info("本次处理租户{}中没有有效的未删除部门", tenant.getId());

        }

    }

    private Integer getInteger(ConcurrentHashMap<String, TreeBean> result, Integer count, Map<String, TreeBean> collect, Map<String, List<TreeBean>> parentCollect,
                               TreeBean dept, Tenant tenant) {
        if (null != parentCollect.get(dept.getCode()) && parentCollect.get(dept.getCode()).size() > 0) {
            for (TreeBean type : parentCollect.get(dept.getCode())) {
                count += deptExtracted(result, collect, type, tenant);
                count = getInteger(result, count, collect, parentCollect, type, tenant);
            }
        }
        return count;
    }

    private int deptExtracted(ConcurrentHashMap<String, TreeBean> result, Map<String, TreeBean> collect, TreeBean dept, Tenant tenant) {
        if ((null == collect.get(dept.getParentCode()) || 1 == collect.get(dept.getParentCode()).getOrphan())) {
            dept.setOrphan(1);
            dept.setUpdateTime(LocalDateTime.now());
            dept.setActive(0);
            dept.setActiveTime(LocalDateTime.now());
            result.put(dept.getCode(), dept);
            collect.put(dept.getCode(), dept);

            log.info("本次处理租户{}中,孤儿节点{}", tenant.getId(), dept);
            return 1;
        }
        return 0;
    }
}
