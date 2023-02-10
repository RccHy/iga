package com.qtgl.iga.dataFetcher;


import com.qtgl.iga.bean.IgaOccupyConnection;
import com.qtgl.iga.bean.PersonConnection;
import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.PreViewTask;
import com.qtgl.iga.service.*;
import com.qtgl.iga.utils.CertifiedConnector;
import com.qtgl.iga.utils.exception.CustomException;
import com.qtgl.iga.utils.exception.GraphqlExceptionUtils;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TestTaskFetcher {

    public static Logger logger = LoggerFactory.getLogger(TestTaskFetcher.class);


    @Autowired
    DeptService deptService;

    @Autowired
    PostService postService;

    @Autowired
    PersonService personService;

    @Autowired
    OccupyService occupyService;


    @Autowired
    PreViewTaskService preViewTaskService;

    /**
     * 查询测试同步的 组织机构数据
     *
     * @return
     */
    public DataFetcher igaDepartment() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            try {
                List<TreeBean> dept = deptService.findDept(arguments, domain);
                return dept;
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getErrorMsg());

                return GraphqlExceptionUtils.getObject("查询部门失败", e);


            }
        };
    }

    public DataFetcher lastPreViewTask(){
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            try {
                PreViewTask preViewTask = preViewTaskService.findLastPreViewTask(arguments.get("type").toString(), domain.getId());
                return preViewTask;
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getErrorMsg());

                return GraphqlExceptionUtils.getObject("查询最近一次同步任务失败", e);
            }
        };
    }

    /**
     * 查询测试同步的 岗位数据
     *
     * @return
     */
    public DataFetcher igaPost() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            try {
                List<TreeBean> posts = postService.findPosts(arguments, domain);
                return posts;
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());
                return GraphqlExceptionUtils.getObject("查询岗位失败", e);

            }
        };
    }


    /**
     * 查询测试同步的 人员数据
     *
     * @return
     */
    public DataFetcher igaUser() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();


            try {
                PersonConnection persons = personService.igaUser(arguments, domain);
                return persons;
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("查询人员失败", e);

            }


        };
    }

    /**
     * 查询测试同步的 人员身份数据
     *
     * @return
     */
    public DataFetcher igaOccupy() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();

            try {
                IgaOccupyConnection occupies = occupyService.igaOccupy(arguments, domain);
                return occupies;
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("查询人员身份失败", e);
            }


        };
    }

    /**
     * 人员同步测试接口
     *
     * @return
     */
    public DataFetcher testUserTask() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();


            try {
                PreViewTask taskResult = personService.testUserTask(domain, null);
                return taskResult;
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("当前正在人员测试同步中,请稍后再试", e);
            }

        };
    }

    /**
     * 人员身份同步测试接口
     *
     * @return
     */
    public DataFetcher testOccupyTask() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            try {

                PreViewTask taskResult = occupyService.testOccupyTask(domain, null);
                return taskResult;
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("当前正在人员身份测试同步中,请稍后再试", e);
            }

        };
    }

    /**
     * 组织机构同步测试接口
     *
     * @return
     */
    public DataFetcher testDeptTask() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            return null;

        };
    }

    /**
     * 岗位同步测试接口
     *
     * @return
     */
    public DataFetcher testPostTask() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            return null;

        };
    }
}
