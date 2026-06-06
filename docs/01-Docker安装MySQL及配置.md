# Docker 安装 MySQL 8.0 及配置

---

## 1. 拉取镜像

```powershell
docker pull mysql:8.0.33
```

---

## 2. 启动容器

```powershell
docker run -d `
  --name dev-mysql `
  -p 3306:3306 `
  -e MYSQL_ROOT_PASSWORD=root123 `
  -e MYSQL_DATABASE=test_ci `
  -e MYSQL_USER=dev `
  -e MYSQL_PASSWORD='3!spPY(7rK1p-3L' `
  -v mysql_data:/var/lib/mysql `
  -v F:\other\code\ai\claudeCodeDemo3\backend\src\main\resources\schema.sql:/docker-entrypoint-initdb.d/01-schema.sql `
  --restart unless-stopped `
  mysql:8.0.33 `
  --character-set-server=utf8mb4 `
  --collation-server=utf8mb4_unicode_ci `
  --default-time-zone=+08:00
```

> **说明**
> - `MYSQL_DATABASE=test_ci` — 容器启动时自动创建数据库
> - `MYSQL_USER=dev` / `MYSQL_PASSWORD=...` — 创建应用账号（只有 test_ci 库的权限）
> - `01-schema.sql` — 挂载到 `docker-entrypoint-initdb.d` 会在首次启动时自动执行建表
> - 数据持久化在 named volume `mysql_data`，删容器不会丢数据

---

## 3. 验证连接

### 命令行验证

```powershell
docker exec -it dev-mysql mysql -udev -p'3!spPY(7rK1p-3L' test_ci
```

进去后跑个查询确认 schema 已执行：

```sql
SHOW TABLES;
```

应该能看到项目里定义的表（user、role、menu、dept 等）。

### 用 Navicat / DBeaver 等工具连接

| 参数 | 值 |
|------|-----|
| Host | `localhost` |
| Port | `3306` |
| User | `dev` |
| Password | `3!spPY(7rK1p-3L` |
| Database | `test_ci` |

---

## 4. 常用管理命令

```powershell
# 查看日志
docker logs dev-mysql

# 进入容器
docker exec -it dev-mysql bash

# 重启
docker restart dev-mysql

# 停止
docker stop dev-mysql

# 启动（已停止的容器）
docker start dev-mysql

# 彻底删除（包括数据卷）
docker rm -f dev-mysql
docker volume rm mysql_data
```

---

## 5. 连接配置对照

`application-dev.yml` 中的对应配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/test_ci?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: dev
    password: '3!spPY(7rK1p-3L'
```

默认的 `${DB_HOST:11.11.11.142}` 是团队共享数据库的地址。本地开发时改为 `localhost` 或者通过环境变量覆盖：

```powershell
# 方式一：改 application-dev.yml 第 8 行
#   ${DB_HOST:11.11.11.142} → ${DB_HOST:localhost}

# 方式二：启动时传参
$env:DB_HOST="localhost"
mvn spring-boot:run
```

---

## 6. 常见问题

### schema.sql 没执行？

只有**首次创建容器且数据目录为空**时，`/docker-entrypoint-initdb.d/` 下的脚本才会执行。如果容器已运行过：

```powershell
# 手动执行 schema
docker exec -i dev-mysql mysql -udev -p'3!spPY(7rK1p-3L' test_ci < F:\other\code\ai\claudeCodeDemo3\backend\src\main\resources\schema.sql
```

### MySQL 8 连接报 "Public Key Retrieval is not allowed"

JDBC URL 末尾加参数 `&allowPublicKeyRetrieval=true`：

```
jdbc:mysql://localhost:3306/test_ci?...&allowPublicKeyRetrieval=true
```

### 数据卷在哪？

```powershell
docker volume inspect mysql_data
```

Windows 下通常在 `\\wsl$\docker-desktop-data\data\docker\volumes\`。
