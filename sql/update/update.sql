-- 20210510
alter table t_mgr_task_log
	add domain varchar(50) null comment '租户';
alter table t_mgr_task_log modify status varchar(50) not null;



-- 20210510
alter table t_mgr_task_log modify dept_no varchar(50) null;
alter table t_mgr_task_log modify post_no varchar(50) null;
alter table t_mgr_task_log modify person_no varchar(50) null;
alter table t_mgr_task_log modify occupy_no varchar(50) null;







