
package com.qtgl.iga.bean.QUserSource;
import com.qtgl.iga.bo.Tenant;

import java.util.List;


public class QUserSource {

    private String name;
    private String text;
    private String color;
    private List<Sources> sources;
    private Tenant tenant;
    public void setName(String name) {
         this.name = name;
     }
     public String getName() {
         return name;
     }

    public void setText(String text) {
         this.text = text;
     }
     public String getText() {
         return text;
     }

    public void setColor(String color) {
         this.color = color;
     }
     public String getColor() {
         return color;
     }

    public void setSources(List<Sources> sources) {
         this.sources = sources;
     }
     public List<Sources> getSources() {
         return sources;
     }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }
}