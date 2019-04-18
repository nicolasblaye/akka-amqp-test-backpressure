FROM openjdk:8u201-jdk-alpine3.9

RUN apk add --no-cache bash

ADD akka-amqp.tgz .
WORKDIR akka-amqp-example-0.1

CMD ["./bin/producer"]