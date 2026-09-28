# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Install maven (se non si usa il wrapper)
RUN apk add --no-cache maven

# Copy root pom and module poms for dependency caching
COPY pom.xml .
COPY bar-common/pom.xml bar-common/
COPY bar-dispatcher/pom.xml bar-dispatcher/
COPY bar-counter/pom.xml bar-counter/
COPY bar-kitchen/pom.xml bar-kitchen/
COPY bar-desk/pom.xml bar-desk/

# Download dependencies
RUN mvn dependency:go-offline -B -pl bar-dispatcher -am

# Copy source code and build
COPY bar-common/src bar-common/src
COPY bar-dispatcher/src bar-dispatcher/src
RUN mvn clean package -pl bar-dispatcher -am -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root system user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /workspace/bar-dispatcher/target/bar-dispatcher-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:+ZGenerational", "-jar", "app.jar"]
