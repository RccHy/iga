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












