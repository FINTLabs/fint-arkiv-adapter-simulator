FROM gcr.io/distroless/java25:nonroot@sha256:5d3177252775d4b01603470fe21c015393019089904f68724bd37242e4bc6e6d
ENV TZ="Europe/Oslo"
ENV JAVA_TOOL_OPTIONS="-XX:+ExitOnOutOfMemoryError --sun-misc-unsafe-memory-access=allow"
WORKDIR /app
COPY build/libs/*.jar ./app.jar
EXPOSE 8080 9090
CMD ["app.jar"]
