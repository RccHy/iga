-- 20221220

CREATE TABLE `dept` (
                        `id` varchar(50) DEFAULT NULL,
                        `dept_code` varchar(50) DEFAULT NULL,
                        `dept_name` varchar(200) DEFAULT NULL,
                        `dept_en_name` varchar(100) DEFAULT NULL COMMENT '英文名称',
                        `parent_code` varchar(50) DEFAULT NULL,
                        `del_mark` tinyint(4) DEFAULT NULL,
                        `tenant_id` varchar(50) DEFAULT NULL,
                        `update_time` datetime DEFAULT NULL,
                        `source` varchar(20) DEFAULT NULL,
                        `tags` varchar(50) DEFAULT NULL,
                        `data_source` varchar(255) DEFAULT NULL,
                        `description` varchar(255) DEFAULT NULL,
                        `orphan` tinyint(4) DEFAULT NULL,
                        `tree_type` varchar(50) DEFAULT NULL,
                        `type` varchar(50) DEFAULT NULL,
                        `relation_type` varchar(50) DEFAULT NULL COMMENT '关系类型  01隶属 02 直设 03 内设 04 挂靠 ',
                        `create_time` datetime DEFAULT NULL,
                        `active` tinyint(4) DEFAULT NULL,
                        `active_time` datetime DEFAULT NULL,
                        `dept_index` int(11) DEFAULT NULL,
                        `abbreviation` varchar(100) DEFAULT NULL,
                        `independent` tinyint(1) DEFAULT NULL,
                        `create_data_source` varchar(100) DEFAULT NULL COMMENT '创建来源(机读)',
                        `create_source` varchar(100) DEFAULT NULL COMMENT '创建来源',
                        `sync_state` int(2) DEFAULT NULL COMMENT '同步后状态 0 无变化 1 新增 2 修改 3 删除 4 失效'
);
CREATE TABLE `user_type` (
                             `id` varchar(50) NOT NULL,
                             `user_type` varchar(50) DEFAULT NULL,
                             `name` varchar(50) DEFAULT NULL,
                             `delay_time` int(11) DEFAULT NULL,
                             `tenant_id` varchar(50) DEFAULT NULL,
                             `formal` tinyint(1) DEFAULT NULL,
                             `del_mark` tinyint(1) DEFAULT 0,
                             `create_time` datetime NOT NULL,
                             `update_time` datetime NOT NULL,
                             `data_source` varchar(100) DEFAULT NULL,
                             `parent_code` varchar(50) DEFAULT NULL,
                             `tags` varchar(255) DEFAULT NULL,
                             `description` varchar(100) DEFAULT NULL,
                             `orphan` tinyint(1) DEFAULT NULL,
                             `active` tinyint(1) DEFAULT 1,
                             `active_time` datetime DEFAULT NULL,
                             `user_type_index` int(11) DEFAULT 0,
                             `source` varchar(100) DEFAULT NULL,
                             `post_type` varchar(50) DEFAULT NULL,
                             `monitor` tinyint(1) DEFAULT NULL,
                             `dept_type_code` varchar(50) DEFAULT NULL,
                             `client_id` varchar(50) DEFAULT NULL,
                             `create_data_source` varchar(100) DEFAULT NULL COMMENT '创建来源(机读)',
                             `create_source` varchar(100) DEFAULT NULL COMMENT '创建来源',
                             `sync_state` int(2) DEFAULT NULL COMMENT '同步后状态 0 无变化 1 新增 2 修改 3 删除 4 失效',
                             PRIMARY KEY (`id`) USING BTREE,
                             UNIQUE KEY `user_type_tenant_id` (`user_type`,`tenant_id`) USING BTREE
);

CREATE TABLE `user` (
                        `id` varchar(36) NOT NULL,
                        `identity_id` varchar(36) NOT NULL,
                        `user_type` varchar(100) DEFAULT NULL,
                        `user_code` varchar(50) DEFAULT NULL,
                        `name` varchar(100) DEFAULT NULL,
                        `card_type` varchar(10) DEFAULT NULL,
                        `card_no` varchar(50) DEFAULT NULL,
                        `del_mark` tinyint(1) DEFAULT 0,
                        `start_time` datetime DEFAULT NULL,
                        `end_time` datetime DEFAULT NULL,
                        `create_time` datetime DEFAULT NULL,
                        `update_time` datetime DEFAULT NULL,
                        `tenant_id` varchar(36) DEFAULT NULL,
                        `dept_code` varchar(100) DEFAULT NULL,
                        `source` varchar(100) DEFAULT NULL,
                        `data_source` varchar(100) DEFAULT NULL,
                        `active` tinyint(1) DEFAULT 1,
                        `active_time` datetime DEFAULT NULL,
                        `tags` varchar(255) DEFAULT NULL,
                        `description` varchar(100) DEFAULT NULL,
                        `user_index` int(11) DEFAULT 0,
                        `valid_start_time` datetime DEFAULT NULL,
                        `valid_end_time` datetime DEFAULT NULL,
                        `account_no` varchar(50) DEFAULT NULL,
                        `orphan` int(1) NOT NULL DEFAULT 0,
                        `create_data_source` varchar(100) DEFAULT NULL COMMENT '创建来源(机读)',
                        `create_source` varchar(100) DEFAULT NULL COMMENT '创建来源',
                        `sync_state` int(2) DEFAULT NULL COMMENT '同步后状态 0 无变化 1 新增 2 修改 3 删除 4 失效',
                        PRIMARY KEY (`id`) USING BTREE,
                        KEY `user_type` (`user_type`) USING BTREE,
                        KEY `user_code` (`user_code`) USING BTREE,
                        KEY `start_time` (`start_time`) USING BTREE,
                        KEY `end_time` (`end_time`) USING BTREE,
                        KEY `tenant_id` (`tenant_id`) USING BTREE,
                        KEY `dept_code` (`dept_code`) USING BTREE
);
CREATE TABLE `identity` (
                            `id` varchar(36) NOT NULL,
                            `name` varchar(100) DEFAULT NULL,
                            `open_id` varchar(50) DEFAULT NULL,
                            `account_no` varchar(50) DEFAULT NULL,
                            `description` varchar(100) DEFAULT NULL,
                            `del_mark` tinyint(1) DEFAULT 0,
                            `create_time` datetime DEFAULT NULL,
                            `update_time` datetime DEFAULT NULL,
                            `tenant_id` varchar(36) DEFAULT NULL,
                            `card_type` varchar(10) DEFAULT NULL,
                            `card_no` varchar(50) DEFAULT NULL,
                            `cellphone` varchar(50) DEFAULT NULL,
                            `email` varchar(100) DEFAULT NULL,
                            `source` varchar(100) DEFAULT '',
                            `data_source` varchar(100) DEFAULT NULL,
                            `sex` varchar(50) DEFAULT NULL,
                            `avatar_url` text DEFAULT null,
                            `birthday` varchar(10) DEFAULT NULL,
                            `tags` varchar(255) DEFAULT NULL,
                            `active` tinyint(1) DEFAULT 1,
                            `active_time` datetime DEFAULT NULL,
                            `valid_start_time` datetime DEFAULT NULL,
                            `valid_end_time` datetime DEFAULT NULL,
                            `freeze_time` datetime DEFAULT NULL,
                            `state` varchar(50) DEFAULT NULL,
                            `create_data_source` varchar(100) DEFAULT NULL COMMENT '创建来源(机读)',
                            `create_source` varchar(100) DEFAULT NULL COMMENT '创建来源',
                            `sync_state` int(2) DEFAULT NULL COMMENT '同步后状态 0 无变化 1 新增 2 修改 3 删除 4 失效',
                            PRIMARY KEY (`id`) USING BTREE,
                            UNIQUE KEY `open_id` (`open_id`) USING BTREE,
                            KEY `account_no` (`account_no`) USING BTREE,
                            KEY `del_mark` (`del_mark`) USING BTREE,
                            KEY `tenant_id` (`tenant_id`) USING BTREE,
                            KEY `cellphone` (`cellphone`) USING BTREE,
                            KEY `email` (`email`) USING BTREE,
                            KEY `card_type_card_no` (`card_type`,`card_no`) USING BTREE
);
CREATE TABLE `dynamic_attr` (
                                `id` varchar(50) NOT NULL,
                                `name` varchar(50) DEFAULT NULL,
                                `code` varchar(50) DEFAULT NULL,
                                `required` tinyint(1) DEFAULT 0,
                                `description` varchar(50) DEFAULT NULL,
                                `tenant_id` varchar(50) DEFAULT NULL,
                                `create_time` datetime NOT NULL DEFAULT current_timestamp(),
                                `update_time` datetime NOT NULL DEFAULT current_timestamp(),
                                `type` varchar(50) NOT NULL COMMENT '作用于： 人员USER、岗位POST、部门DEPT',
                                `field_type` varchar(50) NOT NULL COMMENT '字段类型 String、Int、Timestamp、Boolean、Float',
                                `format` varchar(50) DEFAULT NULL COMMENT '格式化',
                                `is_search` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否支持查询',
                                `attr_index` int(11) DEFAULT NULL COMMENT '排序',
                                PRIMARY KEY (`id`) USING BTREE
);
CREATE TABLE `dynamic_value` (
                                 `id` varchar(50) NOT NULL,
                                 `attr_id` varchar(50) DEFAULT NULL,
                                 `entity_id` varchar(50) DEFAULT NULL,
                                 `value` varchar(100) DEFAULT NULL,
                                 `tenant_id` varchar(50) DEFAULT NULL,
                                 PRIMARY KEY (`id`) USING BTREE,
                                 UNIQUE KEY `attr_entity` (`attr_id`,`entity_id`),
                                 KEY `attr_id_index` (`attr_id`) USING BTREE COMMENT '扩展字段索引',
                                 KEY `entity_id_index` (`entity_id`) USING BTREE COMMENT '实体字段索引'
);















--20221026
update  t_mgr_upstream_types set person_characteristic='USERNAME' where  person_characteristic='ACCOUNT_NO';

-- 20220930
alter table t_mgr_upstream_types add person_characteristic varchar(50) null comment '[人特征，人员类型合重方式以及身份匹配人方式] CARD_TYPE_NO:证件类型+证件号码 CARD_NO:仅证件号码 USERNAME:用户名 EMAIL:邮箱 CELLPHONE:手机号 OPENID:openid(仅身份类型匹配人)';
update  t_mgr_upstream_types set person_characteristic='USERNAME' where syn_type='person' and id in (select upstream_type_id from t_mgr_upstream_types_field where source_field='accountNo');
update  t_mgr_upstream_types set person_characteristic='CARD_TYPE_NO' where syn_type='person' and id in (select upstream_type_id from t_mgr_upstream_types_field where source_field='cardNo');
update  t_mgr_upstream_types set person_characteristic='USERNAME' where syn_type='occupy' and id in (select upstream_type_id from t_mgr_upstream_types_field where source_field='accountNo');
update  t_mgr_upstream_types set person_characteristic='CARD_TYPE_NO' where syn_type='occupy' and id in (select upstream_type_id from t_mgr_upstream_types_field where source_field='personCardNo');




--20220830
ALTER TABLE `incremental_task`
    ADD COLUMN `operation_no` varchar(50) NULL COMMENT '本次操作数量' AFTER `domain`,
    ADD COLUMN `main_task_id` varchar(50) NULL COMMENT '主任务id' AFTER `operation_no`;

-- 20220805
--删除失效继承规则的range
DELETE FROM  t_mgr_node_rules_range WHERE node_rules_id IN ( SELECT id FROM  t_mgr_node_rules WHERE inherit_id IS NOT NULL
  AND STATUS IN ( 0, 1 )  AND inherit_id NOT IN ( SELECT id FROM t_mgr_node_rules WHERE STATUS IN ( 1, 0 ) ) );
--删除失效的继承规则
DELETE FROM	t_mgr_node_rules WHERE inherit_id IS NOT NULL 	AND STATUS IN ( 0, 1 ) 	AND inherit_id NOT IN ( SELECT a.id FROM ( SELECT id FROM t_mgr_node_rules WHERE STATUS IN ( 1, 0 ) ) a );

--20220725
CREATE TABLE `t_mgr_pre_view_task`
(
    `id`          varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '主键id',
    `task_id`     varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '任务标识',
    `status`      varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '任务状态',
    `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
    `type`        varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '预览类型',
    `domain`      varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '租户信息外建',
    `update_time` timestamp NULL DEFAULT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

SET
FOREIGN_KEY_CHECKS = 1;

ALTER TABLE `t_mgr_upstream_types`
    ADD COLUMN `is_incremental` bit(1) NULL DEFAULT NULL COMMENT '\r\n是否增量  0为不是增量' AFTER `syn_way`;

CREATE TABLE `incremental_task`
(
    `id`               varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '主键id',
    `type`             varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '类型 组织机构、岗位、人、身份',
    `time`             timestamp NULL DEFAULT NULL COMMENT '下次同步查询时间戳。（max9:_then:10。 返回数据 最小时间小于max 则取max。最大时间大于then 则取10）',
    `create_time`      timestamp NULL DEFAULT NULL COMMENT '数据获取完成时间',
    `upstream_type_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '权威源类型ID',
    `domain`           varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '所属租户',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX              `type_index`(`type`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

SET
FOREIGN_KEY_CHECKS = 1;


--20220615
CREATE TABLE `occupy_temp`
(
    `id`               varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `user_type`        varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `user_code`        varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `name`             varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `gender`           varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `country`          varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `birthday`         varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `card_type`        varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `card_no`          varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `del_mark`         tinyint(1) NULL DEFAULT 0,
    `start_time`       datetime NULL DEFAULT NULL,
    `end_time`         datetime NULL DEFAULT NULL,
    `create_time`      datetime NULL DEFAULT NULL,
    `update_time`      datetime NULL DEFAULT NULL,
    `tenant_id`        varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `person_card_type` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `dept_code`        varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `person_card_no`   varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `post_code`        varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `post_name`        varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `source`           varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `data_source`      varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `active`           tinyint(1) NULL DEFAULT 1,
    `active_time`      datetime NULL DEFAULT NULL,
    `tags`             varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `description`      varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `user_index`       int(11) NULL DEFAULT 0,
    `valid_start_time` datetime NULL DEFAULT NULL,
    `valid_end_time`   datetime NULL DEFAULT NULL,
    `account_no`       varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `orphan`           tinyint(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

CREATE TABLE `person_temp`
(
    `id`               varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `name`             varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `open_id`          varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `account_no`       varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `gender`           varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `birthday`         varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `description`      varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `del_mark`         tinyint(1) NULL DEFAULT 0,
    `create_time`      datetime NULL DEFAULT NULL,
    `update_time`      datetime NULL DEFAULT NULL,
    `tenant_id`        varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `card_type`        varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `card_no`          varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `cellphone`        varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `email`            varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `data_source`      varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `sex`              varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `tags`             varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `active`           tinyint(1) NULL DEFAULT 1,
    `active_time`      datetime NULL DEFAULT NULL,
    `valid_start_time` datetime NULL DEFAULT NULL,
    `valid_end_time`   datetime NULL DEFAULT NULL,
    `freeze_time`      datetime NULL DEFAULT NULL,
    `state`            varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    `source`           varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

SET
FOREIGN_KEY_CHECKS = 1;






--20220513
ALTER TABLE `t_mgr_task_log`
    ADD COLUMN `syn_way` int(11) NULL COMMENT '同步方式 :1为手动同步' AFTER `data`;

UPDATE t_mgr_upstream set active = true where active is null

--如果执行该sql只有一条结果的话,执行下一条sql,如果有多条,请跟开发人员沟通
SELECT * FROM `t_mgr_upstream_types` WHERE syn_type = 'post' AND syn_way = 1;

UPDATE t_mgr_upstream_types_field SET source_field = 'type' WHERE source_field = 'depttype' AND upstream_type_id = (SELECT id FROM `t_mgr_upstream_types` WHERE syn_type = 'post' AND syn_way = 1);


-- 20211203
alter table t_mgr_dept_type add type_index int null comment '排序' after domain;


-- 20211022
create table t_mgr_dept_relation_type
(
    id             varchar(50) null,
    code           varchar(50) null comment '机构关系代码',
    name           varchar(50) null comment '机构关系名称',
    domain         varchar(50) null comment '所属租户',
    relation_index int null comment '排序'
) comment '组织机构关系类型表';

alter table t_mgr_dept_type add rule varchar(50) null comment '监控规则 暂不启用' after name;


-- 20210813
alter table t_mgr_upstream_types_field modify target_field text null comment '转换后字段名称';
update t_mgr_task_log
set data=null
where data is not null
   or data !='';


-- 20210629
alter table t_mgr_task_log modify reason text null;


-- 20210604
alter table t_mgr_task_log modify dept_no varchar (50) null;
alter table t_mgr_task_log modify post_no varchar (50) null;
alter table t_mgr_task_log modify person_no varchar (50) null;
alter table t_mgr_task_log modify occupy_no varchar (50) null;
alter table t_mgr_task_log
    add reason varchar(500) null comment '跳过、失败的原因';
alter table t_mgr_task_log
    add mark varchar(50) null comment '忽略ignore/解决solved 标记 ';
alter table t_mgr_task_log change mark data text null;
update t_mgr_upstream_types
set syn_type='occupy'
where syn_type = 'three';
create table t_mgr_monitor_rules
(
    id          varchar(50) null,
    rules       varchar(500) null comment '规则信息',
    type        varchar(50) null comment '监控类型 dept/post/person/occupy',
    domain      varchar(50) null,
    active      bit default b'1' not null comment '是否启用',
    active_time timestamp null comment '是否启用时间',
    create_time timestamp null comment '创建启用时间',
    update_time timestamp null comment '修改时间'
) comment '同步监控规则表';


-- 20210510
create table t_mgr_task_log
(
    id          varchar(50) not null
        primary key,
    status      int         not null,
    dept_no     int null,
    post_no     int null,
    person_no   int null,
    occupy_no   int null,
    create_time timestamp null,
    update_time timestamp null,
    domain      varchar(50) null comment '租户外键'
) comment '同步任务日志表';

alter table t_mgr_task_log
    add domain varchar(50) null comment '租户';
alter table t_mgr_task_log modify status varchar (50) not null;





