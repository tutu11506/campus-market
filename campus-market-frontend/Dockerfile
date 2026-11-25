# 构建阶段：Vite 打包出静态文件
FROM node:22-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm config set registry https://registry.npmmirror.com && npm ci

COPY . .
RUN npm run build

# 运行阶段：Nginx 托管静态文件，/api 反代到后端
FROM nginx:1.27-alpine
ENV TZ=Asia/Shanghai
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
