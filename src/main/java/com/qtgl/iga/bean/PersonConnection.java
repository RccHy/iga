package com.qtgl.iga.bean;

import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

/**
 * <FileName> PersonConnection
 * <Desc>
 *
 * @author 1
 */
@Data
public class PersonConnection {
    private Integer totalCount;

    private List<PersonEdge> edges;

    private Timestamp updateTime;
}
