package com.qtgl.iga.bean;

import lombok.Data;

import java.util.List;

/**
 * <FileName> PersonConnection
 * <Desc>
 *
 * @author 1*/
@Data
public class PersonConnection {
    private Integer totalCount;

    private List<PersonEdge> edges;
}
