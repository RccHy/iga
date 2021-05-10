
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
iga/sql/update/update.sql

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

3:将iga注册进maker，可获取到 clientId 和 clientSecret<br/>

4:通过maker数据集成【页面路径：/service/register】将iga的接口地址作为**数据服务**新增到网关中<br/>
注册数据服务参数：<br/>
**服务代码**：iga； <br/>
**描述**：iga； <br/>
**后端地址**：[https|http]://域名/iga/graphql<br/>
配置完成后可获得**服务地址**
5:通过4获取到的**服务地址**后，需要在apis项目添加配置才可正常使用治理功能。配置地址https://git.qtgl.com.cn/product/ketanyun-v2-support/-/wikis/%E7%A7%91%E6%8E%A2%E4%BA%91%E9%9B%86%E6%88%90%E9%85%8D%E7%BD%AE 搜索IGA_URL


# 环境变量说明【Environment】：<br/>

| 参数 | 参数说明 |  参考值 | 
| ------ | ------ | ------ | 
| IGA_DATASOURCE_URL | IGA项目本身数据库地址 | jdbc:mysql://XXXX:3306/iga?rewriteBatchedStatements=true
| IGA_DATASOURCE_USERNAME | 数据库用户名 | ----- 
| IGA_DATASOURCE_PASSWORD | 数据库地址 | ----- 
| SSO_DATASOURCE_URL | sso项目数据库地址 | jdbc:mysql://XXXX:3306/sso?rewriteBatchedStatements=true
| SSO_DATASOURCE_USERNAME | 数据库用户名 | ----- 
| SSO_DATASOURCE_PASSWORD | 数据库地址 | ----- 
| SSO_API_DATASOURCE_URL | sso-api项目数据库地址 | jdbc:mysql://XXXX:3306/sso-api?rewriteBatchedStatements=true
| SSO_API_DATASOURCE_USERNAME | 数据库用户名 | ----- 
| SSO_API_DATASOURCE_PASSWORD | 数据库地址 | ----- 
| SSO_URL | 科探云SSO地址 | https://cloud.ketanyun.cn/sso 支持多租环境，多组环境下任意挑选其中一租户的sso绝对路径
| BUS_URL | 网关bus地址 | https://cloud.ketanyun.cn/bus 支持多租环境，多组环境下任意挑选其中一租户的sso绝对路径
| APP_CLIENT | 应用id | ------
| APP_SECRET | 应用密钥 |------
| TASK_CRON | 同步时间 | 控制定时同步任务间隔时间。默认值 0 */5 * * * ?

* 注：
在maker中创建应用，并在sso/admin中勾选scope客户端认证：data、introspect<br/>
数据库链接地址中 注意增加参数rewriteBatchedStatements=true  以提高性能
  
* TODO：
SSO_URL、BUS_URL 多组环境下，支持标准的相对路径配置。
