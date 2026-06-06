# Docker 安装 Elasticsearch 及配置

> 本项目使用 ES 7.x，单节点模式，无安全认证。

---

## 1. 拉取镜像

```powershell
docker pull elasticsearch:7.17.15
```

> 为什么是 7.17.x？这是 ES 7.x 的最后一个稳定版，Spring Boot 3 + `spring-boot-starter-data-elasticsearch` 兼容性最好。

---

## 2. 启动容器

```powershell
docker run -d `
  --name dev-elasticsearch `
  -p 9200:9200 `
  -p 9300:9300 `
  -e "discovery.type=single-node" `
  -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" `
  -e "xpack.security.enabled=false" `
  -v es_data:/usr/share/elasticsearch/data `
  --restart unless-stopped `
  elasticsearch:7.17.15
```

| 参数 | 说明 |
|------|------|
| `discovery.type=single-node` | 单节点模式，跳过集群发现 |
| `ES_JAVA_OPTS=-Xms512m -Xmx512m` | 限制 JVM 堆内存（开发机内存有限，别给太高） |
| `xpack.security.enabled=false` | 关闭安全认证，本地开发省事 |
| `-v es_data:/usr/share/elasticsearch/data` | 数据持久化 |

> **Windows 注意**：ES 需要 `vm.max_map_count` 至少 262144。Docker Desktop + WSL 2 环境通常已满足。如果启动报 `max virtual memory areas vm.max_map_count [65530] is too low`，执行：

```powershell
wsl -d docker-desktop sysctl -w vm.max_map_count=262144
```

---

## 3. 验证

### HTTP 接口验证

```powershell
curl http://localhost:9200
```

返回内容类似：

```json
{
  "name": "...",
  "cluster_name": "docker-cluster",
  "cluster_uuid": "...",
  "version": {
    "number": "7.17.15",
    ...
  },
  "tagline": "You Know, for Search"
}
```

### 查看索引

```powershell
curl http://localhost:9200/_cat/indices?v
```

### 查看集群健康

```powershell
curl http://localhost:9200/_cluster/health
```

---

## 4. 安装可视化工具（可选）

推荐 **Elasticvue**，一个 Chrome 插件，打开即用。也可以用 browser 的方式安装：

或者安装 **Kibana**（ES 官方可视化工具）：

```powershell
docker run -d `
  --name dev-kibana `
  --net host `
  -e "ELASTICSEARCH_HOSTS=http://localhost:9200" `
  kibana:7.17.15
```

> `--net host` 让 Kibana 容器直接访问宿主机的 `localhost:9200`。

访问 `http://localhost:5601` 进入 Kibana。

---

## 5. 常用管理命令

```powershell
# 查看日志
docker logs dev-elasticsearch

# 查看 ES 日志（关注启动报错）
docker logs dev-elasticsearch 2>&1 | Select-String "error"

# 进入容器
docker exec -it dev-elasticsearch bash

# 重启
docker restart dev-elasticsearch

# 彻底删除
docker rm -f dev-elasticsearch
docker volume rm es_data
```

---

## 6. 连接配置对照

`application-dev.yml` 中的对应配置：

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
```

Spring Boot 3 使用 `spring.elasticsearch.uris`（注意是 `uris` 不是 `uris` 的旧写法 `rest.uris`）。

---

## 7. 常见问题

### 容器启动后立刻退出？

ES 对内存有最低要求。检查日志：

```powershell
docker logs dev-elasticsearch
```

常见原因：
- **内存不足**：WSL 2 默认吃一半系统内存，但 ES 需要连续内存块。把 `ES_JAVA_OPTS` 的 `-Xmx` 降到 `256m` 试试，或者在 `.wslconfig` 限制 WSL 内存。
- **vm.max_map_count 太低**：按第 2 步的 wsl 命令修复。

### 访问 9200 返回空？

确认端口映射正确：

```powershell
docker port dev-elasticsearch
```

输出应显示 `9200/tcp -> 0.0.0.0:9200`。

### Spring Boot 启动报 "Connection refused: localhost/127.0.0.1:9200"？

- ES 容器没启动 → `docker start dev-elasticsearch`
- ES 在启动中（日志有 "started" 才就绪，通常要等 10-30 秒）→ 等一会再启动 Spring Boot
- `application-dev.yml` 里的 `uris` 写成了远程地址 → 改成 `http://localhost:9200`

### 生产环境注意事项

本项目开发环境关闭了安全认证。生产环境务必：
1. 启用 `xpack.security.enabled=true`
2. 设置密码：`elasticsearch-setup-passwords`
3. 配置 HTTPS
4. `application-dev.yml` 改成 `application-prod.yml` 并在 URL 里带认证信息
