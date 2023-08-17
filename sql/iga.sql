create table t_mgr_dept_tree_type
(
    id                 varchar(50)      not null comment '主键'
        primary key,
    code               varchar(50)      not null comment '机构树类型代码',
    name               varchar(50)      not null comment '机构类型名称',
    description        varchar(500)     null comment '描述',
    multiple_root_node bit default b'0' null comment '是否允许多个根节点',
    create_time        timestamp        null comment '创建时间',
    update_time        timestamp        null comment '修改时间',
    create_user        varchar(50)      null comment '创建人工号',
    domain             varchar(50)      null comment '租户外键',
    tree_index         int(1)           null comment '排序字段'
)
    comment '组织机构树类别' charset = utf8;

create table t_mgr_dept_type
(
    id          varchar(50) not null comment '主键'
        primary key,
    code        varchar(50) not null comment '机构类型代码',
    name        varchar(50) not null comment '机构类型名称',
    rule varchar(50) null comment '监控规则',
    description varchar(50) null comment '描述',
    create_time timestamp   null comment '创建时间',
    update_time timestamp   null comment '修改时间',
    create_user varchar(50) null comment '创建人工号',
    domain      varchar(50) null comment '租户外键',
    type_index int null comment '排序'
)
    comment '组织机构类别' charset = utf8;

create table t_mgr_domain_info
(
    id            varchar(50) not null comment '主键'
        primary key,
    domain_id     varchar(50) null comment '租户id',
    domain_name   varchar(50) not null comment '租户名称（域名）',
    client_id     varchar(50) null comment '授权id',
    client_secret varchar(50) null comment '授权密钥',
    create_time   timestamp   null comment '注册时间',
    update_time   timestamp   null,
    create_user   varchar(50) null comment '注册人员',
    status        int(1)      null comment '状态  启用0/停用1'
)
    comment '租户注册表' charset = utf8;

alter table t_mgr_domain_info add unique (domain_name);


create table t_mgr_node
(
    id             varchar(50)  not null
        primary key,
    manual         bit          null comment '是否允许手工',
    node_code      varchar(255) not null comment '节点',
    create_time    timestamp    null comment '创建时间/版本号',
    update_time    timestamp    null comment '修改时间',
    domain         varchar(50)  not null comment '所属租户',
    dept_tree_type varchar(50)  null comment '部门树类型',
    status         int          not null comment '当前状态 0 发布 1 编辑中 2 历史',
    type           varchar(255) null comment 'person  dept post occupy'
)
    charset = utf8;

create index t_mgr_node_t_mgr_dept_tree_type_id_fk
    on t_mgr_node (dept_tree_type);

create table t_mgr_post_type
(
    id          varchar(50) not null comment '主键'
        primary key,
    code        varchar(50) not null comment '机构类型代码',
    name        varchar(50) not null comment '机构类型名称',
    description varchar(50) null comment '描述',
    create_time timestamp   null comment '创建时间',
    update_time timestamp   null comment '修改时间',
    create_user varchar(50) null comment '创建人工号',
    domain      varchar(50) null comment '租户外键',
    type_index int null comment '排序',
    formal tinyint(1) NULL COMMENT '是否身份岗'
)
    comment '组织机构类别' charset = utf8;

create table t_mgr_task_log
(
    id          varchar(50) not null
        primary key,
    status      varchar(50) not null,
    dept_no     varchar(50) null,
    post_no     varchar(50) null,
    person_no   varchar(50) null,
    occupy_no   varchar(50) null,
    create_time timestamp   null,
    update_time timestamp   null,
    domain      varchar(50) null comment '租户',
    reason      text        null,
    data        text        null,
    syn_way     int(11)     null comment '同步方式 :1为手动同步'
)
    comment '同步任务日志表';


create table t_mgr_upstream
(
    id          varchar(50)      not null comment '主键'
        primary key,
    app_code    varchar(50)      not null comment '应用代码，如人事：HR_SYS',
    app_name    varchar(50)      not null comment '应用名称，如人事',
    data_code   varchar(50)      null comment '数据前缀代码 HR',
    create_time timestamp        null comment '注册时间',
    create_user varchar(50)      null comment '注册人员',
    active      bit default b'0' null comment '状态  启用/不启用',
    color       varchar(50)      null comment '代表色',
    domain      varchar(50)      not null comment '租户信息外建',
    active_time timestamp        null comment '是否启用时间',
    update_time timestamp        null comment '修改时间'
)
    comment '上游源注册表' charset = utf8;

create table t_mgr_upstream_dept
(
    id               varchar(50) not null comment '主键'
        primary key,
    upstream_type_id varchar(50) not null comment '外键',
    dept             text        null comment '内容',
    create_time      timestamp   null comment '拉取时间'
)
    comment '上游部门表' charset = utf8;

create table t_mgr_upstream_types
(
    id                varchar(50)      not null comment '主键'
        primary key,
    upstream_id       varchar(50)      null comment '应用集成注册外建',
    description       varchar(500)     null comment '描述',
    syn_type          varchar(50)      null comment '同步类型  部门/岗位/人员',
    dept_type_id      varchar(50)      null comment '属组织机构类别外建',
    enable_prefix     bit              null comment '是否启用前缀 【规则】',
    active            bit              null comment '是否启用',
    active_time       timestamp        null comment '是否启用时间',
    root              bit default b'0' null comment '是否为根数据源【抽到新表】',
    create_time       timestamp        null comment '注册时间',
    update_time       timestamp        null comment '修改时间',
    graphql_url       varchar(255)     null comment '://服务/类型/方法',
    service_code      varchar(50)      null comment '网关数据服务id',
    domain            varchar(50)      null comment '租户外键',
    dept_tree_type_id varchar(50)      null comment '属组织机构类别树外键',
    is_page           bit              not null comment '是否分页,0为不支持',
    syn_way           int(1)           null comment ' 拉取1/推送0/自定义2',
    `is_incremental` bit NULL DEFAULT NULL COMMENT '是否增量  0为不是增量',
    `person_characteristic` varchar(50) null comment '[人特征，人员类型合重方式以及身份匹配人方式] CARD_TYPE_NO:证件类型+证件号码 CARD_NO:仅证件号码 USERNAME:用户名 EMAIL:邮箱 CELLPHONE:手机号 OPENID:openid(仅身份类型匹配人)',
    `code` varchar(50) NULL DEFAULT NULL COMMENT '机读代码',
    `builtin_data` longtext NULL DEFAULT NULL COMMENT '自定义数据 json格式'
)
    comment '上游源类型注册表' charset = utf8;

create table t_mgr_node_rules
(
    id                varchar(50)              not null comment '主键'
        primary key,
    node_id           varchar(50)              not null comment '节点外建',
    type              int                      not null comment '规则类型 0推送 1拉取 3手动',
    active            bit         default b'1' not null comment '是否启用
',
    active_time       timestamp                null,
    create_time       timestamp                null,
    update_time       timestamp                null comment '修改时间',
    service_key       varchar(255)             null comment '允许【推送】的服务标识',
    upstream_types_id varchar(50)              null comment '【拉取】所用的类型信息外建',
    inherit_id        varchar(50) default '\0' null comment '是否继承自父级',
    sort              int                      not null comment '排序',
    status            int                      not null,
    constraint fk_node
        foreign key (node_id) references t_mgr_node (id),
    constraint fk_upstream_type
        foreign key (upstream_types_id) references t_mgr_upstream_types (id)
)
    comment '节点规则集' charset = utf8;

create table t_mgr_node_rules_range
(
    id            varchar(50)  not null
        primary key,
    node_rules_id varchar(50)  not null comment '外健',
    type          int          null comment '规则类型 0 挂载 1 排除 2 重命名',
    node          varchar(255) null comment '作用节点',
    `range`       int(255)     null comment '作用域0/1；挂载（是否包含节点本身0还是仅其子树1） 排除（排除无用节点以及0/仅1其子树） ',
    create_time   timestamp    null,
    `rename`      varchar(255) null comment '重命名规则',
    update_time   timestamp    null comment '修改时间',
    status        int          not null,
    constraint t_mgr_node_rules_range_t_mgr_node_rules_id_fk
        foreign key (node_rules_id) references t_mgr_node_rules (id)
)
    charset = utf8;

create table t_mgr_upstream_types_field
(
    id               varchar(50) not null comment '主键'
        primary key,
    upstream_type_id varchar(50) not null comment '上游源数据类型注册外键',
    source_field     varchar(50) null comment '源字段名称',
    target_field     text null comment '转换后字段名称',
    create_time      timestamp   null comment '创建时间',
    update_time      timestamp   null comment '修改时间',
    domain           varchar(50) null comment '租户外键'
)
    comment '上游源类型字段映射表' charset = utf8;


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

create table t_mgr_dept_relation_type
(
	id varchar(50) null,
	code varchar(50) null comment '机构关系代码',
	name varchar(50) null comment '机构关系名称',
	domain varchar(50) null comment '所属租户',
	relation_index int null comment '排序'
)
comment '组织机构关系类型表';

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

CREATE TABLE `t_mgr_pre_view_task`  (
                                        `id` varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '主键id',
                                        `task_id` varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '任务标识',
                                        `status` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '任务状态',
                                        `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
                                        `type` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '预览类型',
                                        `domain` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '租户信息外建',
                                        `update_time` timestamp NULL DEFAULT NULL COMMENT '修改时间',
                                        `statistics` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '统计变更数量  没有变化/新增/删除/修改/无效',
                                        `reason` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'COMMENT ''详情(失败的原因)',
                                        PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `incremental_task`  (
                                     `id` varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '主键id',
                                     `type` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '类型 组织机构、岗位、人、身份',
                                     `time` timestamp(0) NULL DEFAULT NULL COMMENT '下次同步查询时间戳。（max9:_then:10。 返回数据 最小时间小于max 则取max。最大时间大于then 则取10）',
                                     `create_time` timestamp(0) NULL DEFAULT NULL COMMENT '数据获取完成时间',
                                     `upstream_type_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '权威源类型ID',
                                     `domain` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '所属租户',
                                     `operation_no` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '本次操作数量',
                                     `main_task_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '主任务id',
                                     PRIMARY KEY (`id`) USING BTREE,
                                     INDEX `type_index`(`type`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

SET FOREIGN_KEY_CHECKS = 1;

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
                        `sync_state` int(2) DEFAULT NULL COMMENT '同步后状态 0 无变化 1 新增 2 删除 3 修改  4 失效'
);
CREATE TABLE `user_type` (
                             `id` varchar(50) NOT NULL,
                             `user_type` varchar(50) DEFAULT NULL,
                             `name` varchar(50) DEFAULT NULL,
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
                             `sync_state` int(2) DEFAULT NULL COMMENT '同步后状态 0 无变化 1 新增 2 删除 3 修改  4 失效',
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
                        `sync_state` int(2) DEFAULT NULL COMMENT '同步后状态 0 无变化 1 新增 2 删除 3 修改  4 失效',
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
                            `birthday` datetime DEFAULT NULL,
                            `avatar` text DEFAULT null,
                            `tags` varchar(255) DEFAULT NULL,
                            `active` tinyint(1) DEFAULT 1,
                            `active_time` datetime DEFAULT NULL,
                            `valid_start_time` datetime DEFAULT NULL,
                            `valid_end_time` datetime DEFAULT NULL,
                            `freeze_time` datetime DEFAULT NULL,
                            `state` varchar(50) DEFAULT NULL,
                            `create_data_source` varchar(100) DEFAULT NULL COMMENT '创建来源(机读)',
                            `create_source` varchar(100) DEFAULT NULL COMMENT '创建来源',
                            `sync_state` int(2) DEFAULT NULL COMMENT '同步后状态 0 无变化 1 新增 2 删除 3 修改  4 失效',
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

CREATE TABLE `t_mgr_domain_ignore`  (
  `id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '主键',
  `domain` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '租户',
  `upstream_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '需要忽略的权威源',
  `node_rule_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '需要忽略的规则',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

SET FOREIGN_KEY_CHECKS = 1;

create table t_mgr_merge_attr_rule
(
    id              varchar(50)  not null
        primary key,
    attr_name       varchar(100) null comment '属性名称',
    entity_id       varchar(50)  not null comment '被赋值的对象',
    from_entity_id  varchar(50)  not null comment '提供值的对象',
    dynamic_attr_id varchar(50)  null comment '扩展属性情况下对应的id',
    create_time     datetime     null,
    tenant_id       varchar(50)  null
)
    comment '手工合重属性';

CREATE TABLE `t_mgr_shadow_copy`  (
                                      `id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '主键',
                                      `data` mediumblob NULL COMMENT '数据',
                                      `upstream_type_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '权威源类型id',
                                      `type` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '同步类型  dept/post/person/occupy',
                                      `domain` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '租户',
                                      `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                      PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

