package com.qtgl.iga.service.impl;

import com.qtgl.iga.bo.PostType;
import com.qtgl.iga.dao.PostTypeDao;
import com.qtgl.iga.service.PostTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class PostTypeServiceImpl implements PostTypeService {

    @Autowired
    PostTypeDao postTypeDao;

    @Override
    public List<PostType> postTypes(Map<String, Object> arguments, String domain) {
        return postTypeDao.postTypes(arguments, domain);
    }

    @Override
    public PostType deletePostType(Map<String, Object> arguments, String domain) throws Exception {
        return postTypeDao.deletePostType(arguments, domain);
    }

    @Override
    public PostType savePostType(PostType postType, String domain) throws Exception {
        return postTypeDao.savePostType(postType, domain);
    }

    @Override
    public PostType updatePostType(PostType postType) throws Exception {
        return postTypeDao.updatePostType(postType);
    }
}
