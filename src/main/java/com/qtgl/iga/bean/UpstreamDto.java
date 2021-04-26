package com.qtgl.iga.bean;

import com.qtgl.iga.bo.Upstream;
import com.qtgl.iga.bo.UpstreamType;
import lombok.Data;

import java.util.List;

/**
 * <FileName> UpstreamDto
 * <Desc>
 *
 * @author 1
 */
@Data
public class UpstreamDto extends Upstream {
    private List<UpstreamType> upstreamTypes;

    public UpstreamDto(Upstream upstream) {
        this.setId(upstream.getId());
        this.setAppCode(upstream.getAppCode());
        this.setAppName(upstream.getAppName());
        this.setDataCode(upstream.getDataCode());
        this.setCreateTime(upstream.getCreateTime());
        this.setUpdateTime(upstream.getUpdateTime());
        this.setActiveTime(upstream.getActiveTime());
        this.setCreateUser(upstream.getCreateUser());
        this.setActive(upstream.getActive());
        this.setColor(upstream.getColor());
        this.setDomain(upstream.getDomain());
    }

    public UpstreamDto() {
    }
}
