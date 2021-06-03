package com.qtgl.iga.bean;

import lombok.Data;

@Data
public class PubDto {

    /*{
    "specversion" : "1.0",
    "type" : "user.created",    // 和Topic一致，一般是 命名空间.数据类型.事件 的命名规范
    "source" : "XsFvzALh6KiMJ8tLa7G3",  // URI Reference格式，目前默认使用应用的client-id
    "subject" : "202103704",            // 对应与type中类型的id，如学工号
    "id" : "A234-1234-1234",            // 消息id，source+id 需唯一
    "time" : "2018-04-05T17:31:00Z",    // 时间戳 ISO 8601 格式
    "datacontenttype" : "application/json",          // 数据内容类型，目前只支持json
    "data" : "{\"name\":\"Alice\", \"age\":\"20\"}", // 额外数据，因消息类型而异
    "domain": "example.com",            // 消息发布者租户（来自access_token）
    "clientid": "n7dkDbprk4JZxDCD9UyU", // 消息发布者应用（来自access_token）
    "openid": "n7dkDbprk4JZxDCD9UyU"    // 消息发布者用户（来自access_token）
}*/
    private String specversion;
    private String id;
    private String type;
    private String source;
    private String subject;
    private String time;
    private String datacontenttype;
    private String data;
    private String domain;
    private String clientid;




}
