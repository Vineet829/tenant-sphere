FROM docker.io/node:22-alpine as base

FROM base as node-build-stage

RUN apk add --no-cache libc6-compat

COPY package.json yarn.lock* package-lock.json* pnpm-lock.yaml* ./


RUN \
  if [ -f yarn.lock ]; then yarn --frozen-lockfile; \
  elif [ -f package-lock.json ]; then npm ci; \
  elif [ -f pnpm-lock.yaml ]; then corepack enable pnpm && pnpm i; \
  else echo "Lockfile not found." && exit 1; \
  fi

FROM base as node-run-stage

ARG APP_HOME=/app
WORKDIR ${APP_HOME}

# su-exec lets the start script drop from root to the checkout's owner.
RUN apk add --no-cache su-exec

COPY --from=node-build-stage /node_modules ./node_modules

COPY . ${APP_HOME}

COPY ./docker/local/start /usr/local/bin/start
RUN chmod +x /usr/local/bin/start

CMD [ "start" ]


