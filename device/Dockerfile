FROM maven:3.9.9

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline

CMD ["mvn", "spring-boot:run"]
