# ============================================================
# Stage: deps -- shared dependency resolution
# ============================================================
FROM eclipse-temurin:21-jdk-noble AS deps

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    ./mvnw dependency:resolve -P no-checks -B

COPY src/ src/
COPY frontend/ frontend/

# ============================================================
# Target: jvm -- standard JVM build (develop branch)
# Fast build (~30s), larger image, higher memory usage
# ============================================================
FROM deps AS jvm-builder

ARG VITE_STRIPE_PUBLISHABLE_KEY
ENV VITE_STRIPE_PUBLISHABLE_KEY=${VITE_STRIPE_PUBLISHABLE_KEY}

RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    ./mvnw package -P no-checks,frontend -B -DskipTests

FROM eclipse-temurin:21-jre-noble AS jvm

RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app
COPY --from=jvm-builder /app/target/azadi-*.jar app.jar

RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

# ============================================================
# Target: native -- GraalVM native image (main branch)
# Slow build (~15min), tiny image, instant startup, low memory
# ============================================================
FROM ghcr.io/graalvm/native-image-community:21 AS native-builder

ARG VITE_STRIPE_PUBLISHABLE_KEY
ENV VITE_STRIPE_PUBLISHABLE_KEY=${VITE_STRIPE_PUBLISHABLE_KEY}

RUN microdnf install -y findutils

ENV LC_ALL=C.UTF-8 LANG=C.UTF-8

WORKDIR /app

COPY --from=deps /app/.mvn/ .mvn/
COPY --from=deps /app/mvnw /app/pom.xml ./
COPY --from=deps /app/src/ src/
COPY --from=deps /app/frontend/ frontend/

RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    --mount=type=cache,target=/app/target/native-image-cache,sharing=locked \
    ./mvnw -Pnative native:compile -P no-checks,frontend -B -DskipTests \
    -Dnative.build.args="-H:+StaticExecutableWithDynamicLibC -H:+CacheAnalysisResults -H:CacheDir=/app/target/native-image-cache"

FROM alpine:3.21 AS native

RUN apk add --no-cache libc6-compat

RUN addgroup -S appuser && adduser -S appuser -G appuser

WORKDIR /app
COPY --from=native-builder /app/target/azadi .

RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 8080

ENTRYPOINT ["./azadi"]
