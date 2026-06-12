FROM docker.elastic.co/elasticsearch/elasticsearch:8.11.0

# 从本地 zip 安装 IK 中文分词插件
COPY elasticsearch-analysis-ik-8.11.0.zip /tmp/ik.zip
RUN elasticsearch-plugin install --batch file:///tmp/ik.zip
