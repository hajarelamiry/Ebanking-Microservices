FROM node:22-alpine

WORKDIR /app

COPY package*.json ./

RUN npm install --omit=dev

COPY . .

EXPOSE 4000

ENV AUTH_SERVICE_URL=http://auth-service:8081
ENV USER_SERVICE_URL=http://user-service:8082

CMD ["node", "index.js"]
