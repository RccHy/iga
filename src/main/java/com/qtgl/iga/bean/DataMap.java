package com.qtgl.iga.bean;

import lombok.Data;

import java.util.List;

@Data
public class DataMap {

    private String code;

    private List<DataMapField> fields;
}
