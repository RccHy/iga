### new

**new**
- 环境变量 `APP_CLIENT` 改名为 `OAUTH_CLIENT_ID`
- 环境变量 `APP_SECRET` 改名为 `OAUTH_INTROSPECT_URL`
- 环境变量 `BUS_URL` 的值发生更改，改为标准 BUS服务地址.可参考[README.md]
- 新增环境变量 `OAUTH_TOKEN_URL`
- 新增环境变量 `OAUTH_AUTHORIZE_URL`
**delete**
- 删除环境变量 `SSO_URL`


### 20211110
**数据库**
- 无

**bug fix**
- 优化定时任务put消息
- 治理页面组织机构,岗位失效数据过滤
- 岗位根节点不合规的提示优化
**new**
- 增加人员冻结时间处理。
- 增加部门关系分类查询。
- 组织机构权威源同步支持简称,英文名称及关系类型的配置同步  


### 20210928
**数据库：**
- 无

**new：**
- 无

**bug fix**
- 解决多租环境下没有对租户信息进行数据库验证的bug

**image：**

- docker.qtgl.com.cn/product/iga:20210928

**依赖其他项目：**

- docker.qtgl.com.cn/product/console:20210928


### 20210923
**数据库：**
- 无

**new：**
- 随着ssoApi数据库中dept表迁移至sso库中调整对应的代码

**bug fix**
- 无

**image：**

- docker.qtgl.com.cn/product/iga:20210923

**依赖其他项目：**

- docker.qtgl.com.cn/product/console:20210814

### 20210906

**数据库：**
- 无

**new：**
- 新增标准环境变量 SERVICE_ORIGIN
- 岗位同步忽略库中**非**全局的岗位信息

**bug fix**
- 修改上个版本中提到的 '新增环境变量 FILE_API' 为 'FILE_URL'
- 修复上个版本文件上传失败,日志对应原因不准确的bug
- 人员同步逻辑调整优化


**image：**

- docker.qtgl.com.cn/product/iga:20210906

**依赖其他项目：**

- docker.qtgl.com.cn/product/console:20210814


### 20210813

**数据库：**
- 执行iga/update/update.sql下 20210813的sql

**new：**
- 新增环境变量 FILE_API
- 新增客户端认证scope： storage
- 将同步过程中忽略的数据生成log文件上传至文件服务器，可通过'运行监控状态'下载查看

**bug fix**
- 人员、人员身份同步合重bug

**image：**

- docker.qtgl.com.cn/product/iga:20210813

**依赖其他项目：**

- docker.qtgl.com.cn/product/console:20210813





### 20210805

**数据库：**

- 无

**new：**

- readme.md 中新增授权操作。*新部署需注意处理
- 对人员信息新增最终有效（根据是否有效、是否删除进行计算）、冻结时间
- 人员合重逻辑变为 必须提供 身份证件类型+身份证件号码 或者 用户名其中一种。 前者优先进行合重。
- 对人员身份信息新增最终有效（根据是否有效、有效时间、是否删除进行计算）、用户名(用于和人员用户名进行匹配)、孤儿标记字段(岗位、部门失效导致孤儿)
  

**bug fix**

- 权威源人员数据同步sso active字段赋值默认有效
- 调整拉取权威源数据字段映射逻辑，优化处理时间（不循环不需要计算的字段）

**image：**

- docker.qtgl.com.cn/product/iga:20210805

**依赖其他项目：**

- docker.qtgl.com.cn/product/console:20210805

### 20210629

**数据库：**

- 执行iga/update/update.sql下 20210629的sql

**new：**

- 同步同时对比身份信息，将产生身份日志，用于计算人员身份甘特图
- 调整错误信息格式，统一报错信息。详见readme.md 说明。
- 权威源管理增加描述重复性校验，同一权威源下不允许出现一样的描述。

**bug fix**

- 治理规则bug修复

**image：**
- docker.qtgl.com.cn/product/iga:20210629

**依赖其他项目：**
- docker.qtgl.com.cn/product/console:20210629

#### 20210604
**数据库：**

- 执行iga/update/update.sql下 20210604的sql

**new：**
- 执行**安装接口**同时对岗位类型、组织机构类型、单位类型数据进行初始化
- 规则发布前校验定时任务状态：若有**进行中**的定时任务则无法发布规则。<br/>
- 同步任务增加pub消息推送。可监听MQ来获取 人员、岗位、组织机构、人员身份 具体变化。参考定义[MQ][https://github.com/infoplus/canvas-docs/wiki/MQ]
- 定时任务监控优化，概览页增加监控信息


**bug fix**
- 循环依赖数据问题bug修复
- 重复code数据冗余bug的优化
- 【**不兼容修改**，若在20210510版本上配置过对应的正则需调整规则配置】挂载节点表达式正则函数调整由原来的=Reg()改为=Reg("")；调整方式，直接对已有正则的规则进行修改。

**image：**
- docker.qtgl.com.cn/product/iga:20210604

**依赖其他项目：**
- docker.qtgl.com.cn/product/console:20210603


### 20210510

**数据库：**

- 执行iga/update/update.sql下 20210510的sql

**new：**

1：日志监控
    监控定时任务监控情况

2：增加环境变量**TASK_CRON**，控制定时同步任务时间。默认值 0 */5 * * * ?

**change：**

1: 支持表达式  （=开头判定为表达式）
* 挂载节点支持 **'全部'**（=*） 以及  **正则**  (=Reg("^hr_20.*$"))
* 权威源字段映射可直接通过表达式进行 **重命名** (="hr_"+$Code) 以及 **正则**
* 去除规则配置中的重命名规则

**delete：**
- 无

**image：**
- docker.qtgl.com.cn/product/iga:20210510

**依赖其他项目：**
- docker.qtgl.com.cn/product/console:20210510



### 20210426
**数据库：**

- 执行iga/sql下   iga.sql

**new：**

1：单位类型管理<br/>

单位类型的字典维护-增/删/查/改

2：岗位类型管理

岗位类型的字典维护-增/删/查/改

3：组织机构类型管理

组织机构的字典维护-增/删/查/改

4：上游源类型注册

上游源类型注册的维护；

A：将有能力提供上游数据的注册成为一个上游源。

B: 将源下提供的graphql类型注册成上游源类型。

C: 每个类型都提供一种数据治理的能力。（人员、部门、岗位、人员身份）

D：配置允许进行接口进行推送数据，并覆盖更新同一个上游源下的相同唯一标识数据


5：人员治理

配置人员数据的数据来源。 

6：岗位治理

配置岗位数据的数据来源以及规则；

7：组织机构治理

配置组织机构数据的数据来源以及规则；

8：人员身份治理

配置人员身份数据的数据来源；

**change：**
- 无

**bugfix：**
- 无

**delete：**
- 无

**image：**
- docker.qtgl.com.cn/product/iga:20210426

**依赖其他项目：**
- docker.qtgl.com.cn/product/console:20210426

[https://github.com/infoplus/canvas-docs/wiki/MQ]: https://github.com/infoplus/canvas-docs/wiki/MQ<br/>