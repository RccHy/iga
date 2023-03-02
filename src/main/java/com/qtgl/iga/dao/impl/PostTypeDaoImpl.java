package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.PostType;
import com.qtgl.iga.dao.PostTypeDao;
import com.qtgl.iga.utils.enums.FilterCodeEnum;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.util.*;


@Repository
@Component
public class PostTypeDaoImpl implements PostTypeDao {


    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;

    @Override
    public List<PostType> postTypes(Map<String, Object> arguments, String domain) {
        String sql = "select id, code, name, description," +
                "create_time as createTime, update_time as updateTime, " +
                "create_user as createUser, domain,type_index as typeIndex,formal from t_mgr_post_type where 1 = 1 and domain=?  ";
        //拼接sql
        StringBuffer stb = new StringBuffer(sql);
        //存入参数
        List<Object> param = new ArrayList<>();
        param.add(domain);
        dealData(arguments, stb, param);
        stb.append(" order by  type_index");
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(stb.toString(), param.toArray());
        ArrayList<PostType> list = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {

            for (Map<String, Object> map : mapList) {
                PostType postType = new PostType();
                BeanMap beanMap = BeanMap.create(postType);
                beanMap.putAll(map);
                list.add(postType);
            }
            return list;
        }

        return null;
    }

    @Override
    public PostType deletePostType(Map<String, Object> arguments, String domain) throws Exception {
        Object[] objects = new Object[2];
        objects[0] = arguments.get("id");
        objects[1] = domain;
        List<Map<String, Object>> mapList = jdbcIGA.queryForList("select id,code,name,description,create_time as createTime," +
                "update_time as updateTime,create_user as createUser, domain from  t_mgr_post_type  where id =? and domain= ? ", objects);
        ArrayList<PostType> list = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {

            for (Map<String, Object> map : mapList) {
                PostType postType = new PostType();
                BeanMap beanMap = BeanMap.create(postType);
                beanMap.putAll(map);
                list.add(postType);
            }
        }
        if (null == list || list.size() > 1 || list.size() == 0) {
            throw new CustomException(ResultCode.FAILED, "数据异常，删除失败");
        }
        PostType postType = list.get(0);


        //删除组织类别数据
        String sql = "delete from t_mgr_post_type  where id =?";
        int id = jdbcIGA.update(sql, preparedStatement -> preparedStatement.setObject(1, arguments.get("id")));


        return id > 0 ? postType : null;
    }

    @Override
    public PostType savePostType(PostType postType, String domain) throws Exception {
        //判重
        Object[] param = new Object[]{postType.getCode(), postType.getName(), domain};
        List<Map<String, Object>> mapList = jdbcIGA.queryForList("select id,code,name,description,create_time as createTime" +
                ",update_time as updateTime,create_user as createUser, domain from t_mgr_post_type where (code =? or name = ?) and domain=? ", param);
        if (null != mapList && mapList.size() > 0) {
            throw new CustomException(ResultCode.FAILED, "code 或 name 不能重复,添加组织机构类别失败");
        }

        String sql = "insert into t_mgr_post_type  values(?,?,?,?,?,?,?,?,?,?)";
        //生成主键和时间
        String id = UUID.randomUUID().toString().replace("-", "");
        postType.setId(id);
        Timestamp date = new Timestamp(System.currentTimeMillis());
        postType.setCreateTime(date);
        postType.setUpdateTime(date);
        int update = jdbcIGA.update(sql, preparedStatement -> {
            preparedStatement.setObject(1, id);
            preparedStatement.setObject(2, postType.getCode());
            preparedStatement.setObject(3, postType.getName());
            preparedStatement.setObject(4, postType.getDescription());
            preparedStatement.setObject(5, postType.getCreateTime());
            preparedStatement.setObject(6, postType.getUpdateTime());
            preparedStatement.setObject(7, postType.getCreateUser());
            preparedStatement.setObject(8, domain);
            preparedStatement.setObject(9, postType.getTypeIndex());
            preparedStatement.setObject(10, postType.getFormal());

        });
        return update > 0 ? postType : null;
    }

    @Override
    public PostType updatePostType(PostType postType) throws Exception {
        //判重
        Object[] param = new Object[]{postType.getCode(), postType.getName(), postType.getId(), postType.getDomain()};
        List<Map<String, Object>> mapList = jdbcIGA.queryForList("select id,code,name,description,create_time as createTime," +
                "update_time as updateTime,create_user as createUser, domain from t_mgr_post_type where (code = ? or name = ?) and id != ? and domain=?  ", param);
        if (null != mapList && mapList.size() > 0) {
            throw new CustomException(ResultCode.FAILED, "code 或 name 不能重复,修改组织机构类别失败");
        }
        String sql = "update t_mgr_post_type  set code = ?,name = ?,description = ?,create_time = ?," +
                "update_time = ?,create_user = ?,domain= ?,type_index=?,formal=?  where id=?";
        Timestamp date = new Timestamp(System.currentTimeMillis());
        return jdbcIGA.update(sql, preparedStatement -> {
            preparedStatement.setObject(1, postType.getCode());
            preparedStatement.setObject(2, postType.getName());
            preparedStatement.setObject(3, postType.getDescription());
            preparedStatement.setObject(4, postType.getCreateTime());
            preparedStatement.setObject(5, date);
            preparedStatement.setObject(6, postType.getCreateUser());
            preparedStatement.setObject(7, postType.getDomain());
            preparedStatement.setObject(8, postType.getTypeIndex());
            preparedStatement.setObject(9, postType.getFormal());
            preparedStatement.setObject(10, postType.getId());
        }) > 0 ? postType : null;
    }

    @Override
    public PostType findById(String id) {
        String sql = "select id, code, name, description," +
                "create_time as createTime, update_time as updateTime, " +
                "create_user as createUser, domain,type_index as typeIndex,formal from t_mgr_post_type where id= ? ";

        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, id);
        PostType postType = new PostType();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {

                BeanMap beanMap = BeanMap.create(postType);
                beanMap.putAll(map);
            }
            return postType;
        }
        return null;
    }

    @Override
    public void initialization(String domain) {

        String sql = "INSERT INTO t_mgr_post_type (id, code, name, description, create_time, update_time, create_user, domain,type_index,formal)\n" +
                "VALUES (uuid(), '01', '人员类别岗位', null, now(), now(), 'iga', ?,1,true)," +
                "       (uuid(), '02', '职务岗位', null, now(), now(), 'iga', ?,2,true)," +
                "       (uuid(), '03', '管理岗位', null, now(), now(), 'iga', ?,3,false)," +
                "       (uuid(), '04', '党务岗位', null, now(), now(), 'iga', ?,4,false),  " +
                "       (uuid(), '05', '业务岗位', null, now(), now(), 'iga', ?,5,false);";


        jdbcIGA.update(sql, preparedStatement -> {
            preparedStatement.setObject(1, domain);
            preparedStatement.setObject(2, domain);
            preparedStatement.setObject(3, domain);
            preparedStatement.setObject(4, domain);
            preparedStatement.setObject(5, domain);
        });

    }

    private void dealData(Map<String, Object> arguments, StringBuffer stb, List<Object> param) {
        Iterator<Map.Entry<String, Object>> it = arguments.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            if ("id".equals(entry.getKey())) {
                stb.append("and id= ? ");
                param.add(entry.getValue());
            }

            if ("filter".equals(entry.getKey())) {
                HashMap<String, Object> map = (HashMap<String, Object>) entry.getValue();
                for (Map.Entry<String, Object> str : map.entrySet()) {
                    if ("code".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if ("like".equals(FilterCodeEnum.getDescByCode(soe.getKey()))) {
                                stb.append("and code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if ("in".equals(FilterCodeEnum.getDescByCode(soe.getKey())) || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("name".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if ("like".equals(FilterCodeEnum.getDescByCode(soe.getKey()))) {
                                stb.append("and name ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if ("in".equals(FilterCodeEnum.getDescByCode(soe.getKey())) || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and name ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and name ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("createTime".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            //判断是否是区间
                            if ("gt".equals(soe.getKey()) || "lt".equals(soe.getKey())
                                    || "gte".equals(soe.getKey()) || "lte".equals(soe.getKey())) {
                                stb.append("and create_time ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());

                            }
                        }
                    }

                    if ("updateTime".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            //判断是否是区间
                            if ("gt".equals(soe.getKey()) || "lt".equals(soe.getKey())
                                    || "gte".equals(soe.getKey()) || "lte".equals(soe.getKey())) {
                                stb.append("and update_time ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());

                            }
                        }
                    }

                }
            }
        }
    }
}
