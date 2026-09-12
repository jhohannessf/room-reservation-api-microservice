# Etapa 1: build da aplicação com Maven
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copia só o pom.xml primeiro para aproveitar cache de dependências
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Agora copia o código-fonte e empacota o .jar (sem rodar os testes no build da imagem)
COPY src ./src
RUN mvn clean package -DskipTests -B

# Etapa 2: imagem final, só com o JRE (bem mais leve que a imagem de build)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
