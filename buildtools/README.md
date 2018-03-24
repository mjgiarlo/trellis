## JPMS

### Building
Because of gradle not yet supporting multi-module interdependency builds, there is a build sequence:
1. `gradle trellis-vocabulary:build -x test`
2. `gradle trellis-api:build -x test`
3. `gradle trellis-id:build -x test`
4. `gradle trellis-io:build -x test`
5. `gradle trellis-http:build -x test`
6. `gradle trellis-file:build -x test`
7. `gradle trellis-jms:build -x test`
8. `gradle trellis-kafka:build -x test`
9. `gradle trellis-namespaces:build -x test`
10. `gradle trellis-test:build -x test`
11. `gradle trellis-triplestore:build -x test`
12. `gradle build -x test`

See `install-jpms.sh` script to execute.

Note that tests are excluded because of an [open issue](https://github.com/gradle/gradle/issues/2657) with `gradle`