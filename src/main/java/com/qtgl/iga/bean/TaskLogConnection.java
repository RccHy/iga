package com.qtgl.iga.bean;

import lombok.Data;

import java.util.List;

/**
 * <FileName> TaskLogConnection
 * <Desc>
 *
 * @author 1*/
@Data
public class TaskLogConnection {

    private Integer totalCount;

    private List<TaskLogEdge> edges;
}
