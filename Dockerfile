FROM  java:8
VOLUME /tmp
ADD  target/web-gateway-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT [ "sh", "-c", "java -Dspring.profiles.active=dev -jar app.jar"]
