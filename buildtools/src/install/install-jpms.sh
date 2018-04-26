#!/usr/bin/env bash
./gradlew --version
./gradlew --stacktrace --warning-mode=all trellis-vocabulary:build
./gradlew --stacktrace --warning-mode=all trellis-api:build
./gradlew --stacktrace --warning-mode=all trellis-id:build
./gradlew --stacktrace --warning-mode=all trellis-io:build
./gradlew --stacktrace --warning-mode=all trellis-namespaces:build
./gradlew --stacktrace --warning-mode=all trellis-agent:build
./gradlew --stacktrace --warning-mode=all trellis-http:build
./gradlew --stacktrace --warning-mode=all trellis-file:build
./gradlew --stacktrace --warning-mode=all trellis-amqp:build
./gradlew --stacktrace --warning-mode=all trellis-jms:build
./gradlew --stacktrace --warning-mode=all trellis-kafka:build
./gradlew --stacktrace --warning-mode=all trellis-test:build
./gradlew --stacktrace --warning-mode=all trellis-triplestore:build
./gradlew --stacktrace --warning-mode=all trellis-webac:build
./gradlew --stacktrace --warning-mode=all trellis-audit:build
./gradlew --stacktrace --warning-mode=all trellis-app:build
./gradlew --stacktrace --warning-mode=all trellis-app-triplestore:build
