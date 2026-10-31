FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY HospitalServer.java .
COPY web ./web

RUN javac HospitalServer.java

EXPOSE 8080

CMD ["java", "HospitalServer"]
