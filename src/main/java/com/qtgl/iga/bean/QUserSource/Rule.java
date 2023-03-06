
package com.qtgl.iga.bean.QUserSource;


public class Rule {

    private boolean enabled;
    private String kind; // "include": "exclude",
    public Mount mount;
    public void setEnabled(boolean enabled) {
         this.enabled = enabled;
     }
     public boolean getEnabled() {
         return enabled;
     }

    public boolean isEnabled() {
        return enabled;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Mount getMount() {
        return mount;
    }

    public void setMount(Mount mount) {
        this.mount = mount;
    }
}