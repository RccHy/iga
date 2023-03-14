
package com.qtgl.iga.bean.QUserSource;


public class Tenant {

    private String name;
    private boolean multitenancy;
    public void setName(String name) {
         this.name = name;
     }
     public String getName() {
         return name;
     }

    public void setMultitenancy(boolean multitenancy) {
         this.multitenancy = multitenancy;
     }
     public boolean getMultitenancy() {
         return multitenancy;
     }

}