FROM gradle:9.6.0-jdk21-alpine AS build

WORKDIR /workspace
COPY --chown=gradle:gradle . .
RUN chown gradle:gradle /workspace
USER gradle
RUN ./gradlew --no-daemon clean build installDist

FROM eclipse-temurin:21-jre-alpine

RUN addgroup --system tagalog && adduser --system --ingroup tagalog tagalog
WORKDIR /app
COPY --from=build --chown=tagalog:tagalog /workspace/build/install/tagalog/ ./
RUN mkdir /app/data && chown tagalog:tagalog /app/data
USER tagalog

ENTRYPOINT ["/app/bin/tagalog"]
CMD ["init"]
