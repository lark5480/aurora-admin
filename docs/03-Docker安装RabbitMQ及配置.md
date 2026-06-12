# Docker 安装 RabbitMQ 及配置

> **推荐直接使用项目根目录的 `docker-compose.yml` 一键启动所有中间件。**
> 本文档仅在你需要单独安装 RabbitMQ 时参考。

---

## 1. 拉取镜像

```powershell
# 带管理面板的版本
docker pull rabbitmq:3-management-alpine
```

> `management` 标签内置了管理面板插件，省去手动 `rabbitmq-plugins enable` 步骤。

---

## 2. 启动容器

```powershell
docker run -d `
  --name aurora-rabbitmq `
  -p 5672:5672 `
  -p 15672:15672 `
  -e RABBITMQ_DEFAULT_USER=admin `
  -e RABBITMQ_DEFAULT_PASS=admin123 `
  -v rabbitmq_data:/var/lib/rabbitmq `
  --restart unless-stopped `
  rabbitmq:3-management-alpine
```

| 端口 | 用途 |
|------|------|
| `5672` | AMQP 协议端口（应用连接用） |
| `15672` | 管理面板 HTTP 端口（浏览器访问） |

---

## 3. 验证

### 浏览器访问管理面板

打开 `http://localhost:15672`

| 参数 | 值 |
|------|-----|
| Username | `admin` |
| Password | `admin123` |

登录后能看到 Overview 页，确认 RabbitMQ 版本、连接数、队列数等信息。

### 命令行验证

```powershell
# 查看状态
docker exec aurora-rabbitmq rabbitmqctl status

# 列出用户
docker exec aurora-rabbitmq rabbitmqctl list_users
```

---

## 4. 常用管理命令

```powershell
# 查看日志
docker logs aurora-rabbitmq

# 进入容器
docker exec -it aurora-rabbitmq sh

# 重启
docker restart aurora-rabbitmq

# 停止
docker stop aurora-rabbitmq

# 彻底删除
docker rm -f aurora-rabbitmq
docker volume rm rabbitmq_data
```

---

## 5. 连接配置对照

`application-dev.yml` 中的对应配置：

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:admin}
    password: ${RABBITMQ_PASSWORD:admin123}
    publisher-confirm-type: correlated
    publisher-returns: true
    listener:
      simple:
        acknowledge-mode: manual
        prefetch: 10
        retry:
          enabled: true
          initial-interval: 3000
          max-attempts: 3
          multiplier: 2
```

Spring Boot 项目启动时会自动建立连接。如果连不上，日志里会看到 `AmqpConnectException`。

---

## 6. 常见问题

### 管理面板无法访问？

等几秒——容器刚启动时插件加载需要时间。如果持续不行：

```powershell
# 检查日志
docker logs aurora-rabbitmq

# 手动启用管理插件（management 镜像已包含，一般不需要）
docker exec aurora-rabbitmq rabbitmq-plugins enable rabbitmq_management
```

### 连接被拒绝？

1. 确认容器在跑：`docker ps | findstr rabbitmq`
2. 确认没有其他服务占用 5672：`netstat -ano | findstr 5672`
3. 确认 `application-dev.yml` 的 host 是 `localhost`（不是远程地址）

### 消息积压了怎么查？

在管理面板 `http://localhost:15672` → Queues 标签页，可以看到每个队列的消息数量。也可以命令行：

```powershell
docker exec aurora-rabbitmq rabbitmqctl list_queues name messages messages_ready messages_unacknowledged
```
