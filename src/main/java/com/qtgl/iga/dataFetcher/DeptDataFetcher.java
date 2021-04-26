package com.qtgl.iga.dataFetcher;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.ErrorData;
import com.qtgl.iga.bean.OccupyConnection;
import com.qtgl.iga.bean.PersonConnection;
import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.service.DeptService;
import com.qtgl.iga.service.OccupyService;
import com.qtgl.iga.service.PersonService;
import com.qtgl.iga.service.PostService;
import com.qtgl.iga.service.impl.NodeRulesCalculationServiceImpl;
import com.qtgl.iga.utils.CertifiedConnector;
import com.qtgl.iga.utils.GraphqlError;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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


    public DataFetcher findDept() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            try {
                List<TreeBean> dept = deptService.findDept(arguments, domain);
                return dept;
            } catch (Exception e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());
//                List<TreeBean> treeBeans = deptService.findDept(arguments, domain);
                Object object = getObject(e, NodeRulesCalculationServiceImpl.errorData,NodeRulesCalculationServiceImpl.errorTree);
                return object;


            }

        };
    }

    public DataFetcher findPosts() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            try {
                List<TreeBean> posts = postService.findPosts(arguments, domain);
                return posts;
            } catch (Exception e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());
//                List<TreeBean> treeBeans = postService.findPosts(arguments, domain);


                return getObject(e, NodeRulesCalculationServiceImpl.errorData,NodeRulesCalculationServiceImpl.errorTree);
            }
        };
    }

    private Object getObject(Exception e, List<ErrorData> errorData, List<TreeBean> treeBeans) {
        JSONObject hashMap = new JSONObject();

        JSONArray errors = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("message", e.getLocalizedMessage());
        jsonObject.put("errorData",errorData);
        errors.add(jsonObject);

        hashMap.put("errors", errors);
        JSONObject json = new JSONObject();
        json.put("depts", treeBeans);
        hashMap.put("data", json);
//                return hashMap;
        List<GraphqlError> errorsList = new ArrayList<>();
        errorsList.add(new GraphqlError(e.getLocalizedMessage(), errorData));
        errorsList.add(new GraphqlError(JSON.toJSONString(errorData), errorData));

        return new DataFetcherResult(treeBeans, errorsList);
    }

    public DataFetcher findPersons() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();


            PersonConnection persons = personService.findPersons(arguments, domain);
            return persons;


        };
    }

    public DataFetcher findOccupies() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();


            OccupyConnection occupies = occupyService.findOccupies(arguments, domain);
            return occupies;


        };
    }
}
