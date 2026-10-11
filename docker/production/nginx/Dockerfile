FROM nginx:1.30-alpine

RUN rm /etc/nginx/conf.d/default.conf

# On start, the image writes /etc/nginx/nginx.conf from the template with the site's host name in it.
ENV NGINX_ENVSUBST_OUTPUT_DIR=/etc/nginx \
    NGINX_ENVSUBST_FILTER=SERVER_NAME

COPY ./nginx.conf.template /etc/nginx/templates/nginx.conf.template
