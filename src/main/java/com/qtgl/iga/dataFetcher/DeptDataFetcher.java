package com.qtgl.iga.dataFetcher;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.service.DeptService;
import com.qtgl.iga.service.PostService;
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
                List<TreeBean> treeBeans = deptService.findDeptByDomainName(domain.getDomainName(), (String) arguments.get("treeType"),0);

                return getObject(e, treeBeans);


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
                List<TreeBean> posts = postService.findPosts(arguments,domain);
                return posts;
            } catch (Exception e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());
                List<TreeBean> treeBeans = postService.findDeptByDomainName(domain.getDomainName());

                return getObject(e, treeBeans);
            }
        };
    }

    private Object getObject(Exception e, List<TreeBean> treeBeans) {
        JSONObject hashMap = new JSONObject();

        JSONArray errors = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("message", e.getLocalizedMessage());
        errors.add(jsonObject);

        hashMap.put("errors", errors);
        JSONObject json = new JSONObject();
        json.put("depts", treeBeans);
        hashMap.put("data", json);
//                return hashMap;
        List<GraphqlError> errorsList = new ArrayList<>();
        errorsList.add(new GraphqlError(e.getLocalizedMessage()));

        return new DataFetcherResult(treeBeans, errorsList);
    }
}
