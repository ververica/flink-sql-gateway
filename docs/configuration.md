# Flink SQL Gateway Configuration

A SQL query needs a configuration environment in which it is executed. The so-called environment file defines server properties, session properties, available catalogs, table sources and sinks, user-defined functions and other properties required for execution and deployment.

Every environment file is a regular [YAML file](http://yaml.org/). An example of such a file is presented below.

```
# Define server properties.

server:
  bind-address: 127.0.0.1           # optional: The address that the gateway binds itself (127.0.0.1 by default)
  address: 127.0.0.1                # optional: The address that should be used by clients to connect to the gateway (127.0.0.1 by default)
  port: 8083                        # optional: The port that the client connects to  (8083 by default)
  jvm_args: "-Xmx2018m -Xms1024m"   # optional: The JVM args for SQL gateway process


# Define session properties.

session:
  idle-timeout: 1d                  # optional: Session will be closed when it's not accessed for this duration, which can be disabled by setting to zero. The minimum unit is in milliseconds. (1d by default)
  check-interval: 1h                # optional: The check interval for session idle timeout, which can be disabled by setting to zero. The minimum unit is in milliseconds. (1h by default)
  max-count: 1000000                # optional: Max count of active sessions, which can be disabled by setting to zero. (1000000 by default)


# Define tables here such as sources, sinks, views, or temporal tables.

tables:
  - name: MyTableSource
    type: source-table
    update-mode: append
    connector:
      type: filesystem
      path: "/path/to/something.csv"
    format:
      type: csv
      fields:
        - name: MyField1
          type: INT
        - name: MyField2
          type: VARCHAR
      line-delimiter: "\n"
      comment-prefix: "#"
    schema:
      - name: MyField1
        type: INT
      - name: MyField2
        type: VARCHAR
  - name: MyCustomView
    type: view
    query: "SELECT MyField2 FROM MyTableSource"

# Define user-defined functions here.

functions:
  - name: myUDF
    from: class
    class: foo.bar.AggregateUDF
    constructor:
      - 7.6
      - false

# Define available catalogs

catalogs:
   - name: catalog_1
     type: hive
     property-version: 1
     hive-conf-dir: ...
   - name: catalog_2
     type: hive
     property-version: 1
     default-database: mydb2
     hive-conf-dir: ...
     hive-version: 1.2.1


# Properties that change the fundamental execution behavior of a table program.

execution:
  parallelism: 1                    # optional: Flink's parallelism (1 by default)
  max-parallelism: 16               # optional: Flink's maximum parallelism (128 by default)
  current-catalog: catalog_1        # optional: name of the current catalog of the session ('default_catalog' by default)
  current-database: mydb1           # optional: name of the current database of the current catalog
                                    #   (default database of the current catalog by default)


# Configuration options for adjusting and tuning table programs.

# A full list of options and their default values can be found
# on the dedicated "Configuration" page.
configuration:
  table.optimizer.join-reorder-enabled: true
  table.exec.spill-compression.enabled: true
  table.exec.spill-compression.block-size: 128kb

# Properties that describe the cluster to which table programs are submitted to.

deployment:
  response-timeout: 5000
```

This configuration:
- defines the server startup options that it will be started with bind-address `127.0.0.1`, address `127.0.0.1` and port `8083`,
- defines the session's limitations that a session will be closed when it's not accessed for `1d` (idle-timeout), the server checks whether the session is idle timeout for every `1h` (check-interval), and only `1000000` (max-count ) active sessions is allowed.
- defines an environment with a table source `MyTableSource` that reads from a CSV file,
- defines a view `MyCustomView` that declares a virtual table using a SQL query,
- defines a user-defined function `myUDF` that can be instantiated using the class name and two constructor parameters,
- connects to two Hive catalogs and uses `catalog_1` as the current catalog with `mydb1` as the current database of the catalog,
- the default parallelism is 1, and the max parallelism is 16,
- and makes some planner adjustments around join reordering and spilling via configuration options.

Properties that have been set (using the `SET` command) within a session have highest precedence.
