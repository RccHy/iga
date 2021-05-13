####


**数据库：**

- 执行iga/update/update.sql下 20210510的sql

**new：**

- 发布前校验定时任务状态：若有**进行中**的定时任务则无法发布规则。<br/>
- 同步任务增加pub消息推送。可监听MQ来获取 人员、岗位、组织机构、人员身份 具体变化。参考定义[MQ][https://github.com/infoplus/canvas-docs/wiki/MQ]

**bug fix**
- 循环依赖数据问题bug修复
- 重复code数据冗余bug的优化
- 【**不兼容修改**，若在20210510版本上配置过对应的正则需调整规则配置】挂载节点表达式正则函数调整由原来的=Reg()改为=Reg("")，




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