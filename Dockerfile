FROM azul/zulu-openjdk-alpine:17.0.10-jdk
WORKDIR /app

RUN apk update && apk add --no-cache curl

EXPOSE 8080
ENV LOG_DIR=/app/logs
ENV MICROSERVICE_NAME=uidam-userManagement

ARG PROJECT_JAR_NAME

ENV PROJECT_JAR_NAME ${PROJECT_JAR_NAME}

COPY target/${PROJECT_JAR_NAME}.jar /app/
COPY src/main/resources /app/
COPY docker-entrypoint.sh /app/

RUN mkdir -p /mnt/resources/localization/

# Run microservice as non root user
RUN chmod +x ./docker-entrypoint.sh && \
  mkdir /etc/emailTemplates/ && \
  addgroup -S msuser && \
  adduser -S msuser -G msuser && \
  chown -R msuser:msuser /app && \
  chown -R msuser:msuser /etc/emailTemplates

USER msuser

CMD ["/bin/sh", "./docker-entrypoint.sh"]
