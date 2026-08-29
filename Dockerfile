FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -q
COPY src ./src
RUN ./mvnw clean package -DskipTests -q

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN curl -Lo /dd-java-agent.jar https://dtdg.co/latest-java-tracer
COPY --from=build /app/target/oficina-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", \
  "-javaagent:/dd-java-agent.jar", \
  "-Ddd.service=oficina-app", \
  "-Ddd.env=${DD_ENV:-prod}", \
  "-Ddd.version=${APP_VERSION:-latest}", \
  "-Ddd.logs.injection=true", \
  "-Ddd.profiling.enabled=true", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
