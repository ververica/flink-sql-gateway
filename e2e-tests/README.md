# Flink SQL Gateway End-to-End Tests

This directory contains tests that verify end-to-end behaviour of Flink SQL gateway.

## Running Tests

Before running the tests, you should have a Flink cluster configured. If you want to run the tests on a Flink session cluster, the session cluster must be started before running the tests.

To run the tests against different executors (for example, on a standlone Flink session, a Flink on Yarn session or a Flink on Yarn per-job executor), please change the `execution.target` and other related options in `$FLINK_HOME/conf/flink-conf.yaml` before running the tests. See "Run Gateway with Different Executors" of the documentation in the project root for the detailed settings. 

You can now run all the tests by executing
```
FLINK_HOME=<flink home> HDFS_ADDRESS=<hdfs host>:<hdfs port> e2e-tests/run-tests.sh
```

where `<flink home>` is a Flink distribution directory, `<hdfs host>` is the ip or host name of an hdfs service available and `<hdfs port>` is the port of the hdfs service.
