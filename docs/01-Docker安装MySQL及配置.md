# Docker 安装 MySQL 8.0 及配置

> **推荐直接使用项目根目录的 `docker-compose.yml` 一键启动所有中间件。**
> 本文档仅在你需要单独安装 MySQL 时参考。

---

## 1. 拉取镜像

```powershell
docker pull mysql:8.0
```

---

## 2. 启动容器

```powershell
docker run -d `
  --name aurora-mysql `
  -p 3306:3306 `
  -e MYSQL_ROOT_PASSWORD=root123 `
  -e MYSQL_DATABASE=aurora_admin `
  -v mysql_data:/var/lib/mysql `
  -v ${PWD}/backend/src/main/resources/schema.sql:/docker-entrypoint-initdb.d/01-schema.sql `
  --restart unless-stopped `
  mysql:8.0 `
  --character-set-server=utf8mb4 `
  --collation-server=utf8mb4_unicode_ci `
  --default-time-zone=+08:00
```

> **说明**
> - `MYSQL_ROOT_PASSWORD=root123` — root 密码，应用直接使用 root 连接
> - `MYSQL_DATABASE=aurora_admin` — 容器启动时自动创建数据库
> - `01-schema.sql` — 挂载到 `docker-entrypoint-initdb.d` 会在首次启动时自动执行建表
> - 数据持久化在 named volume `mysql_data`，删容器不会丢数据

---

## 3. 验证连接

### 命令行验证

```powershell
docker exec -it aurora-mysql mysql -uroot -proot123 aurora_admin
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
| User | `root` |
| Password | `root123` |

---

## 4. 常用管理命令

```powershell
# 查看日志
docker logs aurora-mysql

# 进入容器
docker exec -it aurora-mysql bash

# 重启
docker restart aurora-mysql

# 停止
docker stop aurora-mysql

# 启动（已停止的容器）
docker start aurora-mysql

# 彻底删除（包括数据卷）
docker rm -f aurora-mysql
docker volume rm mysql_data
```

---

## 5. 连接配置对照

`application-dev.yml` 中的对应配置（已含默认值，无需修改）：

```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:aurora_admin}?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:root123}
```

Docker 启动后直接 `mvn spring-boot:run` 即可，默认值已对齐容器配置。

---

## 6. 常见问题

### schema.sql 没执行？

只有**首次创建容器且数据目录为空**时，`/docker-entrypoint-initdb.d/` 下的脚本才会执行。如果容器已运行过：

```powershell
# 手动执行 schema
docker exec -i aurora-mysql mysql -uroot -proot123 aurora_admin < backend/src/main/resources/schema.sql
```

### MySQL 8 连接报 "Public Key Retrieval is not allowed"

JDBC URL 末尾加参数 `&allowPublicKeyRetrieval=true`：

```
jdbc:mysql://localhost:3306/aurora_admin?...&allowPublicKeyRetrieval=true
```

### 数据卷在哪？

```powershell
docker volume inspect mysql_data
```

Windows 下通常在 `\\wsl$\docker-desktop-data\data\docker\volumes\`。
