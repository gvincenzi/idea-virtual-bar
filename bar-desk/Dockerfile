# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

RUN apk add --no-cache maven

COPY pom.xml .
COPY bar-common/pom.xml bar-common/
COPY bar-dispatcher/pom.xml bar-dispatcher/
COPY bar-counter/pom.xml bar-counter/
COPY bar-kitchen/pom.xml bar-kitchen/
COPY bar-desk/pom.xml bar-desk/

RUN mvn dependency:go-offline -B -pl bar-desk -am

COPY bar-common/src bar-common/src
COPY bar-desk/src bar-desk/src
RUN mvn clean package -pl bar-desk -am -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /workspace/bar-desk/target/bar-desk-*.jar app.jar

ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:+ZGenerational", "-jar", "app.jar"]
