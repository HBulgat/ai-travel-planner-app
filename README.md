# Web AI 旅行师
## Quick Start
1. 修改配置
修改docs/dev-ops/下的docker-compose-app.yml文件，将DASHSCOPE_API_KEY修改为你的阿里云百炼平台的api-key
2. 运行
在项目目录下运行下面命令
```bash
chmod +x deploy.sh
./deploy.sh
```

3. 运行效果
打开浏览器输入localhost即可进入页面

### 备注
如果镜像拉取太慢，可以下载github中的tar包：
https://github.com/HBulgat/ai-travel-planner-app/releases/tag/release-v1.0.0
然后使用`docker load -i <镜像名称>.tar`命令加载镜像