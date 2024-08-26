FROM openjdk:17
ARG JAR_FILE=target/*.jar
COPY ./target/AUTPROCJUD-0.0.1.jar app.jar
ENTRYPOINT [ "java", "-jar","app.jar" ]