package com.qtgl.iga.dao;


import com.qtgl.iga.bo.PostType;

import java.util.List;
import java.util.Map;

public interface PostTypeDao {

    List<PostType> postTypes(Map<String, Object> arguments, String domain);

    PostType deletePostType(Map<String, Object> arguments, String domain) throws Exception;

    PostType savePostType(PostType postType, String domain) throws Exception;

    PostType updatePostType(PostType postType) throws Exception;

    PostType findById(String id);

    void initialization(String domain);
}
