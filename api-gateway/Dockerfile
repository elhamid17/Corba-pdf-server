# ═══════════════════════════════════════════
# Stage 1 — Build Maven
# ═══════════════════════════════════════════
FROM maven:3.8.7-eclipse-temurin-11 AS builder

WORKDIR /build

# Copier les pom.xml (cache dépendances)
COPY pom.xml .
COPY corba-idl/pom.xml    corba-idl/
COPY api-gateway/pom.xml  api-gateway/

RUN mvn dependency:go-offline -pl corba-idl,api-gateway -am -q

# Copier les sources
COPY corba-idl/src    corba-idl/src
COPY api-gateway/src  api-gateway/src

RUN mvn package -pl corba-idl,api-gateway -am -DskipTests -q

# ═══════════════════════════════════════════
# Stage 2 — Runtime Spring Boot
# ═══════════════════════════════════════════
FROM eclipse-temurin:11-jre-alpine

WORKDIR /app

COPY --from=builder \
    /build/api-gateway/target/api-gateway-*.jar \
    api-gateway.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV CORBA_HOST=corba-server
ENV CORBA_PORT=2809

ENTRYPOINT ["sh", "-c", \
    "java $JAVA_OPTS \
     -jar api-gateway.jar \
     --corba.host=$CORBA_HOST \
     --corba.port=$CORBA_PORT"]