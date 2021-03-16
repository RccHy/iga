package com.qtgl.iga.bo;

import lombok.Data;

import java.sql.Timestamp;

/**
 * <FileName> Token
 * <Desc> token实体
 **/
@Data
public class Token {

    private String token;

    private Long expireIn;

    private Long now;

    public Token(String token, Long expireIn, Long now) {
        this.token = token;
        this.expireIn = expireIn;
        this.now = now;
    }

    public Token() {
    }
}
