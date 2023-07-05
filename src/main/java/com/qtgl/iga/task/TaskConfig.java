package com.qtgl.iga.task;

import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.OccupyDto;
import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.config.TaskThreadPool;
import com.qtgl.iga.service.*;
import com.qtgl.iga.utils.DataBusUtil;
import com.qtgl.iga.utils.FileUtil;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;


@Slf4j
@Configuration
public class TaskConfig {


    @Value("${iga.hostname}")
    String hostname;

    @Resource
    DomainInfoService domainInfoService;
    @Resource
    DeptService deptService;
    @Resource
    PersonService personService;
    @Resource
    PostService postService;
    @Resource
    OccupyService occupyService;
    @Resource
    TaskLogService taskLogService;
    @Resource
    NodeRulesService nodeRulesService;
    @Resource
    DataBusUtil dataBusUtil;
    @Resource
    FileUtil fileUtil;
    @Resource
    DsConfigService dsConfigService;
    @Resource
    TenantService tenantService;
    @Resource
    OrphanTask orphanTask;

    public static Map<String, String> errorData = new HashMap<>();

    public static final String SYNC_WAY_BRIDGE = "BRIDGE";
    public static final String SYNC_WAY_ENTERPRISE = "ENTERPRISE";
    public static final String SYNC_WAY_ENTERPRISE_GRAPHQL = "ENTERPRISE_GRAPHQL";

    /**
     * 根据租户区分线程池
     * 串行执行 部门、岗位、人员、三元组 同步
     */
    @Scheduled(cron = "${task.cron}")
    public void task() {
        try {
            // k8环境多节点部署环境下 仅01节点会执行定时任务
            if (hostname.contains("-0")) {
                executeTask(null);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void executeTask(DomainInfo domain) {
        List<DomainInfo> domainInfos = new ArrayList<>();
        if (null != domain) {
            domainInfos.add(domain);
        } else {
            domainInfos = domainInfoService.findAll();
        }
        List<DsConfig> dsConfigs = dsConfigService.findAll();
        Map<String, DsConfig> collect = null;
        if (!CollectionUtils.isEmpty(dsConfigs)) {
            collect = dsConfigs.stream().collect(Collectors.toMap(DsConfig::getTenantId, v -> v));
        }
        if (CollectionUtils.isEmpty(domainInfos)) {
            throw new CustomException(ResultCode.FAILED, "当前租户为空,请配置对应租户");
        }
        for (DomainInfo domainInfo : domainInfos) {
            ExecutorService executorService = TaskThreadPool.executorServiceMap.computeIfAbsent(domainInfo.getDomainName(), k -> TaskThreadPool.builderExecutor(k));
            Map<String, DsConfig> finalCollect = collect;
            executorService.execute(() -> {
                //获取最近三次同步状态,均为失败则不同步,其余情况正常同步
                Boolean aBoolean = taskLogService.checkTaskStatus(domainInfo.getId());
                // 如果 获取最近一次同步任务状况
                TaskLog lastTaskLog = taskLogService.last(domainInfo.getId());
                //  最近一次同步任务 状态成功后才能继续同步
                //if ((null == lastTaskLog) || (null != lastTaskLog.getId() && !lastTaskLog.getStatus().equals("failed"))) {

                if (aBoolean) {
                    errorData.remove(domainInfo.getId());
                    Tenant tenant = tenantService.findByDomainName(domainInfo.getDomainName());

                    //运行孤儿监控
                    orphanTask.orphanTask(tenant);

                    //判断sso是否有定时任务
                    if (!CollectionUtils.isEmpty(dsConfigs) && finalCollect.containsKey(tenant.getId())) {
                        DsConfig dsConfig = finalCollect.get(tenant.getId());
                        String syncWay = dsConfigService.getSyncWay(dsConfig);
                        if (StringUtils.isNotBlank(syncWay) && (syncWay.equals(SYNC_WAY_BRIDGE) || syncWay.equals(SYNC_WAY_ENTERPRISE) || syncWay.equals(SYNC_WAY_ENTERPRISE_GRAPHQL))) {
                            log.info("{}sso正在进行同步,跳过本次同步任务", domainInfo.getId());
                            TaskLog taskLog = new TaskLog();
                            if (null != domain) {
                                taskLog.setSynWay(1);
                            }
                            taskLog.setId(UUID.randomUUID().toString());
                            taskLog.setReason("sso开启了同步配置，请先关闭！");
                            taskLogService.save(taskLog, domainInfo.getId(), "skip-error");
                            return;
                        }
                    }
                    // 如果有编辑中的规则，则不进行数据同步
                    Map<String, Object> arguments = new HashMap<>();
                    arguments.put("status", 1);
                    arguments.put("type", 1);
                    final List<NodeRules> nodeRules = nodeRulesService.findNodeRules(arguments, domainInfo.getId());
                    TaskLog taskLog = new TaskLog();
                    if (null != domain) {
                        taskLog.setSynWay(1);
                    }
                    taskLog.setId(UUID.randomUUID().toString());
                    // 如果有编辑中的规则，则不进行数据同步 &&
                    if ((null == nodeRules || nodeRules.size() == 0)) {
                        try {
                            log.info("{}开始同步,task:{}", domainInfo.getDomainName(), taskLog.getId());
                            taskLogService.save(taskLog, domainInfo.getId(), "save");
                            //部门数据同步至sso
                            Map<TreeBean, String> deptResult = deptService.buildDeptUpdateResult(domainInfo, lastTaskLog, taskLog, null);
                            Map<String, List<Map.Entry<TreeBean, String>>> deptResultMap = deptResult.entrySet().stream().collect(Collectors.groupingBy(Map.Entry::getValue));
                            //处理数据
                            Integer recoverDept = deptResultMap.containsKey("recover") ? deptResultMap.get("recover").size() : 0;
                            Integer insertDept = (deptResultMap.containsKey("insert") ? deptResultMap.get("insert").size() : 0) + recoverDept;
                            Integer deleteDept = deptResultMap.containsKey("delete") ? deptResultMap.get("delete").size() : 0;
                            Integer updateDept = (deptResultMap.containsKey("update") ? deptResultMap.get("update").size() : 0);
                            Integer invalidDept = deptResultMap.containsKey("invalid") ? deptResultMap.get("invalid").size() : 0;
                            String deptNo = insertDept + "/" + deleteDept + "/" + updateDept + "/" + invalidDept;

                            log.info(Thread.currentThread().getName() + ": 部门同步完成：{}==={}", deptNo, System.currentTimeMillis());
                            taskLog.setStatus("doing");
                            taskLog.setDeptNo(deptNo);
                            taskLogService.save(taskLog, domainInfo.getId(), "update");
                            // PUT   MQ
                            String pubResult = "";
                            if (deptResult.size() > 0) {
                                pubResult = dataBusUtil.pub(deptResult, null, null, "dept", domainInfo);
                                log.info("dept pub:{}", pubResult);
                            }


                            //=============岗位数据同步至sso=================
                            final Map<TreeBean, String> postResult = postService.buildPostUpdateResult(domainInfo, lastTaskLog, taskLog, null);
                            Map<String, List<Map.Entry<TreeBean, String>>> postResultMap = postResult.entrySet().stream().collect(Collectors.groupingBy(Map.Entry::getValue));
                            Integer recoverPost = postResultMap.containsKey("recover") ? postResultMap.get("recover").size() : 0;
                            Integer insertPost = (postResultMap.containsKey("insert") ? postResultMap.get("insert").size() : 0) + recoverPost;
                            Integer deletePost = postResultMap.containsKey("delete") ? postResultMap.get("delete").size() : 0;
                            Integer updatePost = (postResultMap.containsKey("update") ? postResultMap.get("update").size() : 0);
                            Integer invalidPost = postResultMap.containsKey("invalid") ? postResultMap.get("invalid").size() : 0;
                            String postNo = insertPost + "/" + deletePost + "/" + updatePost + "/" + invalidPost;
                            log.info(Thread.currentThread().getName() + ": 岗位同步完成：{}==={}", postNo, System.currentTimeMillis());
                            taskLog.setPostNo(postNo);
                            taskLogService.save(taskLog, domainInfo.getId(), "update");

                            // PUT   MQ
                            if (postResult.size() > 0) {
                                pubResult = dataBusUtil.pub(postResult, null, null, "post", domainInfo);
                                log.info("post pub:{}", pubResult);
                            }


                            //=============人员数据同步至sso=============
                            Map<String, List<Person>> personResult = personService.buildPerson(domainInfo, lastTaskLog, taskLog, null);
                            Integer countPerson = 0;
                            if (null == personResult) {
                                log.error("无人员管理规则，不进行人员同步");
                                taskLog.setPersonNo("--/--/--/--");
                            } else {
                                Integer insertPerson = (personResult.containsKey("insert") ? personResult.get("insert").size() : 0);
                                Integer deletePerson = personResult.containsKey("delete") ? personResult.get("delete").size() : 0;
                                Integer updatePerson = (personResult.containsKey("update") ? personResult.get("update").size() : 0);
                                Integer invalidPerson = personResult.containsKey("invalid") ? personResult.get("invalid").size() : 0;
                                String personNo = insertPerson + "/" + deletePerson + "/" + updatePerson + "/" + invalidPerson;
                                log.info(Thread.currentThread().getName() + ": 人员同步完成{}==={}", personNo, System.currentTimeMillis());
                                taskLog.setPersonNo(personNo);
                                countPerson = insertPerson + deletePerson + updatePerson + invalidPerson;
                            }
                            //    taskLog.setData(errorData.get(domainInfo.getId()));
                            taskLogService.save(taskLog, domainInfo.getId(), "update");

                            // PUT   MQ
                            if (null != personResult && personResult.size() > 0 && countPerson < 100) {
                                pubResult = dataBusUtil.pub(null, personResult, null, "person", domainInfo);
                                log.info("person pub:{}", pubResult);
                            }

                            //人员身份同步至sso
                            final Map<String, List<OccupyDto>> occupyResult = occupyService.buildOccupy(domainInfo, lastTaskLog, taskLog, null);
                            Integer countOccupy = 0;
                            if (null == occupyResult) {
                                log.error("无身份管理规则，不进行身份同步");
                                taskLog.setStatus("done");
                                taskLog.setOccupyNo("--/--/--/--");
                            } else {
                                //Integer recoverOccupy = occupyResult.containsKey("recover") ? deptResultMap.get("recover").size() : 0;
                                Integer insertOccupy = (occupyResult.containsKey("insert") ? occupyResult.get("insert").size() : 0);
                                Integer deleteOccupy = occupyResult.containsKey("delete") ? occupyResult.get("delete").size() : 0;
                                Integer updateOccupy = (occupyResult.containsKey("update") ? occupyResult.get("update").size() : 0);
                                Integer invalidOccupy = occupyResult.containsKey("invalid") ? occupyResult.get("invalid").size() : 0;
                                String occupyNo = insertOccupy + "/" + deleteOccupy + "/" + updateOccupy + "/" + invalidOccupy;
                                countOccupy = insertOccupy + deleteOccupy + updateOccupy + invalidOccupy;
                                log.info(Thread.currentThread().getName() + ": 人员身份同步完成{}==={}", occupyNo, System.currentTimeMillis());
                                taskLog.setStatus("done");
                                taskLog.setOccupyNo(occupyNo);
                            }
                            taskLogService.save(taskLog, domainInfo.getId(), "update");

                            // PUT   MQ
                            if (null != occupyResult && occupyResult.size() > 0 && countOccupy < 500) {
                                pubResult = dataBusUtil.pub(null, null, occupyResult, "occupy", domainInfo);
                                log.info("occupy pub:{}", pubResult);
                            }
                            //数据上传
                            if (StringUtils.isNotBlank(TaskConfig.errorData.get(domainInfo.getId()))) {
                                upload(domainInfo, taskLog);
                            } else {
                                log.info("{}本次同步无异常数据", domainInfo.getDomainName());
                            }
                            log.info("{}同步结束,task:{}", domainInfo.getDomainName(), taskLog.getId());
                        }catch (CustomException e) {
                            log.error("定时同步异常：" + e);
                            taskLog.setStatus("failed");
                            taskLog.setReason(e.getErrorMsg());
                            e.printStackTrace();
                        } catch (Exception e) {
                            log.error("定时同步异常：" + e);
                            taskLog.setStatus("failed");
                            taskLog.setReason(e.getMessage());
                            e.printStackTrace();
                        } finally {
                            if (StringUtils.isNotBlank(TaskConfig.errorData.get(domainInfo.getId()))) {
                                this.upload(domainInfo, taskLog);
                            } else {
                                taskLogService.save(taskLog, domainInfo.getDomainId(), "update");
                            }
                        }
                    } else {
                        taskLog.setReason("有编辑中规则，跳过数据同步");
                        taskLogService.save(taskLog, domainInfo.getId(), "skip");
                        log.info("编辑中规则数:{}", nodeRules.size());
                        log.info("有编辑中规则，跳过数据同步");
                    }
                }
            });

        }
    }

    public void upload(DomainInfo domainInfo, TaskLog taskLog) {
        try {
            String fileName = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + ".txt";
            JSONObject jsonObject = fileUtil.putFileByGql(TaskConfig.errorData.get(domainInfo.getId()).getBytes("UTF8"), fileName, domainInfo);
            if (null != jsonObject) {
                taskLog.setData(jsonObject.toJSONString());
            }
            taskLogService.save(taskLog, domainInfo.getId(), "upload-update");
            log.info("上传文件{}成功", fileName);

        } catch (CustomException e) {
            if (StringUtils.isBlank(taskLog.getReason())) {
                taskLog.setReason(e.getErrorMsg());
            }
            taskLogService.save(taskLog, domainInfo.getId(), "upload-update");
            log.error("上传文件失败:{}", e.getErrorMsg());
            e.printStackTrace();
        } catch (Exception e) {
            log.error("上传文件失败:{}", e);
            e.printStackTrace();
        }
    }

}
