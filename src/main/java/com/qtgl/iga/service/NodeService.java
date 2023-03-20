package com.qtgl.iga.service;


import com.qtgl.iga.bean.NodeDto;
import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.Node;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public interface NodeService {

    NodeDto saveNode(NodeDto node, String domain) throws Exception;

    Node getRoot(String domain, String deptTreeType);

    List<Node> getByCode(String domain, String deptTreeType, String nodeCode);

    NodeDto deleteNode(Map<String, Object> arguments, String domainId);

    List<NodeDto> findNodes(Map<String, Object> arguments, String domainId, Boolean flag);

    NodeDto updateNode(NodeDto nodeDto);

    List<NodeDto> findNodesPlus(Map<String, Object> arguments, String id);

    List<Node> findNodesByCode(String code, String domain, String type);

    Node applyNode(Map<String, Object> arguments, String id);

    Node rollbackNode(Map<String, Object> arguments, String id) throws Exception;

    Integer judgeEdit(Map<String, Object> arguments, DomainInfo domain, String type) throws Exception;

    List<Node> nodeStatus(Map<String, Object> arguments, String domainId) throws Exception;

    void updateNodeAndRules(List<NodeDto> nodes, List<TreeBean> beans);

    List<Node> findByTreeTypeCode(String code, Integer status, String domain);

    void deleteNodeById(String nodeId, String domain);

    List<NodeDto> findNodesByStatusAndType(Integer status, String type, String domainId, Timestamp version);

    List<Node> getByTreeType(String domainId, String code, Integer status, String type);

    List<NodeDto> findNodes(String domainId, Integer status, String type, Boolean flag);

    Node finNodeById(String nodeId);
}
