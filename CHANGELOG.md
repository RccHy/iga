### 20230427

**数据库**
- 执行iga/update/update.sql下 20230427的sql

**new：**
- 影子副本相关功能(从上游获取失败后通过副本获取数据)
- 权威源数据分页获取异常的逻辑处理
**bug fix**
- 默认监控百分比功能表达式异常 bug fix
- bootstrap推送规则权限添加 bug fix
- 20211203的sql丢失补充

**image：**

- docker.qtgl.com.cn/product/iga:20230427

**依赖其他项目：**

- docker.qtgl.com.cn/product/console:20230427



### 20230406

**数据库**
- 执行iga/update/update.sql下 20230406的sql

**new：**
- 人员同步逻辑兼容手动合重基础属性
- 组织机构及岗位支持sub
**bug fix**
- 仅有超级租户规则时 添加规则异常 bug fix
- 同步日志下载 bug fix
- 人员同步sex字段支持异常 bug fix

**image：**

- docker.qtgl.com.cn/product/iga:20230406

**依赖其他项目：**

- docker.qtgl.com.cn/product/console:20230406



### 身份治理-20230323-v0.3.7

**数据库**
- 执行 /scripts/iga/sql/update/update.sql  中  20230323 日期脚本


**new：**
- *【beta】支持通过bus的MQ触发权威源同步，提高当个权威源数据更新效率。参考定义[MQ](https://github.com/ketanyun/docs/wiki/MQ), 参考邮件["关于身份治理“订阅”模式的支持"]
- 支持通过install解析QUserSource，来实现自动初始化环境所需要的身份治理相关权威源以及规则信息， yaml定义参考[QUserSource.yaml](https://github.com/ketanyun/docs/blob/main/src/main/resources/crd/QUserSource.yaml)

**bug fix**
- 无

**image：**

- docker.qtgl.com.cn/product/iga:20230323
- docker.qtgl.com.cn/product/iga-sync:20230216

- **chart：**

  iga: 0.3.7

### 20230302

**数据库**
- 无

**new：**
- 人员同步支持头像同步

**bug fix**
- 同步日志解析json异常 bug fix
- 权威源校验租户异常 bug fix

**image：**

- docker.qtgl.com.cn/product/iga:20230302

**依赖其他项目：**

- docker.qtgl.com.cn/product/console:20230302



### 20230216

**数据库**
- 执行iga/update/update.sql下 20230216的sql

**new：**
- 岗位同步 formal字段的支持
- 人员同步支持 sex 字段

**bug fix**
- 人员同步未提供active字段时  恢复逻辑异常bug fix

**image：**

- docker.qtgl.com.cn/product/iga:20230216

**依赖其他项目：**

- docker.qtgl.com.cn/product/console:20230216


### 20230209

**数据库**
- 执行iga/update/update.sql下 20221220的sql

**new：**
- 人员及人员身份测试同步及测试数据查询功能

**bug fix**
- 无
  
**image：**

- docker.qtgl.com.cn/product/iga:20230209

**依赖其他项目：**

- docker.qtgl.com.cn/product/console:20230209


### 20230113

**数据库**
- 无

**new：**
- 无

**bug fix**
- 权威源查询租户相关条件补充
- 人员及人员身份同步相关bug fix

**image：**

- docker.qtgl.com.cn/product/iga:20230113

**依赖其他项目：**

- docker.qtgl.com.cn/product/console:20221229



### 20221229

**数据库**
- 无

**new：**
- 人员同步支持birthday字段同步

**bug fix**
- 人员同步扩展字段同步逻辑 bug fix @戴会勇,@刘雨

**image：**

- docker.qtgl.com.cn/product/iga:20221229

**依赖其他项目：**

- docker.qtgl.com.cn/product/console:20221229


### 20221130

**数据库**
- 无

**new：**
- 人员身份扩展字段同步支持

**bug fix**
- 治理规则监控规则初始化 bug fix

**image：**

- docker.qtgl.com.cn/product/iga:20221130

**依赖其他项目：**

- docker.qtgl.com.cn/product/console:20221102



### 20221107

**数据库**
- 无

**new：**
- 无

**bug fix**
- 数据治理 人员同步扩展字段处理 bug fix
- 人员及身份同步 兼容手工合重  bug fix

**image：**

- docker.qtgl.com.cn/product/iga:20221107

**依赖其他项目：**

- docker.qtgl.com.cn/product/console:20221102


### 20221026

**数据库**
- 执行iga/update/update.sql下 20221026的sql

**new：**
- 无

**bug fix**
- 数据治理人员同步 bug fix

**image：**

- docker.qtgl.com.cn/product/iga:20221025

**依赖其他项目：**

- docker.qtgl.com.cn/product/console:20220930


### 20220930

**数据库**
- 执行iga/update/update.sql下 20220930的sql

**new：**
- 监控数据由仅监控删除数据调整为监控删除与失效数据
- 人员,人员身份同步支持指定模式数据同步

**bug fix**
- 无

**image：**

- docker.qtgl.com.cn/product/iga:20220930

**依赖其他项目：**

- docker.qtgl.com.cn/product/console:20220930



### 20220908

**数据库**
- 无

**new：**
- 身份治理覆盖source及DataSource逻辑调整(PULL的数据，如果通过控制台(或者其他方式)修改过发生了源头的改变，在下次同步时会被强制同步回PULL，无论该数据是否发生过变化)  @刘洋 @殷佳波

**bug fix**
- 人员身份同步 bug fix  @陈洋

**image：**

- docker.qtgl.com.cn/product/iga:20220906

**依赖其他项目：**

- docker.qtgl.com.cn/product/console:20220908



### 20220831

**数据库**
- 执行iga/update/update.sql下 20220830的sql

**new：**
- 无

**bug fix**
- 人员身份有效期相关逻辑处理修改
- 身份治理权威源人员,人员身份修改数据后 DataSource未修改的 bug fix
- 人员及人员身份预览任务相关 bug fix
- 增量同步日志与主概览日志关联逻辑添加
- 人员及人员身份删除规则 bug fix
- pub 接口 bug fix

**image：**

- docker.qtgl.com.cn/product/iga:20220831

**依赖其他项目：**

- docker.qtgl.com.cn/product/console:20220831

### 20220819


**bug fix**
- 优化初始化租户的逻辑，防止重复创建
 
### 20220808

**数据库**
- 执行iga/update/update.sql下 20220805的sql

**new：**
- 无

**bug fix**
- 组织机构,岗位数据同步activeTime不更新 bug fix
- 过时继承规则数据处理
- 人员同步证件类型校验异常bug fix
- 人员同步最终有效期未处理的 bug fix

**image：**

- docker.qtgl.com.cn/product/iga:20220808

**依赖其他项目：**

- docker.qtgl.com.cn/product/console:20220808

### 20220726

**数据库**
- 执行iga/update/update.sql下 20220725的sql

**new：**
- 人员,人员身份预览日志数据添加  (需执行数据库脚本)
- 权威源映射字段 map表达式支持
- 数据治理增量数据同步支持 (需执行对应数据库脚本)
- 身份有效期逻辑调整
- 组织机构岗位添加失效规则逻辑,上游拉取节点配置规则后,下次同步不再提供该节点,则该节点的对应规则置为无效


**bug fix**
- user_log 数据插入 判断条件,失效数据未插入的bug 修复 @欧云海 
- 身份同步orphan变更传递bug 修复 @欧云海
- 组织机构,岗位同步 排序字段遗漏同步的bug fix

**image：**

- docker.qtgl.com.cn/product/iga:20220726

**依赖其他项目：**

- docker.qtgl.com.cn/product/console:20220726


### 20220615

**数据库**
- 执行iga/update/update.sql下 20220615的sql

**new：**
- 人员,人员身份预览功能
- 同步表达式 $ENTITY相关支持,具体配置规则参考 README
  **bug fix**
- 无

**image：**

- docker.qtgl.com.cn/product/iga:20220615

**依赖其他项目：**

- docker.qtgl.com.cn/product/console:20220615



### 20220520

**数据库**
- 执行iga/update/update.sql下 20220513的sql

**new：**
- iga CRD相关接口开发
- 手动触发定时任务功能
- 权威源失效则本批数据不做删除失效处理
  **bug fix**
- 校验规则 查询有效条件
- 同步扩展字段bug fix @欧云海
- 人员身份数据库重复身份处理,重复的身份以包含工号及修改时间进行冗余数据删除处理
- 岗位同步映射字段处理(主要针对于身份岗同步异常处理),参考iga/update/update.sql
- 同步任务同步,最近三条都失败的情况下则不进行定时同步任务,处理后可正常同步 @刘洋

**image：**

- docker.qtgl.com.cn/product/iga:20220520

**依赖其他项目：**

- docker.qtgl.com.cn/product/console:20220520




### 20220411

**数据库**
- 无

**new：**
- 无

**bug fix**
- 权威源系统权限校验细化
  之前在身份治理->权威源管理中配置对应的推送权威源类型就认定当前应用有操作的权限,  细化后需要配置到对应的治理规则中去,例如人员推送,1.权威源管理中首先配置推送的权威源类型,2.在人员治理中,配置对应的推送规则,才会生效.  注:不配置规则 就会没有对应的操作权限,本改动属于不兼容变动

**image：**

- docker.qtgl.com.cn/product/iga:20220411

**依赖其他项目：**

- docker.qtgl.com.cn/product/console:20220411


### 20220222

**数据库**
- 无

**new：**
- 组织机构,岗位,人员同步支持扩展字段同步

**bug fix**
- 组织机构,岗位同步判断条件不限制租户的bug修复 @欧云海

**image：**

- docker.qtgl.com.cn/product/iga:20220222

**依赖其他项目：**

- docker.qtgl.com.cn/product/console:20220222


### 20220118

**数据库**
- 无
  
**new：**
- 人员身份同步支持通过身份证件类型及身份证件号码

**bug fix**
- 数据治理中人员及人员身份预览的bug修复
- 修复 全局应用token无法调用接口问题 @张永明

**image：**

- docker.qtgl.com.cn/product/iga:20220118

**依赖其他项目：**

- docker.qtgl.com.cn/product/console:20220118

### 20211220

**数据库**
- 无

**bug fix**
- 修复初始化部署安装初始化bug。

**image：**

- docker.qtgl.com.cn/product/iga:20211221

**依赖其他项目：**

- docker.qtgl.com.cn/product/console:20211221


### 20211203

**数据库**
- - 执行iga/update/update.sql下 20211203的sql
    
**请检测部署方式是否为StatefulSet，若不是需调整**

**bug fix**
- 多租环境下编辑岗位类型、单位类型、单位树类型报错bug

**new**
- 环境变量 `APP_CLIENT` 改名为 `OAUTH_CLIENT_ID`
- 环境变量 `APP_SECRET` 改名为 `OAUTH_CLIENT_SECERT`
- 环境变量 `BUS_URL` 的值发生更改，改为标准 BUS服务地址.可参考[README.md]
- 新增环境变量 `OAUTH_TOKEN_URL` 可参考[README.md]
- 新增环境变量 `OAUTH_INTROSPECT_URL` 可参考[README.md]
- 新增环境变量 `OAUTH_AUTHORIZE_URL` 可参考[README.md]
- 后端新增 `岗位类型` `部门类型` 排序字段
**delete**
- 删除环境变量 `SSO_URL`

**image：**

- docker.qtgl.com.cn/product/iga:20211203

**依赖其他项目：**

- docker.qtgl.com.cn/product/console:20211203



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

[https://github.co