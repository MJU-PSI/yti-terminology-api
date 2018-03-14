FROM openjdk:8-jdk-alpine

ADD build/libs/yti-terminology-api.jar yti-terminology-api.jar

ADD *.properties ./

ENV JAVA_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /yti-terminology-api.jar" ]
