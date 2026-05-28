CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    role_code VARCHAR(64) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用，0禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父菜单ID',
    menu_name VARCHAR(64) NOT NULL COMMENT '菜单名称',
    menu_type CHAR(1) NOT NULL COMMENT 'M菜单，B按钮',
    route_path VARCHAR(128) NULL COMMENT '前端路由路径',
    component VARCHAR(128) NULL COMMENT '前端组件标识',
    icon VARCHAR(64) NULL COMMENT '图标',
    permission_code VARCHAR(128) NULL COMMENT '权限标识',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    visible TINYINT NOT NULL DEFAULT 1 COMMENT '1显示，0隐藏',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用，0禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_menu_permission (permission_code),
    KEY idx_sys_menu_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单权限表';

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_menu (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联表';

INSERT INTO sys_user (username, password_hash, nickname, status)
VALUES
    ('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', '管理员', 1),
    ('operator', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', '运营专员', 1)
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    nickname = VALUES(nickname),
    status = VALUES(status);

INSERT INTO sys_role (id, role_code, role_name, status)
VALUES
    (1, 'admin', '系统管理员', 1),
    (2, 'operator', '订单运营', 1)
ON DUPLICATE KEY UPDATE
    role_code = VALUES(role_code),
    role_name = VALUES(role_name),
    status = VALUES(status);

INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, route_path, component, icon, permission_code, sort_order, visible, status)
VALUES
    (1, 0, '工作台', 'M', '/dashboard', 'DashboardView', 'House', 'dashboard:view', 10, 1, 1),
    (10, 0, '商品管理', 'M', '/products', 'ProductsView', 'Box', 'product:view', 20, 1, 1),
    (11, 10, '新增商品', 'B', NULL, NULL, NULL, 'product:add', 21, 0, 1),
    (12, 10, '编辑商品', 'B', NULL, NULL, NULL, 'product:edit', 22, 0, 1),
    (13, 10, '删除商品', 'B', NULL, NULL, NULL, 'product:delete', 23, 0, 1),
    (14, 10, '商品上下架', 'B', NULL, NULL, NULL, 'product:status', 24, 0, 1),
    (20, 0, '订单管理', 'M', '/orders', 'OrdersView', 'Tickets', 'order:view', 30, 1, 1),
    (21, 20, '订单详情', 'B', NULL, NULL, NULL, 'order:detail', 31, 0, 1),
    (22, 20, '修改订单状态', 'B', NULL, NULL, NULL, 'order:status', 32, 0, 1)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    menu_name = VALUES(menu_name),
    menu_type = VALUES(menu_type),
    route_path = VALUES(route_path),
    component = VALUES(component),
    icon = VALUES(icon),
    permission_code = VALUES(permission_code),
    sort_order = VALUES(sort_order),
    visible = VALUES(visible),
    status = VALUES(status);

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, 1
FROM sys_user u
WHERE u.username = 'admin'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, 2
FROM sys_user u
WHERE u.username = 'operator'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.id
FROM sys_menu m
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2, m.id
FROM sys_menu m
WHERE m.permission_code IN (
    'dashboard:view',
    'order:view',
    'order:detail',
    'order:status'
)
ON DUPLICATE KEY UPDATE menu_id = VALUES(menu_id);
