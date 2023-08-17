# Dependency images
FROM yti-spring-security:latest as yti-spring-security
FROM yti-spring-migration:latest as yti-spring-migration

# Builder image
FROM gradle:6.9-jdk11 as builder

# Copy yti-spring-migration dependency from MAVEN repo
COPY --from=yti-spring-migration /root/.m2/repository/fi/vm/yti/ /root/.m2/repository/fi/vm/yti/

# Copy yti-spring-security dependency from MAVEN repo
COPY --from=yti-spring-security /root/.m2/repository/fi/vm/yti/ /root/.m2/repository/fi/vm/yti/

# Set working dir
WORKDIR /app

# Copy source file
COPY src src
COPY build.gradle .
COPY settings.gradle .

# Build project
RUN gradle build -x test --no-daemon

# Pull base image
FROM yti-docker-java11-base:alpine

# Copy from builder 
COPY --from=builder /app/build/libs/yti-terminology-api.jar ${deploy_dir}/yti-terminology-api.jar

# Set default command on run
ENTRYPOINT ["/bootstrap.sh", "yti-terminology-api.jar", "-j", "-Djava.security.egd=file:/dev/./urandom"]
