# 兼容 amd、arm 构建镜像
docker buildx build --load --platform linux/amd64,linux/arm64 -t hbulgat/ai-travel-planner-app:1.1 -f ./Dockerfile .
