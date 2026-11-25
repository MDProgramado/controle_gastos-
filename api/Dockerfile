FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
# Testes unitários + spotless no build; surefire exclui *IntegrationTest/*MigrationTest (sem Docker)
RUN mvn -B clean verify

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY --from=build /app/src/main/resources/rag/corpus_financeiro_RAG_chunks.jsonl /app/rag/corpus_financeiro_RAG_chunks.jsonl
# A Render injeta a env var PORT (10000 por padrão) — application.yml já usa ${PORT:8080}
ENV AI_RAG_CORPUS_PATH=/app/rag/corpus_financeiro_RAG_chunks.jsonl
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
