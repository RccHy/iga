
package com.qtgl.iga.bean.QUserSource;

import java.util.List;


public class Sources {


    private String name;
    private String text;
    private String kind;
    private String mode;
    private Strategies strategies;

    private Data data;
    private Service service;
    private Principal principal;
    private App app;
    private List<Field> fields;
    private Rule rule;

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getKind() {
        return kind;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getMode() {
        return mode;
    }

    public void setStrategies(Strategies strategies) {
        this.strategies = strategies;
    }

    public Strategies getStrategies() {
        return strategies;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public Service getService() {
        return service;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    public List<Field> getFields() {
        return fields;
    }

    public Rule getRule() {
        return rule;

    }

    public void setRule(Rule rule) {
        this.rule = rule;
    }

    public App getApp() {
        return app;
    }

    public void setApp(App app) {
        this.app = app;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public void setPrincipal(Principal principal) {
        this.principal = principal;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }
}