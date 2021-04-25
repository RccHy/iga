package com.qtgl.iga.bean;

import lombok.Data;

import java.util.List;

/**
 * <FileName> PersonConnection
 * <Desc>
 **/
@Data
public class PersonConnection {
    private Integer totalCount;

    private List<PersonEdge> edges;
}
