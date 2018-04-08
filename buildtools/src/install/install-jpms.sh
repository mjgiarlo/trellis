#!/usr/bin/env bash
./gradlew --version
./gradlew --scan --stacktrace --warning-mode=all trellis-vocabulary:build
./gradlew --scan --stacktrace --warning-mode=all trellis-api:build
./gradlew --scan --stacktrace --warning-mode=all trellis-id:build
./gradlew --scan --stacktrace --warning-mode=all trellis-io:build
./gradlew --scan --stacktrace --warning-mode=all trellis-namespaces:build
./gradlew --scan --stacktrace --warning-mode=all trellis-http:build
./gradlew --scan --stacktrace --warning-mode=all trellis-file:build
./gradlew --scan --stacktrace --warning-mode=all trellis-amqp:build
./gradlew --scan --stacktrace --warning-mode=all trellis-jms:build
./gradlew --scan --stacktrace --warning-mode=all trellis-kafka:build
./gradlew --scan --stacktrace --warning-mode=all trellis-test:build
./gradlew --scan --stacktrace --warning-mode=all trellis-triplestore:build
./gradlew --scan --stacktrace --warning-mode=all trellis-webac:build
./gradlew --scan --stacktrace --warning-mode=all trellis-agent:build
./gradlew --scan --stacktrace --warning-mode=all trellis-audit:build
./gradlew --scan --stacktrace --warning-mode=all trellis-app:build
./gradlew --scan --stacktrace --warning-mode=all trellis-osgi:build