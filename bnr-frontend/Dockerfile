# --- Stage 1: build the React app ---
FROM node:20-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm ci --silent

COPY . .

# VITE_API_URL is baked in at build time; docker-compose passes it via build-arg
ARG VITE_API_URL=http://localhost:8080
ENV VITE_API_URL=$VITE_API_URL

RUN npm run build

# --- Stage 2: serve with nginx ---
FROM nginx:alpine

LABEL maintainer="David NTAMAKEMWA"

# Replace default nginx config with one that handles React Router (HTML5 history API)
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
