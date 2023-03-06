
package com.qtgl.iga.bean.QUserSource;

import java.util.List;
import java.util.Map;


public class Operation {

    private String kind;
    private String name;

    private List<Map> filters;
    private List<Map> arguments;

    private Map attributes;

    public void setKind(String kind) {
         this.kind = kind;
     }
     public String getKind() {
         return kind;
     }

    public void setName(String name) {
         this.name = name;
     }
     public String getName() {
         return name;
     }

    public List<Map> getFilters() {
        return filters;
    }

    public void setFilters(List<Map> filters) {
        this.filters = filters;
    }

    public List<Map> getArguments() {
        return arguments;
    }

    public void setArguments(List<Map> arguments) {
        this.arguments = arguments;
    }

    public Map getAttributes() {
        return attributes;
    }

    public void setAttributes(Map attributes) {
        this.attributes = attributes;
    }
}