FROM eclipse-temurin:24-jdk-alpine AS builder
WORKDIR /build
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src src
RUN ./mvnw clean package -DskipTests -B

FROM eclipse-temurin:24-jre-alpine AS runtime
RUN addgroup -g 1000 appgroup && adduser -u 1000 -G appgroup -D -h /app appuser
WORKDIR /app
RUN mkdir -p /app/tmp /app/cache && chown -R 1000:1000 /app
COPY --from=builder --chown=1000:1000 /build/target/*.jar /app/app.jar
USER 1000:1000
EXPOSE 8081
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8081/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.io.tmpdir=/app/tmp", "-jar", "app.jar"]
