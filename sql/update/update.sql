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

alter table t_mgr_dept_type add rule varchar(50) null comment '监控规则 暂不启用' after name;



-- 20211203
alter table t_mgr_dept_type add type_index int null comment '排序' after domain;
alter table t_mgr_post_type add type_index int null comment '排序' after domain;

--20220513
ALTER TABLE `t_mgr_task_log`
ADD COLUMN `syn_way` int(11) NULL COMMENT '同步方式 :1为手动同步' AFTER `data`;

UPDATE t_mgr_upstream set active = true where active is null

--如果执行该sql只有一条结果的话,执行下一条sql,如果有多条,请跟开发人员沟通
SELECT	* FROM	`t_mgr_upstream_types` WHERE	syn_type = 'post' 	AND syn_way = 1;

UPDATE t_mgr_upstream_types_field SET source_field = 'type' WHERE	source_field = 'depttype'
	AND upstream_type_id = ( SELECT id FROM `t_mgr_upstream_types` WHERE syn_type = 'post' AND syn_way = 1 );

--20220615
DROP TABLE IF EXISTS `occupy_temp`;
CREATE TABLE `occupy_temp`  (
                                `id` varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
                                `user_type` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `user_code` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `name` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `gender` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `country` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `birthday` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `card_type` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `card_no` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `del_mark` tinyint(1) NULL DEFAULT 0,
                                `start_time` datetime NULL DEFAULT NULL,
                                `end_time` datetime NULL DEFAULT NULL,
                                `create_time` datetime NULL DEFAULT NULL,
                                `update_time` datetime NULL DEFAULT NULL,
                                `tenant_id` varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `person_card_type` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `dept_code` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `person_card_no` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `post_code` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `post_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `source` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `data_source` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `active` tinyint(1) NULL DEFAULT 1,
                                `active_time` datetime NULL DEFAULT NULL,
                                `tags` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `description` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `user_index` int(11) NULL DEFAULT 0,
                                `valid_start_time` datetime NULL DEFAULT NULL,
                                `valid_end_time` datetime NULL DEFAULT NULL,
                                `account_no` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `orphan` tinyint(1) NOT NULL DEFAULT 0,
                                PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

DROP TABLE IF EXISTS `person_temp`;
CREATE TABLE `person_temp`  (
                                `id` varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
                                `name` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `open_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `account_no` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `gender` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `birthday` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `description` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `del_mark` tinyint(1) NULL DEFAULT 0,
                                `create_time` datetime NULL DEFAULT NULL,
                                `update_time` datetime NULL DEFAULT NULL,
                                `tenant_id` varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `card_type` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `card_no` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `cellphone` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `email` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `data_source` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `sex` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `tags` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `active` tinyint(1) NULL DEFAULT 1,
                                `active_time` datetime NULL DEFAULT NULL,
                                `valid_start_time` datetime NULL DEFAULT NULL,
                                `valid_end_time` datetime NULL DEFAULT NULL,
                                `freeze_time` datetime NULL DEFAULT NULL,
                                `state` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                `source` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

SET FOREIGN_KEY_CHECKS = 1;























