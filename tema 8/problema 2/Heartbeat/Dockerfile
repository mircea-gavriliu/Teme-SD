FROM openjdk:8-jdk-alpine
ADD target/Heartbeat-1.0-SNAPSHOT-jar-with-dependencies.jar Heartbeat.jar

ENTRYPOINT ["java","-jar", "Heartbeat.jar"]