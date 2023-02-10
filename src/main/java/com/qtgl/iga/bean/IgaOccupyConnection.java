package com.qtgl.iga.bean;

import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

/**
 * <FileName> IgaOccupyConnection
 * <Desc>
 *
 * @author 1
 */
@Data
public class IgaOccupyConnection {

    private Integer totalCount;

    private List<IgaOccupyEdge> edges;

    private Timestamp updateTime;

}
