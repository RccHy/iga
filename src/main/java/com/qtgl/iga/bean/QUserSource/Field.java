/**
  * Copyright 2023 bejson.com 
  */
package com.qtgl.iga.bean.QUserSource;
import java.util.List;


public class Field {

    private String name;
    private Expression expression;
    public void setName(String name) {
         this.name = name;
     }
     public String getName() {
         return name;
     }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }
}