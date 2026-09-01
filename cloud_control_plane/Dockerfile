# MISSION 17.2C.2: Portable JVM Control Plane
FROM gradle:8.4-jdk17 AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
COPY app ./app
COPY cloud_control_plane ./cloud_control_plane
RUN gradle :cloud_control_plane:installDist --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/cloud_control_plane/build/install/cloud_control_plane ./
EXPOSE 8080
CMD ["./bin/cloud_control_plane"]
