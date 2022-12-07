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
    type_index int null comment '排序'
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
    syn_way           int(1)           null comment ' 拉取1/推送0',
    `is_incremental` bit NULL DEFAULT NULL COMMENT '\r\n是否增量  0为不是增量',
    `person_characteristic` varchar(50) null comment '[人特征，人员类型合重方式以及身份匹配人方式] CARD_TYPE_NO:证件类型+证件号码 CARD_NO:仅证件号码 USERNAME:用户名 EMAIL:邮箱 CELLPHONE:手机号 OPENID:openid(仅身份类型匹配人)'
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
                                        PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `incremental_task`  (
                                     `id` varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '主键id',
                                     `type` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '类型 组织机构、岗位、人、身份',
                                     `time` timestamp NULL DEFAULT NULL COMMENT '下次同步查询时间戳。（max9:_then:10。 返回数据 最小时间小于max 则取max。最大时间大于then 则取10）',
                                     `create_time` timestamp NULL DEFAULT NULL COMMENT '数据获取完成时间',
                                     `upstream_type_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '权威源类型ID',
                                     `domain` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '所属租户',
                                     PRIMARY KEY (`id`) USING BTREE,
                                     INDEX `type_index`(`type`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

SET FOREIGN_KEY_CHECKS = 1;
