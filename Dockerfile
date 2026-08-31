FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -q
COPY src ./src
RUN ./mvnw clean package -DskipTests -q

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update -q && apt-get install -y -q curl unzip \
    && curl -sSL https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-java.zip -o newrelic-java.zip \
    && unzip -q newrelic-java.zip \
    && rm newrelic-java.zip \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/oficina-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-javaagent:/app/newrelic/newrelic.jar", \
  "-jar", "app.jar"]
