# ═══════════════════════════════════════════
# CORBA PDF Server — runtime image
# ═══════════════════════════════════════════
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Dépendances natives : Tesseract OCR + polices PDF
RUN apk add --no-cache \
    tesseract-ocr \
    tesseract-ocr-data-fra \
    tesseract-ocr-data-eng \
    fontconfig \
    ttf-dejavu \
    netcat-openbsd

COPY corba-server/target/corba-server-jar-with-dependencies.jar corba-server.jar

RUN mkdir -p /volumes/pdf-storage /volumes/tessdata

EXPOSE 2809

ENV JAVA_OPTS="-Xms256m -Xmx1024m"
ENV TESSDATA_PATH=/usr/share/tessdata

ENTRYPOINT ["sh", "-c", \
    "java $JAVA_OPTS \
     -Dtessdata.path=$TESSDATA_PATH \
     -Djava.awt.headless=true \
     -jar corba-server.jar"]
