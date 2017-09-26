FROM openjdk:8-jdk-alpine

ADD build/libs/iow-termed-api.war iow-termed-api.war

ENV JAVA_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /iow-termed-api.war" ]
