package com.qtgl.iga.dataFetcher;


import com.qtgl.iga.bean.OccupyConnection;
import com.qtgl.iga.bean.PersonConnection;
import com.qtgl.iga.bean.TaskResult;
import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.service.DeptService;
import com.qtgl.iga.service.OccupyService;
import com.qtgl.iga.service.PersonService;
import com.qtgl.iga.service.PostService;
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

    /**
     * 查询测试同步的 组织机构数据
     *
     * @return
     */
    public DataFetcher igaDepartments() {
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

    /**
     * 查询测试同步的 岗位数据
     *
     * @return
     */
    public DataFetcher igaPosts() {
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
    public DataFetcher queryPersonTaskData() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();


            try {
                PersonConnection persons = personService.findPersons(arguments, domain);
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
    public DataFetcher igaUsers() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();

            try {
                OccupyConnection occupies = occupyService.findOccupies(arguments, domain);
                return occupies;
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("查询人员身份失败", e);
            }


        };
    }

    /**
     *  人员同步测试接口
     *
     * @return
     */
    public DataFetcher testPersonTask() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            TaskResult taskResult = personService.testPersonTask(domain);
            return taskResult;

        };
    }

    /**
     *  人员身份同步测试接口
     *
     * @return
     */
    public DataFetcher testUserTask() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            TaskResult taskResult = personService.testPersonTask(domain);
            return taskResult;

        };
    }

    /**
     *  组织机构同步测试接口
     *
     * @return
     */
    public DataFetcher testDeptTask() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            TaskResult taskResult = personService.testPersonTask(domain);
            return taskResult;

        };
    }

    /**
     *  岗位同步测试接口
     *
     * @return
     */
    public DataFetcher testUserTypeTask() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            TaskResult taskResult = personService.testPersonTask(domain);
            return taskResult;

        };
    }
}
