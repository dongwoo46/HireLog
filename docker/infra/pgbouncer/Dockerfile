FROM alpine:3.19

# PgBouncer 및 필요 패키지 설치
RUN apk add --no-cache \
    pgbouncer \
    postgresql-client \
    su-exec \
    && mkdir -p /etc/pgbouncer

# 우리의 초기화 스크립트 복사
COPY entrypoint.sh /custom-entrypoint.sh
RUN chmod +x /custom-entrypoint.sh

EXPOSE 5432

# sh를 명시적으로 사용 + 다른 이름 사용
ENTRYPOINT ["sh", "/custom-entrypoint.sh"]