package com.qtgl.iga.bean;

import lombok.Data;

import java.util.List;

/**
 * <FileName> OccupyConnection
 * <Desc>
 *
 * @author 1
 */
@Data
public class OccupyConnection {

    private Integer totalCount;

    private List<OccupyEdge> edges;
}
