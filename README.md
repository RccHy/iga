
# IGA

## 产品说明
身份治理

###  构建命令
vm构建命令，根据实际环境替换对应的值<br/>
```
待补充
```  
   
# 初始化数据库脚本
* 初次部署直接执行以下sql即可安装最新完整数据库
iga/sql/iga.sql
# 更新数据库脚本
* 在部署过环境进行更新，根据日期逐条执行sql
iga/sql/update/iga.sql

# 如何安装

1：运行数据脚本 <br/>
2：安装接口: http://ip:端口/iga/api/event <br/>
入参参考：<br/>
{
clientId: "", // 应用client_id <br/>
authCode: "", // 授权code,用于调用网关接口获取租户及应用启用信息 <br/>
timestamp: ,  //时间戳 <br/>
eventType: "create_tenant", //消息事件类型，值：「create_tenant:租户启用|disable_tenant: 禁用租户|delete_tenant: 删除租户 <br/>
tenantId: "" 租户全局code(domain)
}<br/>

安装成功返回：{"success": true}<br/>

# 环境变量说明【Environment】：<br/>

| 参数 | 参数说明 |  参考值 | 
| ------ | ------ | ------ | 
| IGA_DATASOURCE_URL | IGA项目本身数据库地址 | jdbc:mysql://XXXX:3306/iga
| IGA_DATASOURCE_USERNAME | 数据库用户名 | ----- 
| IGA_DATASOURCE_PASSWORD | 数据库地址 | ----- 
| SSO_DATASOURCE_URL | sso项目数据库地址 | jdbc:mysql://XXXX:3306/sso
| SSO_DATASOURCE_USERNAME | 数据库用户名 | ----- 
| SSO_DATASOURCE_PASSWORD | 数据库地址 | ----- 
| SSO_API_DATASOURCE_URL | sso-api项目数据库地址 | jdbc:mysql://XXXX:3306/sso-api
| SSO_API_DATASOURCE_USERNAME | 数据库用户名 | ----- 
| SSO_API_DATASOURCE_PASSWORD | 数据库地址 | ----- 
| SSO_URL | 科探云SSO地址 | https://cloud.ketanyun.cn/sso 支持多租环境，多组环境下任意挑选其中一租户的sso绝对路径
| BUS_URL | 网关bus地址 | https://cloud.ketanyun.cn/bus 支持多租环境，多组环境下任意挑选其中一租户的sso绝对路径
| APP_CLIENT | 应用id | ------
| APP_SECRET | 应用密钥 |------

* 注：
在maker中创建应用，并在sso/admin中勾选scope客户端认证：data、introspect
  
* TODO：
SSO_URL、BUS_URL 多组环境下，支持标准的相对路径配置。
