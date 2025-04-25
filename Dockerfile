# Dockerfile (in root directory 'credable-io')

# Stage 1: Build the application using Gradle
# Use a JDK version matching your project's toolchain (Java 21)
FROM gradle:8.7-jdk21-jammy AS builder

WORKDIR /app

# --- Copy necessary files for multi-module build with buildSrc ---
# Copy root build files and wrapper
COPY loan-management-system/build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle

# Copy the buildSrc directory (convention plugins)
COPY buildSrc ./buildSrc

# Copy the application subproject source code and build file
COPY loan-management-system ./loan-management-system
# --- End Copy ---

# Grant execution permission to the gradlew script
RUN chmod +x ./gradlew

# Build the application JAR for the specific subproject
# Use the subproject task path :<subproject-name>:<task-name>
# Clean task is good practice before building the final artifact
RUN gradle :loan-management-system:clean :loan-management-system:bootJar --no-daemon

# Stage 2: Create the final runtime image
# Use a JRE image matching the JDK version used for building
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Create a non-root user and group for security
RUN groupadd --system --gid 1001 appgroup && \
    useradd --system --uid 1001 --gid appgroup appuser

# Copy the executable JAR from the builder stage - adjust path to subproject output
COPY --from=builder /app/loan-management-system/build/libs/*.jar app.jar

# Set ownership to the non-root user
RUN chown appuser:appgroup app.jar

# Switch to the non-root user
USER appuser

# Expose the application port (ensure it matches application.yaml server.port)
EXPOSE 8080

# Set the entrypoint to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

