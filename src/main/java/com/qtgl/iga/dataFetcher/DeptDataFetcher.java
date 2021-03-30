package com.qtgl.iga.dataFetcher;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.DeptBean;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.service.DeptService;
import com.qtgl.iga.service.PostTypeService;
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
    PostTypeService postTypeService;


    public DataFetcher findDept() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            try {
                List<DeptBean> dept = deptService.findDept(arguments, domain);
                return dept;
            } catch (Exception e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());
                List<DeptBean> deptBeans = deptService.findDeptByDomainName(domain.getDomainName());

                return getObject(e, deptBeans);


            }

        };
    }

    public DataFetcher findPostType() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            try {
                List<DeptBean> userType = postTypeService.findUserType(domain);
                return userType;
            } catch (Exception e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());
                List<DeptBean> deptBeans = postTypeService.findDeptByDomainName(domain.getDomainName());

                return getObject(e, deptBeans);
            }
        };
    }

    private Object getObject(Exception e, List<DeptBean> deptBeans) {
        JSONObject hashMap = new JSONObject();

        JSONArray errors = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("message", e.getLocalizedMessage());
        errors.add(jsonObject);

        hashMap.put("errors", errors);
        JSONObject json = new JSONObject();
        json.put("depts", deptBeans);
        hashMap.put("data", json);
//                return hashMap;
        List<GraphqlError> errorsList = new ArrayList<>();
        errorsList.add(new GraphqlError(e.getLocalizedMessage()));

        return new DataFetcherResult(deptBeans, errorsList);
    }
}
