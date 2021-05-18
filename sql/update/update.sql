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











