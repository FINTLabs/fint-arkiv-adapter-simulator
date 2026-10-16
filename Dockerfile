FROM gcr.io/distroless/java25:nonroot
ENV TZ="Europe/Oslo"
ENV JAVA_TOOL_OPTIONS="-XX:+ExitOnOutOfMemoryError --sun-misc-unsafe-memory-access=allow"
WORKDIR /app
COPY build/libs/*.jar ./app.jar
EXPOSE 8080 9090
CMD ["app.jar"]
