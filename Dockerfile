# ==========================================
# Etapa 1: Construcción (Builder)
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# 1. Optimización de caché de capas (Descargar dependencias primero)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
# Damos permisos de ejecución al wrapper de maven por si acaso
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline

# 2. Copiar el código fuente y compilar
COPY src src
RUN ./mvnw package -DskipTests

# ==========================================
# Etapa 2: Ejecución (Runner)
# ==========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 3. Usuario no-root para seguridad
RUN addgroup -S spring && adduser -S spring -G spring
# Cambiamos al usuario 'spring' antes de ejecutar la app
USER spring:spring

# 4. Copiar el JAR desde la etapa de construcción
# Cambiamos el propietario de los archivos al usuario spring
COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

# 5. Variables de entorno para producción (completando tu línea)
ENV JAVA_OPTS="-Xms256m -Xmx512m"

EXPOSE 8080

# Usamos sh -c para que evalúe la variable de entorno JAVA_OPTS correctamente
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS \
  -Dspring.datasource.url=jdbc:postgresql://${DB_HOST}/postgres \
  -Dspring.datasource.username=${DB_USER} \
  -Dspring.datasource.password=${DB_PASSWORD} \
  -jar app.jar"]