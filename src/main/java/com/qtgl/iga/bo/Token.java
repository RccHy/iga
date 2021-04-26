package com.qtgl.iga.bo;

import lombok.Data;


/**
 * <FileName> Token
 * <Desc> token实体
 *
 * @author 1
 */
@Data
public class Token {
    /**
     * token
     */
    private String token;
    /**
     * 过期时间
     */
    private Long expireIn;
    /**
     * 现在时刻
     */
    private Long now;

    public Token(String token, Long expireIn, Long now) {
        this.token = token;
        this.expireIn = expireIn;
        this.now = now;
    }

    public Token() {
    }
}
