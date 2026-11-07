FROM postgres:16

COPY config/certs/server.crt /etc/postgresql/certs/server.crt
COPY config/certs/server.key /etc/postgresql/certs/server.key

RUN chown postgres:postgres /etc/postgresql/certs/server.crt /etc/postgresql/certs/server.key \
    && chmod 600 /etc/postgresql/certs/server.key \
    && chmod 644 /etc/postgresql/certs/server.crt
