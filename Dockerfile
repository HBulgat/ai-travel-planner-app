# 基础镜像
FROM openjdk:17-jdk-slim

# 作者
MAINTAINER bulgat

# 配置
ENV PARAMS=""

# 时区
ENV TZ=PRC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
RUN sed -i 's/deb.debian.org/mirrors.aliyun.com/g' /etc/apt/sources.list && \
    sed -i 's/security.debian.org/mirrors.aliyun.com/g' /etc/apt/sources.list && \
    apt update && \
    apt install -y ffmpeg && \
    apt clean && \
    rm -rf /var/lib/apt/lists/*
# 添加应用
ADD target/ai-travel-planner-app.jar /ai-travel-planner-app.jar

ENTRYPOINT ["sh","-c","java -jar $JAVA_OPTS /ai-travel-planner-app.jar $PARAMS"]
