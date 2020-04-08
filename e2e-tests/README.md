# Flink SQL Gateway End-to-End Tests

This directory contains tests that verify end-to-end behaviour of Flink SQL gateway.

## Running Tests

Before running the tests, you should have a Flink cluster configured. If you want to run the tests on a Flink session cluster, the session cluster must be started before running the tests.

### Running Tests against different Execution Targets

To run the tests against different executors (for example, on a standlone Flink session, a Flink on Yarn session or a Flink on Yarn per-job executor), please change the `execution.target` and other related options in `$FLINK_HOME/conf/flink-conf.yaml` before running the tests. See "Run Gateway with Different Executors" of the documentation in the project root for the detailed settings.

Note that if you want to run the tests against a Flink on Yarn per job cluster, it's recommended to set both `jobmanager.heap.size` and `taskmanager.memory.process.size` options to `4096m`.

### Running Tests on a Local Flink Standalone Session Cluster

To quickly run the tests on a local Flink standalone session cluster (the job manager and all task managers are on the same local machine), you should first start the local cluster and then run the following command:

```
FLINK_HOME=<flink home> e2e-tests/run-tests.sh
```

where `<flink home>` is a Flink distribution directory.

### Running Tests on a Flink Cluster with Multiple Machines

Running tests on a Flink cluster with multiple machines requires an available HDFS service to store test data such that it can be accessed across multiple machines. 

You can now run all the tests by executing
```
FLINK_HOME=<flink home> HDFS_ADDRESS=<hdfs host>:<hdfs port> e2e-tests/run-tests.sh
```

where `<flink home>` is a Flink distribution directory, `<hdfs host>` is the ip or host name of an hdfs service available and `<hdfs port>` is the port of the hdfs service.
