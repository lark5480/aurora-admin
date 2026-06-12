# Docker 安装 Elasticsearch 及配置

> **推荐直接使用项目根目录的 `docker-compose.yml` 一键启动所有中间件。**
> 本文档仅在你需要单独安装 Elasticsearch 时参考。

---

## 1. 拉取镜像

项目使用自定义镜像（含 IK 中文分词插件），通过 `docker compose up elasticsearch -d` 自动构建。
如需单独拉取原始镜像：

```powershell
docker pull docker.elastic.co/elasticsearch/elasticsearch:8.11.0
```

> Spring Boot 3.2 搭配 ES 8.x 使用。项目选用 8.11.0 以匹配 IK 分词插件版本。

---

## 2. 启动容器

```powershell
docker run -d `
  --name aurora-es `
  -p 9200:9200 `
  -e "discovery.type=single-node" `
  -e "xpack.security.enabled=false" `
  -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" `
  -v es_data:/usr/share/elasticsearch/data `
  --restart unless-stopped `
  docker.elastic.co/elasticsearch/elasticsearch:8.11.0
```

| 参数 | 说明 |
|------|------|
| `discovery.type=single-node` | 单节点模式，跳过集群发现 |
| `xpack.security.enabled=false` | 关闭安全认证，本地开发省事 |
| `ES_JAVA_OPTS=-Xms512m -Xmx512m` | 限制 JVM 堆内存（开发机内存有限，别给太高） |
| `-v es_data:/usr/share/elasticsearch/data` | 数据持久化 |

> **Windows 注意**：ES 需要 `vm.max_map_count` 至少 262144。Docker Desktop + WSL 2 环境通常已满足。如果启动报 `max virtual memory areas vm.max_map_count [65530] is too low`，执行：

```powershell
wsl -d docker-desktop sysctl -w vm.max_map_count=262144
```

---

## 3. IK 中文分词插件

项目搜索功能使用了 `ik_smart`（粗粒度）和 `ik_max_word`（细粒度）分词器，需要安装 IK 插件。

项目已通过自定义 Dockerfile 自动安装，构建上下文在 `docker/elasticsearch/`：

```
docker/elasticsearch/
├── Dockerfile                              # 基于 ES 8.11.0，COPY + install IK zip
└── elasticsearch-analysis-ik-8.11.0.zip    # IK 插件包（版本必须与 ES 一致）
```

首次 `docker compose up elasticsearch` 时会自动构建镜像 `aurora-es:8.11.0-ik`。

验证插件是否安装成功：

```powershell
docker exec aurora-es elasticsearch-plugin list
# 应输出: analysis-ik
```

测试分词效果：

```powershell
curl -X POST "http://localhost:9200/_analyze?pretty" -H "Content-Type: application/json" -d "{\"analyzer\":\"ik_smart\",\"text\":\" Aurora 管理系统\"}"
```

---

## 4. 验证

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
    "number": "8.11.0",
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

## 5. 安装可视化工具（可选）

推荐 **Elasticvue**，一个 Chrome 插件，打开即用。也可以安装 **Kibana**（ES 官方可视化工具）：

```powershell
docker run -d `
  --name aurora-kibana `
  --net host `
  -e "ELASTICSEARCH_HOSTS=http://localhost:9200" `
  docker.elastic.co/kibana/kibana:8.11.0
```

> `--net host` 让 Kibana 容器直接访问宿主机的 `localhost:9200`。

访问 `http://localhost:5601` 进入 Kibana。

---

## 6. 常用管理命令

```powershell
# 查看日志
docker logs aurora-es

# 查看 ES 日志（关注启动报错）
docker logs aurora-es 2>&1 | Select-String "error"

# 进入容器
docker exec -it aurora-es bash

# 重启
docker restart aurora-es

# 彻底删除
docker rm -f aurora-es
docker volume rm es_data
```

---

## 7. 连接配置对照

`application-dev.yml` 中的对应配置：

```yaml
spring:
  elasticsearch:
    uris: ${ELASTICSEARCH_URIS:http://localhost:9200}
```

Spring Boot 3 使用 `spring.elasticsearch.uris`（注意不是旧版 `rest.uris`）。

---

## 8. 常见问题

### 容器启动后立刻退出？

ES 对内存有最低要求。检查日志：

```powershell
docker logs aurora-es
```

常见原因：
- **内存不足**：WSL 2 默认吃一半系统内存，但 ES 需要连续内存块。把 `ES_JAVA_OPTS` 的 `-Xmx` 降到 `256m` 试试，或者在 `.wslconfig` 限制 WSL 内存。
- **vm.max_map_count 太低**：按第 2 步的 wsl 命令修复。

### 访问 9200 返回空？

确认端口映射正确：

```powershell
docker port aurora-es
```

输出应显示 `9200/tcp -> 0.0.0.0:9200`。

### Spring Boot 启动报 "Connection refused: localhost/127.0.0.1:9200"？

- ES 容器没启动 → `docker start aurora-es`
- ES 在启动中（日志有 "started" 才就绪，通常要等 10-30 秒）→ 等一会再启动 Spring Boot
- `application-dev.yml` 里的 `uris` 写成了远程地址 → 改成 `http://localhost:9200`

### 生产环境注意事项

本项目开发环境关闭了安全认证。生产环境务必：
1. 启用 `xpack.security.enabled=true`
2. 设置密码：`elasticsearch-setup-passwords`
3. 配置 HTTPS
4. `application-dev.yml` 改成 `application-prod.yml` 并在 URL 里带认证信息
