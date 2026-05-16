FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY api-gateway/target/api-gateway-*.jar api-gateway.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV CORBA_HOST=corba-server
ENV CORBA_PORT=2809

ENTRYPOINT ["sh", "-c", \
    "java $JAVA_OPTS \
     -jar api-gateway.jar \
     --corba.host=$CORBA_HOST \
     --corba.port=$CORBA_PORT"]
