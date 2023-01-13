package com.qtgl.iga.bean;

import lombok.Data;

import java.util.List;

/**
 * <FileName> UserConnection
 * <Desc>
 *
 * @author 1
 */
@Data
public class UserConnection {

    private Integer totalCount;

    private List<OccupyEdge> edges;
}
