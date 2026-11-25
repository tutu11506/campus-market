# 运行阶段，直接 COPY 本地已打好的 jar（避开 BuildKit 文件系统同步坑）
FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms256m -Xmx512m"

# 健康检查探活使用 curl（基础镜像未自带），同时预建日志目录
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /app/logs

COPY target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
