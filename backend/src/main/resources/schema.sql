-- ========================================
-- 用户表
-- ========================================
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密）',
    email VARCHAR(100) COMMENT '邮箱',
    avatar VARCHAR(255) COMMENT '头像URL',
    nickname VARCHAR(50) COMMENT '昵称',
    role VARCHAR(20) DEFAULT 'user' COMMENT '角色标识',
    dept_id BIGINT COMMENT '所属部门ID',
    dept_name VARCHAR(50) COMMENT '部门名称',
    status INT DEFAULT 1 COMMENT '状态 1:启用 0:禁用',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除 0:否 1:是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_dept_id (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ========================================
-- 部门表
-- ========================================
CREATE TABLE IF NOT EXISTS t_dept (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父部门ID，0为根',
    name VARCHAR(64) NOT NULL COMMENT '部门名称',
    code VARCHAR(64) NOT NULL UNIQUE COMMENT '部门编码',
    sort_order INT DEFAULT 0 COMMENT '排序',
    leader_user_id BIGINT COMMENT '负责人用户ID',
    leader_name VARCHAR(50) COMMENT '负责人姓名',
    phone VARCHAR(20) COMMENT '联系电话',
    email VARCHAR(100) COMMENT '联系邮箱',
    status TINYINT DEFAULT 1 COMMENT '状态 1:启用 0:禁用',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除 0:否 1:是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_id (parent_id),
    INDEX idx_leader_user_id (leader_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- ========================================
-- 操作日志表
-- ========================================
CREATE TABLE IF NOT EXISTS t_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '操作用户ID',
    username VARCHAR(50) NOT NULL COMMENT '操作用户名',
    operation VARCHAR(100) NOT NULL COMMENT '操作描述',
    method VARCHAR(10) COMMENT 'HTTP方法',
    url VARCHAR(500) COMMENT '请求URL',
    ip VARCHAR(50) COMMENT '客户端IP',
    params TEXT COMMENT '请求参数',
    status INT DEFAULT 1 COMMENT '状态 1:成功 0:失败',
    duration_ms INT COMMENT '执行耗时（毫秒）',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除 0:否 1:是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ========================================
-- 系统配置表
-- ========================================
CREATE TABLE IF NOT EXISTS t_system_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    config_type VARCHAR(50) DEFAULT 'string' COMMENT '配置类型: string/number/boolean',
    config_name VARCHAR(100) COMMENT '配置名称',
    config_group VARCHAR(50) COMMENT '配置分组',
    description VARCHAR(500) COMMENT '配置描述',
    is_visible TINYINT DEFAULT 1 COMMENT '是否前端可见 1:是 0:否',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除 0:否 1:是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 系统默认配置（使用 INSERT IGNORE 避免重复插入）
INSERT IGNORE INTO t_system_config (config_key, config_value, config_type, config_name, config_group, description, is_visible) VALUES
('site.name', 'Admin System', 'string', '站点名称', 'site', '网站全局名称，显示在标题栏和登录页', 1),
('site.logo', '', 'string', '站点Logo', 'site', 'Logo文字，留空使用默认图标', 1),
('site.footer', '', 'string', '页脚文字', 'site', '页面底部版权文字，留空不显示', 1),
('upload.max_size', '10', 'number', '上传大小限制(MB)', 'upload', '文件上传最大大小，单位MB', 1),
('upload.allowed_types', '', 'string', '允许上传类型', 'upload', '逗号分隔的扩展名，如 .jpg,.png,.pdf，留空不限制', 1),
('register.enabled', 'true', 'boolean', '开放注册', 'system', '是否允许新用户注册', 1),
('notice.banner', '', 'string', '首页公告', 'notice', '首页顶部公告横幅内容，留空不显示', 1),
('ws.enabled', 'true', 'boolean', 'WebSocket', 'system', '是否开启WebSocket实时消息推送', 1);

-- ========================================
-- 每日统计表
-- ========================================
CREATE TABLE IF NOT EXISTS t_daily_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    stat_date DATE NOT NULL COMMENT '统计日期',
    stat_type VARCHAR(50) NOT NULL COMMENT '统计类型',
    stat_count INT DEFAULT 0 COMMENT '统计数量',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除 0:否 1:是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_date_type (stat_date, stat_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日统计表';

-- ========================================
-- 消息通知表
-- ========================================
CREATE TABLE IF NOT EXISTS t_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    title VARCHAR(200) NOT NULL COMMENT '消息标题',
    content TEXT COMMENT '消息内容',
    type VARCHAR(50) NOT NULL COMMENT '消息类型',
    priority VARCHAR(20) DEFAULT 'normal' COMMENT '优先级: normal/high/urgent',
    sender_id BIGINT COMMENT '发送者ID',
    sender_name VARCHAR(50) COMMENT '发送者名称',
    recipient_id BIGINT NOT NULL COMMENT '接收者ID',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读 0:未读 1:已读',
    read_time DATETIME COMMENT '阅读时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除 0:否 1:是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_recipient (recipient_id),
    INDEX idx_is_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知表';

-- ========================================
-- 系统公告表
-- ========================================
CREATE TABLE IF NOT EXISTS t_notice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    title VARCHAR(200) NOT NULL COMMENT '公告标题',
    content TEXT COMMENT '公告内容（富文本HTML）',
    target_type VARCHAR(20) NOT NULL DEFAULT 'ALL' COMMENT '可见范围类型: ALL/DEPT/USER',
    target_ids JSON COMMENT '目标ID列表（部门ID或用户ID）',
    publish_time DATETIME COMMENT '定时发布时间，为空则立即发布',
    expire_time DATETIME COMMENT '过期时间',
    status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PUBLISHED/EXPIRED/WITHDRAWN',
    create_by BIGINT COMMENT '创建人ID',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除 0:否 1:是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (status),
    INDEX idx_publish_time (publish_time),
    INDEX idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告表';

-- 初始化系统公告
INSERT IGNORE INTO t_notice (id, title, content, target_type, publish_time, status, create_by) VALUES
(1, '欢迎使用 Aurora Admin 管理系统', '<h3>🎉 系统功能概览</h3>'
'<table style="width:100%;border-collapse:collapse;">'
'<tr><td style="padding:8px 12px;border-bottom:1px solid rgba(255,255,255,0.1);"><b>🛒 商品与订单</b></td><td style="padding:8px 12px;border-bottom:1px solid rgba(255,255,255,0.1);">商品浏览与多规格 SKU、购物车、下单支付、收货地址管理、快递追踪</td></tr>'
'<tr><td style="padding:8px 12px;border-bottom:1px solid rgba(255,255,255,0.1);"><b>🔙 售后服务</b></td><td style="padding:8px 12px;border-bottom:1px solid rgba(255,255,255,0.1);">仅退款 / 退货退款申请、审核流程、整单售后</td></tr>'
'<tr><td style="padding:8px 12px;border-bottom:1px solid rgba(255,255,255,0.1);"><b>📁 文件管理</b></td><td style="padding:8px 12px;border-bottom:1px solid rgba(255,255,255,0.1);">文件上传、下载、预览，支持多种格式</td></tr>'
'<tr><td style="padding:8px 12px;border-bottom:1px solid rgba(255,255,255,0.1);"><b>💬 消息中心</b></td><td style="padding:8px 12px;border-bottom:1px solid rgba(255,255,255,0.1);">站内信通知，WebSocket 实时推送未读提醒</td></tr>'
'<tr><td style="padding:8px 12px;border-bottom:1px solid rgba(255,255,255,0.1);"><b>📊 数据统计</b></td><td style="padding:8px 12px;border-bottom:1px solid rgba(255,255,255,0.1);">可视化数据面板，用户/文件/消息/订单趋势图与分布图</td></tr>'
'<tr><td style="padding:8px 12px;border-bottom:1px solid rgba(255,255,255,0.1);"><b>👥 用户与权限</b></td><td style="padding:8px 12px;border-bottom:1px solid rgba(255,255,255,0.1);">部门管理、角色管理、菜单管理，RBAC 细粒度权限控制</td></tr>'
'<tr><td style="padding:8px 12px;border-bottom:1px solid rgba(255,255,255,0.1);"><b>📢 公告管理</b></td><td style="padding:8px 12px;border-bottom:1px solid rgba(255,255,255,0.1);">富文本公告发布，支持定时发布与可见范围控制</td></tr>'
'<tr><td style="padding:8px 12px;border-bottom:1px solid rgba(255,255,255,0.1);"><b>📋 操作日志</b></td><td style="padding:8px 12px;border-bottom:1px solid rgba(255,255,255,0.1);">全操作审计记录，敏感操作可追溯</td></tr>'
'<tr><td style="padding:8px 12px;"><b>⚙️ 系统配置</b></td><td style="padding:8px 12px;">站点名称、Logo、上传限制、注册开关等动态配置</td></tr>'
'</table>'
'<p style="margin-top:12px;color:var(--text-muted, #888);">默认管理员账号 admin / admin123，普通用户账号 user / 123456</p>', 'ALL', NOW(), 'PUBLISHED', 1),
(2, '账户安全提醒', '<p>为保障您的账户安全，请注意以下事项：</p><ul><li>定期修改登录密码，避免使用简单密码</li><li>不要在公共网络环境下登录系统</li><li>离开时请及时退出或锁定页面</li><li>如发现账户异常，请立即联系管理员</li></ul><p>系统将自动记录所有敏感操作，请规范使用。</p>', 'ALL', NOW(), 'PUBLISHED', 1),
(3, '普通用户也能浏览商品啦', '<p>本次更新为普通用户开放了商品浏览功能：</p><ul><li><b>首页推荐</b> — 登录即可看到热门商品卡片，一键加入购物车</li><li><b>商品浏览</b> — 支持按名称搜索、按分类筛选、查看商品详情与多规格 SKU</li><li><b>完整购物链路</b> — 浏览 → 加购 → 下单 → 支付 → 收货 → 售后，全流程打通</li></ul><p>赶紧去首页逛逛吧 🚀</p>', 'ALL', NOW(), 'PUBLISHED', 1);

-- ========================================
-- 文件记录表
-- ========================================
CREATE TABLE IF NOT EXISTS t_file (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名称',
    file_path VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    file_size BIGINT COMMENT '文件大小（字节）',
    file_type VARCHAR(50) COMMENT '文件MIME类型',
    file_ext VARCHAR(20) COMMENT '文件扩展名',
    upload_user_id BIGINT NOT NULL COMMENT '上传用户ID',
    upload_username VARCHAR(50) COMMENT '上传用户名',
    download_count INT DEFAULT 0 COMMENT '下载次数',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除 0:否 1:是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_upload_user (upload_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件记录表';

-- ========================================
-- 角色表
-- ========================================
CREATE TABLE IF NOT EXISTS t_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    code VARCHAR(64) NOT NULL UNIQUE COMMENT '角色编码，如: ADMIN',
    name VARCHAR(64) NOT NULL COMMENT '角色名称',
    description VARCHAR(255) COMMENT '描述',
    data_scope TINYINT DEFAULT 1 COMMENT '数据权限范围: 1:全部 2:本部门及下级 3:本部门 4:仅本人',
    is_system TINYINT DEFAULT 0 COMMENT '是否系统内置角色（不可删除）',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态 1:启用 0:禁用',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除 0:否 1:是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ========================================
-- 用户-角色关联表（多对多）
-- ========================================
CREATE TABLE IF NOT EXISTS t_user_role (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (user_id, role_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联表';

-- ========================================
-- 角色-菜单关联表（多对多）
-- ========================================
CREATE TABLE IF NOT EXISTS t_role_menu (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (role_id, menu_id),
    INDEX idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-菜单关联表';

-- ========================================
-- 菜单表（支持两级菜单）
-- ========================================
CREATE TABLE IF NOT EXISTS t_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父菜单ID，0为根',
    name VARCHAR(64) NOT NULL COMMENT '菜单名称',
    path VARCHAR(255) COMMENT '路由路径',
    component VARCHAR(255) COMMENT '前端组件路径',
    menu_type TINYINT NOT NULL COMMENT '菜单类型 1:目录 2:菜单 3:按钮',
    icon VARCHAR(64) COMMENT '图标',
    sort_order INT DEFAULT 0 COMMENT '排序',
    permission VARCHAR(128) COMMENT '权限标识，如: system:user:add',
    status TINYINT DEFAULT 1 COMMENT '状态 1:启用 0:禁用',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除 0:否 1:是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_id (parent_id),
    INDEX idx_permission (permission)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

-- 初始化系统内置角色
INSERT IGNORE INTO t_role (code, name, description, data_scope, is_system, status) VALUES
('SUPER_ADMIN', '超级管理员', '拥有系统所有权限', 1, 1, 1),
('ADMIN', '管理员', '拥有管理后台的权限', 1, 0, 1),
('USER', '普通用户', '普通用户权限', 4, 0, 1);

-- 初始化默认用户（密码: admin123 / 123456）
INSERT IGNORE INTO t_user (username, password, email, role, status) VALUES
('admin', '$2b$10$3dtP0ZCNC67nSecv.lol.ey2TaDwbDLs6iWWigsI5MVI6UWdeVYHi', 'admin@example.com', 'admin', 1),
('user', '$2b$10$xBoLZ0HJ2GagPO.mDd7J/ORX2oByw7jI2dfVYM.PmwIIw2cXq8luu', 'user@example.com', 'user', 1);

-- 用户-角色关联
INSERT IGNORE INTO t_user_role (user_id, role_id)
SELECT u.id, r.id FROM t_user u, t_role r
WHERE u.username = 'admin' AND r.code = 'SUPER_ADMIN';

INSERT IGNORE INTO t_user_role (user_id, role_id)
SELECT u.id, r.id FROM t_user u, t_role r
WHERE u.username = 'user' AND r.code = 'USER';

-- 初始化默认部门
INSERT IGNORE INTO t_dept (parent_id, name, code, sort_order, status) VALUES
(0, '管理部', 'ADMIN_DEPT', 1, 1);

-- admin 归属默认部门
UPDATE t_user u
JOIN t_dept d ON d.code = 'ADMIN_DEPT'
SET u.dept_id = d.id, u.dept_name = d.name
WHERE u.username = 'admin' AND u.dept_id IS NULL;

-- 初始化默认菜单数据
-- 一级目录
INSERT IGNORE INTO t_menu (id, parent_id, name, path, component, menu_type, icon, sort_order, permission, status) VALUES
(1, 0, '系统管理', '/system', NULL, 1, 'Setting', 1, NULL, 1),
(2, 0, '用户管理', '/users', NULL, 1, 'User', 2, NULL, 1),
(3, 0, '数据统计', '/stats', NULL, 1, 'DataAnalysis', 3, NULL, 1),
(4, 0, '文件管理', '/files', NULL, 1, 'Folder', 4, NULL, 1),
(5, 0, '消息中心', '/messages', NULL, 1, 'Message', 5, NULL, 1),
(6, 0, '操作日志', '/logs', NULL, 1, 'Document', 6, NULL, 1),
(7, 0, '公告管理', '/notice', NULL, 1, 'Bell', 7, NULL, 1),
(8, 0, '商品管理', '/product', NULL, 1, 'ShoppingBag', 8, NULL, 1),
(9, 0, '订单管理', '/order', NULL, 1, 'Tickets', 9, NULL, 1);

-- 二级菜单
INSERT IGNORE INTO t_menu (id, parent_id, name, path, component, menu_type, icon, sort_order, permission, status) VALUES
-- 系统管理子菜单
(101, 1, '部门管理', 'departments', 'system/DeptManagement', 2, 'OfficeBuilding', 1, 'system:dept:list', 1),
(102, 1, '角色管理', 'roles', 'system/RoleManagement', 2, 'Key', 2, 'system:role:list', 1),
(103, 1, '菜单管理', 'menus', 'system/MenuManagement', 2, 'Menu', 3, 'system:menu:list', 1),
(104, 1, '系统配置', 'settings', 'system/SystemSettings', 2, 'Tools', 4, 'system:config:list', 1),
-- 用户管理子菜单
(201, 2, '用户列表', 'users', 'user/UserManagement', 2, 'User', 1, 'system:user:list', 1),
-- 数据统计子菜单
(301, 3, '数据面板', 'stats', 'statistics/Statistics', 2, 'DataAnalysis', 1, 'system:stats:list', 1),
-- 文件管理子菜单
(401, 4, '文件列表', 'files', 'file/FileManagement', 2, 'Folder', 1, 'system:file:list', 1),
-- 消息中心子菜单
(501, 5, '消息列表', 'messages', 'message/MessageCenter', 2, 'Message', 1, 'system:message:list', 1),
-- 操作日志子菜单
(601, 6, '日志列表', 'logs', 'log/OperationLogs', 2, 'Document', 1, 'system:log:list', 1),
-- 公告管理子菜单
(701, 7, '公告管理', 'notice', 'notice/NoticeManagement', 2, 'Bell', 1, 'system:notice:list', 1),
-- 商品管理子菜单
(801, 8, '商品列表', 'product', 'product/ProductManagement', 2, 'List', 1, 'system:product:list', 1),
(802, 8, '商品分类', 'category', 'product/CategoryManagement', 2, 'Grid', 2, 'system:category:list', 1),
-- 订单管理子菜单
(901, 9, '订单列表', 'order', 'order/OrderManagement', 2, 'DocumentChecked', 1, 'system:order:list', 1),
(902, 9, '收货地址', 'address', 'order/AddressManagement', 2, 'MapLocation', 2, 'system:address:list', 1),
(903, 9, '购物车', 'cart', 'order/ShoppingCart', 2, 'ShoppingCart', 3, 'system:cart:list', 1),
(904, 9, '售后管理', 'afterSales', 'order/AfterSaleManagement', 2, 'WarningFilled', 4, 'system:afterSale:list', 1);

-- 为超级管理员分配所有菜单
INSERT IGNORE INTO t_role_menu (role_id, menu_id)
SELECT 1, id FROM t_menu;

-- 为普通用户分配菜单（个人功能：文件管理、消息中心、订单管理、商品浏览）
-- 注意：父级目录和子菜单必须同时分配，否则菜单树构建时子菜单会成为孤儿不显示
INSERT IGNORE INTO t_role_menu (role_id, menu_id) VALUES
(3, 4),    -- 文件管理（目录）
(3, 401),  -- 文件列表
(3, 5),    -- 消息中心（目录）
(3, 501),  -- 消息列表
(3, 8),    -- 商品管理（目录）
(3, 801),  -- 商品列表
(3, 9),    -- 订单管理（目录）
(3, 901),  -- 订单列表
(3, 902),  -- 收货地址
(3, 903),  -- 购物车
(3, 904);  -- 售后管理

-- ========================================
-- 商品分类表
-- ========================================
CREATE TABLE IF NOT EXISTS t_product_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID，0为顶级',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    sort_order INT DEFAULT 0 COMMENT '排序',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除 0:否 1:是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- ========================================
-- 商品表
-- ========================================
CREATE TABLE IF NOT EXISTS t_product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    name VARCHAR(200) NOT NULL COMMENT '商品名称',
    description TEXT COMMENT '描述',
    cover_image VARCHAR(500) COMMENT '封面图',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    stock INT DEFAULT 0 COMMENT '库存',
    status VARCHAR(20) DEFAULT 'ON_SALE' COMMENT '状态: ON_SALE/OFF_SHELF',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除 0:否 1:是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_category (category_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- ========================================
-- 商品SKU表（多规格）
-- ========================================
CREATE TABLE IF NOT EXISTS t_product_sku (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    spec_name VARCHAR(100) COMMENT '规格名称，如"红色;XL"',
    spec_code VARCHAR(200) COMMENT '规格编码',
    price DECIMAL(10,2) COMMENT '规格价格',
    stock INT DEFAULT 0 COMMENT '规格库存',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除 0:否 1:是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SKU表';

-- ========================================
-- 购物车表
-- ========================================
CREATE TABLE IF NOT EXISTS t_shopping_cart (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sku_id BIGINT DEFAULT NULL COMMENT 'SKU ID',
    quantity INT NOT NULL DEFAULT 1 COMMENT '数量',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除 0:否 1:是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_product_sku (user_id, product_id, sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- ========================================
-- 订单表
-- ========================================
CREATE TABLE IF NOT EXISTS t_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    order_no VARCHAR(32) NOT NULL UNIQUE COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '总金额',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/PAID/SHIPPED/COMPLETED/CANCELLED/REFUNDING/REFUNDED',
    receiver_name VARCHAR(50) COMMENT '收货人姓名',
    receiver_phone VARCHAR(20) COMMENT '收货人电话',
    receiver_address VARCHAR(500) COMMENT '收货地址',
    remark VARCHAR(500) COMMENT '订单备注',
    tracking_number VARCHAR(50) COMMENT '快递单号',
    pay_time DATETIME COMMENT '支付时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除 0:否 1:是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user (user_id),
    INDEX idx_order_no (order_no),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ========================================
-- 订单明细表
-- ========================================
CREATE TABLE IF NOT EXISTS t_order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sku_id BIGINT COMMENT 'SKU ID',
    product_name VARCHAR(200) COMMENT '商品名称（快照）',
    spec_name VARCHAR(100) COMMENT '规格名称（快照）',
    price DECIMAL(10,2) NOT NULL COMMENT '购买单价',
    quantity INT NOT NULL COMMENT '购买数量',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    refund_status VARCHAR(20) DEFAULT 'NONE' COMMENT '退款状态: NONE(未退款)/REFUNDED(已退款)',
    INDEX idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- ========================================
-- 售后表
-- ========================================
CREATE TABLE IF NOT EXISTS t_after_sale (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    after_sale_no VARCHAR(32) NOT NULL UNIQUE COMMENT '售后单号',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_item_id BIGINT NOT NULL COMMENT '订单明细ID',
    user_id BIGINT NOT NULL COMMENT '申请用户ID',
    type VARCHAR(10) NOT NULL COMMENT '类型: REFUND(仅退款) / RETURN(退货退款)',
    reason VARCHAR(500) COMMENT '申请原因',
    refund_amount DECIMAL(10,2) NOT NULL COMMENT '退款金额',
    original_order_status VARCHAR(20) COMMENT '申请时的原始订单状态',
    status VARCHAR(20) NOT NULL DEFAULT 'APPLIED' COMMENT '状态: APPLIED(售后中)/COMPLETED(已退款)/REJECTED(已驳回)',
    review_remark VARCHAR(500) COMMENT '审核备注',
    review_time DATETIME COMMENT '审核时间',
    reviewer_id BIGINT COMMENT '审核人ID',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除 0:否 1:是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_after_sale_order (order_id),
    INDEX idx_after_sale_user (user_id),
    INDEX idx_after_sale_order_item (order_item_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后表';

-- ========================================
-- 支付记录表
-- ========================================
CREATE TABLE IF NOT EXISTS t_payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_no VARCHAR(32) NOT NULL COMMENT '订单号',
    transaction_no VARCHAR(64) COMMENT '支付流水号',
    amount DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    pay_method VARCHAR(20) COMMENT '支付方式: ALIPAY/WECHAT/CASH',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING/SUCCESS/FAILED/REFUNDED',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除 0:否 1:是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_order (order_id),
    INDEX idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付记录表';

-- ========================================
-- 收货地址表
-- ========================================
CREATE TABLE IF NOT EXISTS t_user_address (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    receiver_name VARCHAR(50) NOT NULL COMMENT '收货人姓名',
    receiver_phone VARCHAR(20) NOT NULL COMMENT '收货人电话',
    province VARCHAR(50) COMMENT '省份',
    city VARCHAR(50) COMMENT '城市',
    district VARCHAR(50) COMMENT '区/县',
    detail VARCHAR(200) NOT NULL COMMENT '详细地址',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认地址 0:否 1:是',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除 0:否 1:是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';

-- ========================================
-- MQ 消息补偿表（可靠消息最终一致性）
-- ========================================
CREATE TABLE IF NOT EXISTS t_mq_message_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    message_id VARCHAR(64) NOT NULL UNIQUE COMMENT '消息唯一ID（幂等Key）',
    exchange_name VARCHAR(128) NOT NULL COMMENT '交换机名称',
    routing_key VARCHAR(128) NOT NULL COMMENT '路由Key',
    message_body TEXT NOT NULL COMMENT '消息体（JSON）',
    status TINYINT DEFAULT 0 COMMENT '状态 0:待发送 1:已发送 2:发送失败',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    max_retry INT DEFAULT 5 COMMENT '最大重试次数',
    next_retry_time DATETIME COMMENT '下次重试时间',
    error_msg VARCHAR(500) COMMENT '最近一次错误信息',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除 0:否 1:是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status_retry (status, next_retry_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MQ消息补偿表';

-- ========================================
-- 示例数据：商品分类 + 商品 + SKU
-- 仅首次建库时写入（INSERT IGNORE），已有数据不受影响
-- ========================================

-- 商品分类
INSERT IGNORE INTO t_product_category (id, parent_id, name, sort_order) VALUES
(1, 0, '电子产品', 1),
(2, 0, '服装配饰', 2),
(3, 0, '食品饮料', 3),
(4, 0, '图书办公', 4);

-- 商品（10件，覆盖4个分类）
INSERT IGNORE INTO t_product (id, category_id, name, description, cover_image, price, stock, status) VALUES
(1, 1, '机械键盘 K870T', '87键紧凑布局，热插拔轴体，RGB 背光，Type-C 键线分离。兼容 Windows / macOS 双系统。', '', 299.00, 50, 'ON_SALE'),
(2, 1, '无线蓝牙耳机 Pro', '主动降噪，40小时超长续航，蓝牙 5.3，IPX5 防水。支持双设备同时连接。', '', 199.00, 80, 'ON_SALE'),
(3, 1, '无线充电鼠标垫', '15W 快充兼容 Qi 协议，100×50cm 大尺寸。防滑底垫，智能温控。', '', 59.00, 45, 'ON_SALE'),
(4, 2, '极简双肩背包', '防泼水面料，可容纳 15.6 英寸笔记本。多隔层收纳，背部透气减震设计。', '', 159.00, 30, 'ON_SALE'),
(5, 2, '纯棉圆领T恤', '260g 高克重新疆长绒棉，宽松版型，不易变形。多色多尺码可选。', '', 79.00, 380, 'ON_SALE'),
(6, 3, '明前龙井 100g', '西湖产区明前头采，手工炒制。色泽翠绿，豆香馥郁，回甘悠长。', '', 128.00, 100, 'ON_SALE'),
(7, 3, '精品咖啡豆 哥伦比亚 500g', '慧兰产区中度烘焙，焦糖与坚果风味。下单现烘，48 小时发货。', '', 88.00, 60, 'ON_SALE'),
(8, 3, '保温随手杯 480ml', '316 不锈钢内胆，12 小时保温 / 保冷。食品级硅胶密封圈，防滑杯底。', '', 49.00, 120, 'ON_SALE'),
(9, 4, '深入理解Java虚拟机（第3版）', '周志明著。全面讲解 JVM 内存管理、类加载、编译优化与并发编程。', '', 79.00, 40, 'ON_SALE'),
(10, 4, 'A5 皮质笔记本', '头层牛皮封面，96 页道林纸。磁吸搭扣设计，可平摊书写，多色可选。', '', 29.90, 150, 'ON_SALE');

-- SKU 规格（纯棉T恤 id=5：颜色 + 尺码。有 SKU 时商品库存自动 = 各 SKU 库存之和）
INSERT IGNORE INTO t_product_sku (product_id, spec_name, price, stock) VALUES
(5, '白色 / S',  79.00, 50),
(5, '白色 / M',  79.00, 60),
(5, '白色 / L',  79.00, 55),
(5, '白色 / XL', 79.00, 35),
(5, '黑色 / S',  79.00, 45),
(5, '黑色 / M',  79.00, 55),
(5, '黑色 / L',  79.00, 50),
(5, '黑色 / XL', 79.00, 30);
