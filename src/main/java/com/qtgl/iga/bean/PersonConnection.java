package com.qtgl.iga.bean;

import com.qtgl.iga.bo.Person;
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
