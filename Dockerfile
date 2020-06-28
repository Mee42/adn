FROM openjdk:11-jdk-slim AS build-env

ADD . /adn/code/
WORKDIR /adn/
RUN cd code && ./gradlew
RUN cd code && ./gradlew shadowJar && mv build/libs/adn-v*all.jar ../adn.jar

FROM gcr.io/distroless/java:11
COPY --from=build-env /adn /adn
WORKDIR /adn/
ENTRYPOINT ["java","-jar","adn.jar"]
EXPOSE 5001
CMD ["--run-server","--port","5001"]
