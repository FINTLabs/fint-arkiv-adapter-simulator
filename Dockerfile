FROM gcr.io/distroless/java25:nonroot@sha256:8ac5525549d14b8ddd34562f9fc8e6ba4840c995363a1507b17cadf8bb7ab57e
ENV TZ="Europe/Oslo"
ENV JAVA_TOOL_OPTIONS="-XX:+ExitOnOutOfMemoryError --sun-misc-unsafe-memory-access=allow"
WORKDIR /app
COPY build/libs/*.jar ./app.jar
EXPOSE 8080 9090
CMD ["app.jar"]
