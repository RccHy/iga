-- 20210510
create table t_mgr_task_log
(
    id          varchar(50) not null
        primary key,
    status      int         not null,
    dept_no     int         null,
    post_no     int         null,
    person_no   int         null,
    occupy_no   int         null,
    create_time timestamp   null,
    update_time timestamp   null,
    domain varchar(50)   null comment '租户外键'
)
    comment '同步任务日志表';

alter table t_mgr_task_log
	add domain varchar(50) null comment '租户';
alter table t_mgr_task_log modify status varchar(50) not null;

-- 20210604
alter table t_mgr_task_log modify dept_no varchar(50) null;
alter table t_mgr_task_log modify post_no varchar(50) null;
alter table t_mgr_task_log modify person_no varchar(50) null;
alter table t_mgr_task_log modify occupy_no varchar(50) null;
alter table t_mgr_task_log add reason varchar(500) null comment '跳过、失败的原因';
alter table t_mgr_task_log add mark varchar(50) null comment '忽略ignore/解决solved 标记 ';
alter table t_mgr_task_log change mark data text null;
update t_mgr_upstream_types set syn_type='occupy' where syn_type='three';
create table t_mgr_monitor_rules
(
	id varchar(50) null,
	rules varchar(500) null comment '规则信息',
	type varchar(50) null comment '监控类型 dept/post/person/occupy',
	domain varchar(50) null,
	active bit default b'1' not null comment '是否启用',
	active_time timestamp        null comment '是否启用时间',
	create_time timestamp        null comment '创建启用时间',
    update_time timestamp        null comment '修改时间'
)
comment '同步监控规则表';

-- 20210629
alter table t_mgr_task_log modify reason text null;


-- 20210813
alter table t_mgr_upstream_types_field modify target_field text null comment '转换后字段名称';
update t_mgr_task_log set data=null where data is not null or data !='';

-- 20211022
create table t_mgr_dept_relation_type
(
	id varchar(50) null,
	code varchar(50) null comment '机构关系代码',
	name varchar(50) null comment '机构关系名称',
	domain varchar(50) null comment '所属租户',
	relation_index int null comment '排序'
)
comment '组织机构关系类型表';

alter table t_mgr_dept_type modify rule varchar(50) null comment '监控规则 暂不启用' after name;

-- 20211203
alter table t_mgr_dept_type add type_index int null comment '排序' after domain;
alter table t_mgr_post_type add type_index int null comment '排序' after domain;





















