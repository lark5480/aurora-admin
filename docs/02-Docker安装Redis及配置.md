# Docker 安装 Redis 及配置

> **本地开发使用单机 Redis**，简单够用。线上生产环境使用 Redis Cluster，见 `application-prod.yml`。

---

## 本地开发：单机 Redis（推荐）

### 1. 拉取镜像 & 启动

```powershell
docker pull redis:7.0-alpine

docker run -d `
  --name dev-redis `
  -p 6379:6379 `
  -v redis_data:/data `
  --restart unless-stopped `
  redis:7.0-alpine `
  redis-server --appendonly yes --protected-mode no
```

一行搞定，AOF 持久化开，protected-mode 关（Docker 内部走 bridge 网络不影响）。

### 2. 验证

```powershell
docker exec -it dev-redis redis-cli ping
# 返回 PONG

docker exec -it dev-redis redis-cli
127.0.0.1:6379> set foo bar
127.0.0.1:6379> get foo
# "bar"
```

### 3. 连接配置对照

`application-dev.yml` 已默认为单机配置：

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
```

### 4. 常用命令

```powershell
docker logs dev-redis                 # 查看日志
docker restart dev-redis              # 重启
docker exec -it dev-redis redis-cli   # 进入 CLI

# 彻底删除
docker rm -f dev-redis
docker volume rm redis_data
```

---

## 线上环境：Redis Cluster（生产对标）

> 生产环境使用 3 主 3 从集群，配置见 `application-prod.yml`。本地如果要对标线上做验证，走下面这套。

### 1. 拉取镜像

```powershell
docker pull redis:7.0-alpine
```

### 2. 手动搭建

```powershell
# 1) 创建网络
docker network create redis-net

# 2) 启动 6 个节点（7000-7005）
for ($i=0; $i -lt 6; $i++) {
    docker run -d `
        --name redis-node-$i `
        --net redis-net `
        -p $((7000+$i)):6379 `
        -v redis_data_$i:/data `
        --restart unless-stopped `
        redis:7.0-alpine `
        redis-server --cluster-enabled yes --cluster-config-file nodes.conf `
                     --appendonly yes --protected-mode no
}

# 3) 等节点就绪后创建集群
$ips = @()
for ($i=0; $i -lt 6; $i++) {
    $ip = docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' "redis-node-$i"
    $ips += "$ip`：6379"
}
docker exec redis-node-0 redis-cli --cluster create $($ips -join ' ') --cluster-replicas 1 --cluster-yes
```

### 3. 验证集群

```powershell
docker exec -it redis-node-0 redis-cli -p 6379 -c

# 查看集群拓扑
cluster nodes

# 写入测试（-c 开启集群模式，自动 redirect 到正确节点）
set foo bar
get foo
```

期望看到 6 个节点，3 master + 3 slave。

### 4. 配置对照

`application-prod.yml` 中的集群配置（密码和节点列表通过环境变量注入）：

```yaml
spring:
  data:
    redis:
      cluster:
        nodes: ${REDIS_CLUSTER_NODES}
        max-redirects: 3
      password: ${REDIS_PASSWORD}
      timeout: 5000ms
      lettuce:
        pool:
          max-active: 16
          max-idle: 16
          min-idle: 4
```

本地模拟时，在 `application-dev.yml` 里临时切到集群即可（已有注释掉的示例）。

### 5. 集群管理命令

```powershell
docker ps --filter "name=redis-node"   # 查看节点
docker logs redis-node-0               # 节点日志

# 停止/重启全部
for ($i=0; $i -lt 6; $i++) { docker stop redis-node-$i }
for ($i=0; $i -lt 6; $i++) { docker start redis-node-$i }

# 彻底删除
for ($i=0; $i -lt 6; $i++) { docker rm -f redis-node-$i }
for ($i=0; $i -lt 6; $i++) { docker volume rm redis_data_$i }
docker network rm redis-net
```

---

## 单机 ↔ 集群切换

Spring Data Redis 的单机配置和集群配置是**互斥的**——不能同时存在，否则启动报错。

| 场景 | 改哪个文件 | 操作 |
|------|-----------|------|
| 本地开发 | `application-dev.yml` | 用单机 `host:port`，注释掉 `cluster` 块 |
| 对标线上验证 | `application-dev.yml` | 注释单机，打开 `cluster` 块 |
| 线上 | `application-prod.yml` | 走 `cluster`，节点通过 `REDIS_CLUSTER_NODES` 注入 |

---

## 常见问题

### 集群状态变成 fail？

```powershell
docker exec -it redis-node-0 redis-cli --cluster fix 127.0.0.1:7000
```

### 端口被占用？

```powershell
netstat -ano | findstr 6379   # 单机
netstat -ano | findstr 7000   # 集群
```

### Spring Boot 连不上集群？

Lettuce 客户端需要连上全部节点才能初始化。确认 yml 的 nodes 列表可访问。如果之前用的单机模式，确保注释掉了 `host:port` 配置。
