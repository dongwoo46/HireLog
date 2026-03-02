# Build stage
FROM node:20-alpine AS build

WORKDIR /app

# Accept build-time environment variables
ARG VITE_API_BASE_URL
ARG VITE_MODE=production
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_MODE=$VITE_MODE

COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build -- --mode $VITE_MODE

# Production stage
FROM node:20-alpine

RUN npm install -g serve

COPY --from=build /app/dist /app/dist

EXPOSE 5173
CMD ["serve", "-s", "/app/dist", "-l", "5173"]