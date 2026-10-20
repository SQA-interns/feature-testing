# Build stage
FROM node:24.13.0-alpine AS build
WORKDIR /app
COPY package.json package-lock.json .npmrc ./
RUN npm ci --no-fund --no-audit
COPY . .
RUN npm run build

# Runtime stage: static files served by nginx as a non-root user (SB-11)
FROM nginx:1.30.5-alpine
COPY nginx.conf /etc/nginx/nginx.conf
COPY --from=build /app/dist /usr/share/nginx/html
USER nginx
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --retries=10 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/ || exit 1
