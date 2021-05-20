-- 20210510
alter table t_mgr_task_log
	add domain varchar(50) null comment '租户';
alter table t_mgr_task_log modify status varchar(50) not null;



-- XXXXX
alter table t_mgr_task_log modify dept_no varchar(50) null;
alter table t_mgr_task_log modify post_no varchar(50) null;
alter table t_mgr_task_log modify person_no varchar(50) null;
alter table t_mgr_task_log modify occupy_no varchar(50) null;
alter table t_mgr_task_log add reason varchar(500) null comment '跳过、失败的原因';
alter table t_mgr_task_log add mark varchar(50) null comment '忽略ignore/解决solved 标记 ';
alter table t_mgr_task_log change mark data text null;
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













