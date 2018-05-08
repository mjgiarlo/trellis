#!/usr/bin/env bash
./gradlew --version
./gradlew --stacktrace --warning-mode=all trellis-vocabulary-jpms:build
./gradlew --stacktrace --warning-mode=all trellis-api-jpms:build
./gradlew --stacktrace --warning-mode=all trellis-id-jpms:build
./gradlew --stacktrace --warning-mode=all trellis-io-jena-jpms:build
./gradlew --stacktrace --warning-mode=all trellis-namespaces-jpms:build
./gradlew --stacktrace --warning-mode=all trellis-agent-jpms:build
./gradlew --stacktrace --warning-mode=all trellis-constraint-rules-jpms:build
./gradlew --stacktrace --warning-mode=all trellis-http-jpms:build
./gradlew --stacktrace --warning-mode=all trellis-file-jpms:build
./gradlew --stacktrace --warning-mode=all trellis-triplestore-jpms:build
./gradlew --stacktrace --warning-mode=all trellis-event-serialization-jpms:build
./gradlew --stacktrace --warning-mode=all trellis-jms-jpms:build
./gradlew --stacktrace --warning-mode=all trellis-kafka-jpms:build
./gradlew --stacktrace --warning-mode=all trellis-test-jpms:build
./gradlew --stacktrace --warning-mode=all trellis-webac-jpms:build
./gradlew --stacktrace --warning-mode=all trellis-audit-jpms:build
./gradlew --stacktrace --warning-mode=all trellis-app-jpms:build
./gradlew --stacktrace --warning-mode=all trellis-app-triplestore-jpms:build
