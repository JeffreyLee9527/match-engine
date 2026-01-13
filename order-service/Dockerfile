# ============================================
# Builder Stage - 构建阶段
# ============================================
# 注意：构建上下文应为项目根目录（在 docker-compose.yml 中配置）
FROM maven:3.9-eclipse-temurin-21 AS builder

LABEL stage="builder"
LABEL service.name="order-service"

# 设置工作目录
WORKDIR /build

# 第一步：只复制POM文件（利用Docker层缓存）
# 只要POM文件不变，这一层就会被缓存，依赖不会重新下载
COPY pom.xml pom.xml.orig
COPY common/pom.xml common/pom.xml
COPY order-service/pom.xml order-service/pom.xml

# 修改父 pom，只保留当前需要的模块
RUN awk '/<modules>/{in_modules=1; print; print "    <module>common</module>"; print "    <module>order-service</module>"; next} /<\/modules>/{if(in_modules){in_modules=0; print "  </modules>"; next}} !in_modules' pom.xml.orig > pom.xml

# 第二步：下载外部依赖到本地仓库（这一层会被缓存，只要POM不变就不会重新执行）
# 注意：这里只下载外部依赖，不包括项目内部的common模块
# common模块会在构建时安装，所以这里跳过对common的依赖解析
# 使用 dependency:resolve 而不是 go-offline，因为 go-offline 会下载所有插件，很慢
# 添加超时和重试参数，避免网络问题导致卡住
RUN mvn dependency:resolve -pl common -nsu -B -q -Dmaven.wagon.http.retryHandler.count=3 -Dmaven.wagon.httpconnectionManager.ttlSeconds=30 || \
    echo "依赖解析失败，将在构建时下载"

# 第三步：复制源码（源码变化不会影响依赖下载层）
COPY common/src common/src
COPY order-service/src order-service/src

# 第四步：构建（先安装common，再构建order-service）
# Docker层缓存会保存依赖下载层，只要POM不变就不会重新下载
# 构建时会先安装common模块到本地仓库，然后order-service才能使用
RUN mvn clean install -pl common -DskipTests -nsu -B && \
    mvn clean package -pl order-service -am -DskipTests -nsu -B

# ============================================
# Runtime Stage - 运行阶段
# ============================================
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="spark-match-engine"
LABEL service.name="order-service"

# 设置时区
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata wget && \
    cp /usr/share/zoneinfo/${TZ} /etc/localtime && \
    echo "${TZ}" > /etc/timezone && \
    rm -rf /var/cache/apk/*

# 创建非 root 用户
RUN addgroup -S appgroup && \
    adduser -S appuser -G appgroup

# 设置工作目录
WORKDIR /app

# 创建日志目录
RUN mkdir -p /logs && \
    chown -R appuser:appgroup /app /logs

# 从构建阶段复制 jar 包
COPY --from=builder --chown=appuser:appgroup /build/order-service/target/*.jar /app/app.jar

# 设置 JVM 参数（可通过环境变量覆盖）
ENV JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/logs/heapdump.hprof -Djava.security.egd=file:/dev/./urandom"

# 暴露端口
EXPOSE 8081

# 切换到非 root 用户
USER appuser

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
