# ═══════════════════════════════════════════
# Stage 1 — Build Maven
# ═══════════════════════════════════════════
FROM maven:3.8.7-eclipse-temurin-11 AS builder

WORKDIR /build

# Copier les pom.xml en premier (cache des dépendances)
COPY pom.xml .
COPY corba-idl/pom.xml    corba-idl/
COPY corba-server/pom.xml corba-server/

# Télécharger les dépendances (layer cachée si pom.xml inchangé)
RUN mvn dependency:go-offline -pl corba-idl,corba-server -am -q

# Copier les sources
COPY corba-idl/src    corba-idl/src
COPY corba-server/src corba-server/src

# Compiler et packager
RUN mvn package -pl corba-idl,corba-server -am -DskipTests -q

# ═══════════════════════════════════════════
# Stage 2 — Image runtime légère
# ═══════════════════════════════════════════
FROM eclipse-temurin:11-jre-alpine

WORKDIR /app

# Installer Tesseract OCR + modèles de langue
RUN apk add --no-cache \
    tesseract-ocr \
    tesseract-ocr-data-fra \
    tesseract-ocr-data-eng \
    fontconfig \
    ttf-dejavu

# Copier le JAR assemblé
COPY --from=builder \
    /build/corba-server/target/corba-server-jar-with-dependencies.jar \
    corba-server.jar

# Dossier pour les PDFs temporaires
RUN mkdir -p /volumes/pdf-storage /volumes/tessdata

# Port IIOP CORBA
EXPOSE 2809

# Variables d'environnement
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV tessdata.path=/usr/share/tessdata

ENTRYPOINT ["sh", "-c", \
    "java $JAVA_OPTS \
     -Dtessdata.path=$tessdata.path \
     -Djava.awt.headless=true \
     -jar corba-server.jar"]