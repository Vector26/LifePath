# Build stage
FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# Run stage - nginx on port 39173, proxy /api to backend
FROM nginx:alpine
RUN apk add --no-cache wget
RUN rm -rf /usr/share/nginx/html/*
COPY --from=build /app/dist /usr/share/nginx/html
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 39173
CMD ["nginx", "-g", "daemon off;"]
