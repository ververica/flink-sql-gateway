# Flink SQL Gateway

Flink SQL gateway is a service that allows other applications to easily interact with a Flink cluster through a REST API. 

User applications (e.g. Java/Python/Shell program, Postman) can use the REST API to submit queries, cancel jobs, retrieve results, etc. 

[Flink JDBC driver](https://github.com/ververica/flink-jdbc-driver) enables JDBC clients to connect to Flink SQL gateway based on the REST API.

Currently, the REST API is a set of internal APIs and we recommend users to interact with the gateway through JDBC API. 
Flink SQL gateway stores the session properties in memory now. If the service is stopped or crashed, all properties are lost. We will improve this in the future.

This project is at an early stage. Feel free to file an issue if you meet any problems or have any suggestions.

## Startup Gateway Service

There are four steps to start the service from scratch:

1. Download (or build) the Flink package. Flink SQL gateway currently only supports Apache Flink 1.10, you can download Flink 1.10 from [here](https://flink.apache.org/downloads.html#apache-flink-1100).

2. Start up a Flink cluster. Flink SQL gateway requires a running Flink cluster where table programs can be executed. For more information about setting up a Flink cluster see the [Cluster & Deployment](https://ci.apache.org/projects/flink/flink-docs-release-1.10/ops/deployment/cluster_setup.html) part.

3. Configure the `FLINK_HOME` environment variable with the command: `export FLINK_HOME=<flink-install-dir>` and add the same command to your bash configuration file like `~/.bashrc` or `~/.bash_profile`

4. Download from the [download page](https://github.com/ververica/flink-sql-gateway/releases) (or build) the Flink SQL gateway package, and execute `./bin/sql-gateway.sh`

The gateway can be started with the following optional command line arguments.
```
./bin/sql-gateway.sh -h

The following options are available:
     -d,--defaults <default configuration file>   The properties with which every new session is initialized. 
                                                  Properties might be overwritten by session properties.
     -h,--help                                    Show the help message with descriptions of all options.
     -j,--jar <JAR file>                          A JAR file to be imported into the session. 
                                                  The file might contain user-defined classes needed for 
                                                  statements such as functions, the execution of table sources,
                                                  or sinks. Can be used multiple times.
     -l,--library <JAR directory>                 A JAR file directory with which every new session is initialized. 
                                                  The files might contain user-defined classes needed for 
                                                  the execution of statements such as functions,
                                                  table sources, or sinks. Can be used multiple times.
     -p,--port <service port>                     The port to which the REST client connects to.
```

If no configuration file is specified, the gateway will read its default configuration from the file located in `./conf/sql-gateway-defaults.yaml`. See the [configuration](#Configuration) part for more information about the structure of environment files. 
If no port is specified in CLI commands or in the configuration file, the gateway will be started with the default port `8083`. 

## Dependencies

The gateway does not require to setup a Java project using Maven or SBT. Instead, you can pass the dependencies as regular JAR files that get submitted to the cluster. You can either specify each JAR file separately (using `--jar`) or define entire library directories (using `--library`). For connectors to external systems (such as Apache Kafka) and corresponding data formats (such as JSON), Flink provides **ready-to-use JAR bundles**. These JAR files can be downloaded for each release from the Maven central repository.

The full list of offered SQL JARs and documentation about how to use them can be found on the [connection to external systems page](https://ci.apache.org/projects/flink/flink-docs-release-1.10/dev/table/connect.html).

## Run Gateway with Different Executors
You might want to run the gateway on a standalone Flink cluster or with Yarn / Kubernetes deployment. Flink SQL gateway currently supports the following executors:
* **[Standalone Flink Session](https://ci.apache.org/projects/flink/flink-docs-release-1.10/ops/deployment/cluster_setup.html)**: This is the default executor in Flink. No further configuration is needed.
* **[Flink on Yarn Session](https://ci.apache.org/projects/flink/flink-docs-release-1.10/ops/deployment/yarn_setup.html#flink-yarn-session)**: Set the following options in `$FLINK_HOME/conf/flink-conf.yaml` to use this executor.
    ```
    execution.target: yarn-session
    ```
* **[Flink on Yarn per Job](https://ci.apache.org/projects/flink/flink-docs-release-1.10/ops/deployment/yarn_setup.html#run-a-single-flink-job-on-yarn)**: Set the following options in `$FLINK_HOME/conf/flink-conf.yaml` to use this executor.
    ```
    execution.target: yarn-per-job
    ```
* **[Standalone Flink Session on Kubernetes](https://ci.apache.org/projects/flink/flink-docs-stable/ops/deployment/kubernetes.html)**: This is the same with the normal standalone Flink session. No further configuration is needed.
* **[Native Flink Session on Kubernetes](https://ci.apache.org/projects/flink/flink-docs-release-1.10/ops/deployment/native_kubernetes.html)**: Set the following options in `$FLINK_HOME/conf/flink-conf.yaml` to use this executor.
    ```
    execution.target: kubernetes-session
    kubernetes.cluster-id: <your-flink-cluster-id>
    ```

Flink SQL gateway currently hasn't been tested against per-job execution mode. We'll test and support this in the future.

## Documentation

See [the docs directory](docs).
