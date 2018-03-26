#!/usr/bin/env bash

./gradlew trellis-vocabulary:build -x test
./gradlew trellis-api:build -x test
./gradlew trellis-id:build -x test
./gradlew trellis-io:build -x test
./gradlew trellis-http:build -x test
./gradlew trellis-file:build -x test
./gradlew trellis-jms:build -x test
./gradlew trellis-kafka:build -x test
./gradlew trellis-namespaces:build -x test
./gradlew trellis-test:build -x test
./gradlew trellis-triplestore:build -x test
./gradlew build -x test