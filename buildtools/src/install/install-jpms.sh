#!/usr/bin/env bash
cd ../../..
gradle trellis-vocabulary:build -x test
gradle trellis-api:build -x test
gradle trellis-id:build -x test
gradle trellis-io:build -x test
gradle trellis-http:build -x test
gradle trellis-file:build -x test
gradle trellis-jms:build -x test
gradle trellis-kafka:build -x test
gradle trellis-namespaces:build -x test
gradle trellis-test:build -x test
gradle trellis-triplestore:build -x test
gradle build -x test