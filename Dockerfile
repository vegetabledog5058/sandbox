# 在基础镜像的基础上继续构建
FROM openjdk:8-jdk-alpine

# 创建一个新的用户`appuser`用于运行应用，避免使用root用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
# 创建 tmpCode 目录并赋予 appuser 相关的权限
RUN mkdir -p /app/tmpCode && chown appuser:appgroup /app/tmpCode

# （可选）设置工作目录
WORKDIR /app

# 将你的jar包添加到镜像中（请确保路径和文件名正确）
COPY ./siyi-code-sandbox-1.0-SNAPSHOT.jar /app/siyi-code-sandbox-1.0-SNAPSHOT.jar

# 切换到非root用户
USER appuser

# 暴露端口
EXPOSE 8106

# 启动命令
ENTRYPOINT ["java", \
    "-XX:+UseG1GC", \
    "-Xms256m", \
    "-Xmx512m", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "/app/siyi-code-sandbox-1.0-SNAPSHOT.jar", \
    "--spring.profiles.active=prod"]
