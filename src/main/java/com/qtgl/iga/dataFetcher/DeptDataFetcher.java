package com.qtgl.iga.dataFetcher;

import com.qtgl.iga.bean.OccupyConnection;
import com.qtgl.iga.bean.PersonConnection;
import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.service.DeptService;
import com.qtgl.iga.service.OccupyService;
import com.qtgl.iga.service.PersonService;
import com.qtgl.iga.service.PostService;
import com.qtgl.iga.utils.CertifiedConnector;
import com.qtgl.iga.utils.exception.GraphqlExceptionUtils;
import com.qtgl.iga.utils.exception.CustomException;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DeptDataFetcher {

    public static Logger logger = LoggerFactory.getLogger(DeptDataFetcher.class);


    @Autowired
    DeptService deptService;

    @Autowired
    PostService postService;

    @Autowired
    PersonService personService;

    @Autowired
    OccupyService occupyService;

    /**
     * 查询组织机构数据
     *
     * @return
     */
    public DataFetcher findDept() {
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
     * 查询岗位数据
     *
     * @return
     */
    public DataFetcher findPosts() {
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
     * 查询人员数据
     *
     * @return
     */
    public DataFetcher findPersons() {
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
     * 查询人员身份数据
     *
     * @return
     */
    public DataFetcher findOccupies() {
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
}
