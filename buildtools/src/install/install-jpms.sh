#!/usr/bin/env bash

./gradlew trellis-vocabulary:build
./gradlew trellis-api:build
./gradlew trellis-id:build
./gradlew trellis-io:build
./gradlew trellis-namespaces:build
./gradlew trellis-http:build
./gradlew trellis-file:build
./gradlew trellis-jms:build
./gradlew trellis-kafka:build
./gradlew trellis-test:build
./gradlew trellis-triplestore:build
./gradlew trellis-webac:build
./gradlew build