
# 20210425
create table t_mgr_task_log
(
	id varchar(50) not null,
	status int not null,
	dept_no int null,
	post_no int null,
	person_no int null,
	occupy_no int null,
	create_time timestamp null,
	update_time timestamp null,
	constraint t_mgr_task_log_pk
		primary key (id)
)
comment '同步任务日志表';

