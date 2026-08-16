FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn -q -DskipTests package

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
ENV TZ=UTC
# Default: small box (~2 GB shared with Postgres/Grafana). Serial uses the least native RAM.
ENV JAVA_TOOL_OPTIONS="-Xms64m -Xmx256m -XX:+UseSerialGC"
# Bigger box (this JVM can take ≥1 GB). G1 is Java 25's default; swap to -XX:+UseZGC if you want shorter pauses.
# ENV JAVA_TOOL_OPTIONS="-Xms256m -Xmx1g -XX:+UseG1GC"
RUN apk add --no-cache wget
COPY --from=build /app/target/teslamate-query-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
