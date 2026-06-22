# ════════════════════════════════════════════════════════════════
#  API Gateway — image mono-service (Spring Boot, JVM unique, Java 21)
#  Un seul process java -jar (monolithe modulaire, sans IIOP).
#  L'OCR (Tesseract) et le rendu PDF tournent DANS cette JVM.
# ════════════════════════════════════════════════════════════════

# ── Stage 1 : build du reactor → fat jar api-gateway ────────────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml ./
COPY pdf-engine ./pdf-engine
COPY api-gateway ./api-gateway
# settings.xml : redirige Maven Central vers le miroir Google Cloud Storage
# pour éviter les .pom corrompus (page HTML Cloudflare) servis à certaines IP.
COPY maven-settings.xml /root/.m2/settings.xml

RUN mvn -B -q -s /root/.m2/settings.xml \
        -Dmaven.wagon.http.retryHandler.count=3 \
        clean package -DskipTests

# ── Stage 2 : runtime léger ─────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

# Tesseract (OCR) + données de langue fra/eng + polices (rendu PDF).
# Sur Alpine, les paquets tesseract-ocr-data-* installent dans
# /usr/share/tessdata/ — chemin par défaut lu par OcrHandler.
RUN apk add --no-cache \
      tesseract-ocr \
      tesseract-ocr-data-fra \
      tesseract-ocr-data-eng \
      fontconfig \
      ttf-dejavu

WORKDIR /app

COPY --from=build /build/api-gateway/target/api-gateway-*.jar api-gateway.jar

EXPOSE 8080

# JVM unique (remplace les deux anciennes) — free tier Render = 512 Mo.
ENV JAVA_OPTS="-Xms256m -Xmx420m -XX:+UseSerialGC" \
    TESSDATA_PATH=/usr/share/tessdata

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.awt.headless=true -jar api-gateway.jar"]
