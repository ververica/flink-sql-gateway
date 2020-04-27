# Flink SQL Gateway REST API

## Error Handling

When the REST API encounters an error, it will return a JSON object containing only one field "errors", a string array in which the strings are the details of the errors. This behavior reuses the error handling in the original Flink REST server code. We will not apply additional "error" fields in the following APIs.

Currently, the HTTP response code when encountering an error will be either 400 BAD REQUEST (if the parameters provided by the user is invalid) or 500 INTERNAL SERVER ERROR (otherwise).

## Basic JSON Objects

### Result Set

A result set is a JSON object indicating the execution results of a statement or a job.

<table>
  <tr>
    <th>Field</th>
    <th>Type</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>result_kind</td>
    <td>string</td>
    <td><p>Type of the result set, which is either <code>SUCCESS</code> or <code>SUCCESS_WITH_CONTENT</code>.</p><ul><li><code>SUCCESS</code> is just an acknowledgement indicating that your request is successfully handled.</li><li><code>SUCCESS_WITH_CONTENT</code> indicates that there are more information in the result set.</li></ul></td>
  </tr>
  <tr>
    <td>columns</td>
    <td>array of column type</td>
    <td>The i-th element in the array indicates the type of the i-th column of this result set.</td>
  </tr>
  <tr>
    <td>data</td>
    <td>array of array</td>
    <td>The i-th element (which is also an array) in the array indicates the i-th result row of this result set.</td>
  </tr>
  <tr>
    <td>change_flags</td>
    <td>array of boolean</td>
    <td><p>This field only appears for a streaming job result.</p><ul><li>If the i-th element is `true`, the i-th result row should be appended to the results of the job.</li><li>If the i-th element is `false`, the i-th result row should be retracted from the results of the job.</li></ul></td>
  </tr>
</table>

### Column Type

A column type is a JSON object indicating the type of a column in the result set.

<table>
  <tr>
    <th>Field</th>
    <th>Type</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>name</td>
    <td>string</td>
    <td>Name of this column.</td>
  </tr>
  <tr>
    <td>type</td>
    <td>string</td>
    <td>The summary string of the <code>LogicalType</code> of this column. See Flink's <a href="https://github.com/apache/flink/blob/master/flink-table/flink-table-common/src/main/java/org/apache/flink/table/types/logical/LogicalType.java"><code>LogicalType#asSummaryString</code></a> for detailed format.</td>
  </tr>
</table>

## Get Cluster Info

Get meta data for this cluster.

<table>
  <tr>
    <th>Path</th>
    <td colspan="3">/v1/info</td>
  </tr>
  <tr>
    <th>Verb</th>
    <td>GET</td>
    <th>Response Code</td>
    <td>200 OK</td>
  </tr>
  <tr>
    <th>Request Body</th>
    <td colspan="3">empty</td>
  </tr>
  <tr>
    <th rowspan="3">Response Body</th>
    <th>Parameter</th>
    <th>Type</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>product_name</td>
    <td>string</td>
    <td>Must be <code>Apache Flink</code>.</td>
  </tr>
  <tr>
    <td>version</td>
    <td>string</td>
    <td>Version of Flink.</td>
  </tr>
</table>

## Create a Session

Create a new session with a specific `planner` and `execution_type`. A specific `properties` could be given for current session which will override the gateway's default properties.

<table>
  <tr>
    <th>Path</th>
    <td colspan="3">/v1/sessions</td>
  </tr>
  <tr>
    <th>Verb</th>
    <td>POST</td>
    <th>Response Code</td>
    <td>200 OK</td>
  </tr>
  <tr>
    <th rowspan="5">Request Body</th>
    <th>Parameter</th>
    <th>Type</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>planner</td>
    <td>string</td>
    <td><b>[required]</b> Planner of the new session.<br>Must be either <code>old</code> or <code>blink</code>.</td>
  </tr>
  <tr>
    <td>execution_type</td>
    <td>string</td>
    <td><b>[required]</b> Execution type of the new session.<br>Must be either <code>batch</code> or <code>streaming</code>.</td>
  </tr>
  <tr>
    <td>session_name</td>
    <td>string</td>
    <td>[optional] Name of the new session.</td>
  </tr>
  <tr>
    <td>properties</td>
    <td>map</td>
    <td>[optional] Properties of the new session.<br>These will override the gateway's default properties.</td>
  </tr>
  <tr>
    <th rowspan="2">Response Body</th>
    <th>Parameter</th>
    <th>Type</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>session_id</td>
    <td>string</td>
    <td>ID of the new session.</td>
  </tr>
</table>

## Trigger Heartbeat of a Session

Trigger heartbeat to tell the server that the client is active and to keep the session alive.

If the server does not receive any heartbeats or operations for a session, the session will be destroyed when the timeout is reached.

<table>
  <tr>
    <th>Path</th>
    <td colspan="3">/v1/sessions/:session_id/heartbeat</td>
  </tr>
  <tr>
    <th>Verb</th>
    <td>POST</td>
    <th>Response Code</td>
    <td>200 OK</td>
  </tr>
  <tr>
    <th rowspan="2">Path Parameter</th>
    <th>Parameter</th>
    <th colspan="2">Description</th> 
  </tr>
  <tr>
    <td>session_id</td>
    <td colspan="2">ID of the session whose heartbeat is being triggered.</td>
  </tr>
  <tr>
    <th>Request Body</th>
    <td colspan="3">empty</td>
  </tr>
  <tr>
    <th>Response Body</th>
    <td colspan="3">empty</td>
  </tr>
</table>

## Close a Session

Close a session and release related resources including jobs and properties.

Note that closing a session does not cancel unfinished jobs. It's just that the gateway will not track these jobs anymore.

<table>
  <tr>
    <th>Path</th>
    <td colspan="3">/v1/sessions/:session_id</td>
  </tr>
  <tr>
    <th>Verb</th>
    <td>DELETE</td>
    <th>Response Code</td>
    <td>200 OK</td>
  </tr>
  <tr>
    <th rowspan="2">Path Parameter</th>
    <th>Parameter</th>
    <th colspan="2">Description</th> 
  </tr>
  <tr>
    <td>session_id</td>
    <td colspan="2">ID of the session which is going to be closed.</td>
  </tr>
  <tr>
    <th>Request Body</th>
    <td colspan="3">empty</td>
  </tr>
  <tr>
    <th rowspan="2">Response Body</th>
    <th>Parameter</th>
    <th>Type</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>status</td>
    <td>string</td>
    <td>Must be <code>CLOSED</code>.</td>
  </tr>
</table>

## Execute a Statement

Execute a **single** statement. An exception will be thrown if multiple statements are given in one call. We'll support multiple statements in the future.

<table>
  <tr>
    <th>Path</th>
    <td colspan="3">/v1/sessions/:session_id/statements</td>
  </tr>
  <tr>
    <th>Verb</th>
    <td>POST</td>
    <th>Response Code</td>
    <td>200 OK</td>
  </tr>
  <tr>
    <th rowspan="2">Path Parameter</th>
    <th>Parameter</th>
    <th colspan="2">Description</th>
  </tr>
  <tr>
    <td>session_id</td>
    <td colspan="2">ID of the session in which the statement is going to execute.</td>
  </tr>
  <tr>
    <th rowspan="2">Request Body</th>
    <th>Parameter</th>
    <th>Type</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>statement</td>
    <td>string</td>
    <td><b>[required]</b> A single statement to execute.</td>
  </tr>
  <tr>
    <th rowspan="3">Response Body</th>
    <th>Parameter</th>
    <th>Type</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>results</td>
    <td>array of result set</td>
    <td>The results of executing the statement.<br>Currently there is only one result set in the array.</td>
  </tr>
  <tr>
    <td>statement_types</td>
    <td>array of string</td>
    <td>The types of the submitted statement.<br>Currently there is only one string in the array.</td>
  </tr>
</table>

### Supported Statements

For a description of what each statement does, please refer to the <a href="https://github.com/ververica/flink-sql-gateway/blob/master/README.md">project's README file</a>.

#### SELECT

<table>
  <tr>
    <th>Statement Type</th>
    <td>SELECT</td>
    <th>Example</th>
    <td><code>SELECT * FROM t</code></td>
  </tr>
  <tr>
    <th>Result Kind</th>
    <td colspan="3">SUCCESS_WITH_CONTENT</td>
  </tr>
  <tr>
    <th>Row Count</th>
    <td colspan="3">1</td>
  </tr>
  <tr>
    <th rowspan="2">Result Columns</th>
    <th>Column Name</th>
    <th>Column Type</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>job_id</td>
    <td>varchar</td>
    <td>By executing a <code>SELECT</code> statement we mean to submit the query to the cluster. This column contains the ID of the generated Flink job. To fetch the result of this job, see "Fetching the Result" section.</td>
  </tr>
</table>

#### INSERT

<table>
  <tr>
    <th>Statement Type</th>
    <td>INSERT</td>
    <th>Example</th>
    <td><code>INESRT INTO t2 SELECT * FROM t1</code><br><code>INSERT OVERWRITE t2 SELECT * FROM t1</code></td>
  </tr>
  <tr>
    <th>Result Kind</th>
    <td colspan="3">SUCCESS_WITH_CONTENT</td>
  </tr>
  <tr>
    <th>Row Count</th>
    <td colspan="3">1</td>
  </tr>
  <tr>
    <th rowspan="2">Result Columns</th>
    <th>Column Name</th>
    <th>Column Type</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>job_id</td>
    <td>varchar</td>
    <td>By executing a <code>INSERT</code> statement we mean to submit the statement to the cluster. This column contains the ID of the generated Flink job. To fetch the result of this job, see "Fetching the Result" section.</td>
  </tr>
</table>

#### DDL

<table>
  <tr>
    <th colspan="2">Statement Type</th>
    <th colspan="2">Example</th>
  </tr>
  <tr>
    <td colspan="2">CREATE_TABLE</td>
    <td colspan="2"><code>CREATE TABLE t( a INT, b BIGINT )</code></td>
  </tr>
  <tr>
    <td colspan="2">DROP_TABLE</td>
    <td colspan="2"><code>DROP TABLE t</code></td>
  </tr>
  <tr>
    <td colspan="2">ALTER_TABLE</td>
    <td colspan="2"><code>ALTER TABLE t1 RENAME TO t2</code></td>
  </tr>
  <tr>
    <td colspan="2">CREATE_VIEW</td>
    <td colspan="2"><code>CREATE VIEW myview AS SELECT * FROM t</code></td>
  </tr>
  <tr>
    <td colspan="2">DROP_VIEW</td>
    <td colspan="2"><code>DROP VIEW myview</code></td>
  </tr>
  <tr>
    <td colspan="2">CREATE_DATABASE</td>
    <td colspan="2"><code>CREATE DATABASE mydb</code></td>
  </tr>
  <tr>
    <td colspan="2">DROP_DATABASE</td>
    <td colspan="2"><code>DROP DATABASE mydb</code></td>
  </tr>
  <tr>
    <td colspan="2">ALTER_DATABASE</td>
    <td colspan="2"><code>ALTER DATABASE mydb SET ('k1' = 'a')</code></td>
  </tr>
  <tr>
    <th>Result Kind</th>
    <td colspan="3">SUCCESS</td>
  </tr>
  <tr>
    <th>Row Count</th>
    <td colspan="3">1</td>
  </tr>
  <tr>
    <th rowspan="2">Result Columns</th>
    <th>Column Name</th>
    <th>Column Type</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>result</td>
    <td>varchar</td>
    <td>Must be <code>OK</code>.</td>
  </tr>
</table>

#### USE

<table>
  <tr>
    <th colspan="2">Statement Type</th>
    <th colspan="2">Example</th>
  </tr>
  <tr>
    <td colspan="2">USE_CATALOG</td>
    <td colspan="2"><code>USE CATALOG mycatalog</code></td>
  </tr>
  <tr>
    <td colspan="2">USE</td>
    <td colspan="2"><code>USE mydb</code></td>
  </tr>
  <tr>
    <th>Result Kind</th>
    <td colspan="3">SUCCESS</td>
  </tr>
  <tr>
    <th>Row Count</th>
    <td colspan="3">1</td>
  </tr>
  <tr>
    <th rowspan="2">Result Columns</th>
    <th>Column Name</th>
    <th>Column Type</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>result</td>
    <td>varchar</td>
    <td>Must be <code>OK</code>.</td>
  </tr>
</table>

#### SHOW CATALOGS

<table>
  <tr>
    <th>Statement Type</th>
    <td>SHOW_CATALOGS</td>
    <th>Example</th>
    <td><code>SHOW CATALOGS</code></td>
  </tr>
  <tr>
    <th>Result Kind</th>
    <td colspan="3">SUCCESS_WITH_CONTENT</td>
  </tr>
  <tr>
    <th>Row Count</th>
    <td colspan="3">&ge;1</td>
  </tr>
  <tr>
    <th rowspan="2">Result Columns</th>
    <th>Column Name</th>
    <th>Column Type</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>catalogs</td>
    <td>varchar</td>
    <td>Catalog names.</td>
  </tr>
</table>

#### SHOW DATABASES

<table>
  <tr>
    <th>Statement Type</th>
    <td>SHOW_DATABASES</td>
    <th>Example</th>
    <td><code>SHOW DATABASES</code></td>
  </tr>
  <tr>
    <th>Result Kind</th>
    <td colspan="3">SUCCESS_WITH_CONTENT</td>
  </tr>
  <tr>
    <th>Row Count</th>
    <td colspan="3">&ge;1</td>
  </tr>
  <tr>
    <th rowspan="2">Result Columns</th>
    <th>Column Name</th>
    <th>Column Type</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>databases</td>
    <td>varchar</td>
    <td>Database names in the current catalog.</td>
  </tr>
</table>

#### SHOW TABLES

<table>
  <tr>
    <th>Statement Type</th>
    <td>SHOW_TABLES</td>
    <th>Example</th>
    <td><code>SHOW TABLES</code></td>
  </tr>
  <tr>
    <th>Result Kind</th>
    <td colspan="3">SUCCESS_WITH_CONTENT</td>
  </tr>
  <tr>
    <th>Row Count</th>
    <td colspan="3">&ge;1</td>
  </tr>
  <tr>
    <th rowspan="2">Result Columns</th>
    <th>Column Name</th>
    <th>Column Type</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>tables</td>
    <td>varchar</td>
    <td>Table and view names in the current database.</td>
  </tr>
</table>

#### SHOW VIEWS

<table>
  <tr>
    <th>Statement Type</th>
    <td>SHOW_VIEWS</td>
    <th>Example</th>
    <td><code>SHOW VIEWS</code></td>
  </tr>
  <tr>
    <th>Result Kind</th>
    <td colspan="3">SUCCESS_WITH_CONTENT</td>
  </tr>
  <tr>
    <th>Row Count</th>
    <td colspan="3">&ge;1</td>
  </tr>
  <tr>
    <th rowspan="2">Result Columns</th>
    <th>Column Name</th>
    <th>Column Type</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>views</td>
    <td>varchar</td>
    <td>View names in the current database.</td>
  </tr>
</table>

#### SHOW FUNCTIONS

<table>
  <tr>
    <th>Statement Type</th>
    <td>SHOW_FUNCTIONS</td>
    <th>Example</th>
    <td><code>SHOW FUNCTIONS</code></td>
  </tr>
  <tr>
    <th>Result Kind</th>
    <td colspan="3">SUCCESS_WITH_CONTENT</td>
  </tr>
  <tr>
    <th>Row Count</th>
    <td colspan="3">&ge;1</td>
  </tr>
  <tr>
    <th rowspan="2">Result Columns</th>
    <th>Column Name</th>
    <th>Column Type</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>functions</td>
    <td>varchar</td>
    <td>Function names.</td>
  </tr>
</table>

#### DESCRIBE TABLE

<table>
  <tr>
    <th>Statement Type</th>
    <td>DESCRIBE_TABLE</td>
    <th>Example</th>
    <td><code>DESCRIBE t</code></td>
  </tr>
  <tr>
    <th>Result Kind</th>
    <td colspan="3">SUCCESS_WITH_CONTENT</td>
  </tr>
  <tr>
    <th>Row Count</th>
    <td colspan="3">1</td>
  </tr>
  <tr>
    <th rowspan="2">Result Columns</th>
    <th>Column Name</th>
    <th>Column Type</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>schema</td>
    <td>varchar</td>
    <td>A serialized JSON string of the schema of the specified table. See gateway's <a href="https://github.com/ververica/flink-sql-gateway/blob/master/src/main/java/com/ververica/flink/table/gateway/rest/result/TableSchemaUtil.java"><code>TableSchemaUtil</code></a> for detailed format.</td>
  </tr>
</table>

#### EXPLAIN

<table>
  <tr>
    <th>Statement Type</th>
    <td>EXPLAIN</td>
    <th>Example</th>
    <td><code>EXPLAIN PLAN FOR SELECT * FROM t</code></td>
  </tr>
  <tr>
    <th>Result Kind</th>
    <td colspan="3">SUCCESS_WITH_CONTENT</td>
  </tr>
  <tr>
    <th>Row Count</th>
    <td colspan="3">1</td>
  </tr>
  <tr>
    <th rowspan="2">Result Columns</th>
    <th>Column Name</th>
    <th>Column Type</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>explanation</td>
    <td>varchar</td>
    <td>Plan of the specified statement.</td>
  </tr>
</table>

#### SET

**Set with no parameters that lists all properties**

<table>
  <tr>
    <th>Statement Type</th>
    <td>SET</td>
    <th>Example</th>
    <td><code>SET</code></td>
  </tr>
  <tr>
    <th>Result Kind</th>
    <td colspan="3">SUCCESS_WITH_CONTENT</td>
  </tr>
  <tr>
    <th>Row Count</th>
    <td colspan="3">&ge;1</td>
  </tr>
  <tr>
    <th rowspan="3">Result Columns</th>
    <th>Column Name</th>
    <th>Column Type</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>key</td>
    <td>varchar</td>
    <td>Key of the property.</td>
  </tr>
  <tr>
    <td>value</td>
    <td>varchar</td>
    <td>Value of the property.</td>
  </tr>
</table>

**Set with parameters that modifies a property**

<table>
  <tr>
    <th>Statement Type</th>
    <td>SET</td>
    <th>Example</th>
    <td><code>SET execution.parallelism=10</code></td>
  </tr>
  <tr>
    <th>Result Kind</th>
    <td colspan="3">SUCCESS</td>
  </tr>
  <tr>
    <th>Row Count</th>
    <td colspan="3">1</td>
  </tr>
  <tr>
    <th rowspan="2">Result Columns</th>
    <th>Column Name</th>
    <th>Column Type</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>result</td>
    <td>varchar</td>
    <td>Must be <code>OK</code>.</td>
  </tr>
</table>

#### RESET

<table>
  <tr>
    <th>Statement Type</th>
    <td>RESET</td>
    <th>Example</th>
    <td><code>RESET ALL</code></td>
  </tr>
  <tr>
    <th>Result Kind</th>
    <td colspan="3">SUCCESS</td>
  </tr>
  <tr>
    <th>Row Count</th>
    <td colspan="3">1</td>
  </tr>
  <tr>
    <th rowspan="2">Result Columns</th>
    <th>Column Name</th>
    <th>Column Type</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>result</td>
    <td>varchar</td>
    <td>Must be <code>OK</code>.</td>
  </tr>
</table>

## Fetch the Result

Fetch a part of the results of a Flink job.

To deal with possible network or client failures, we introduce a token mechanism in this API. The init value of the token is 0. When the client successfully receives a part of results, it should increase the token by 1 when asking for the next part of the results; Otherwise it should not change the token and retry fetching the same part of the results. Other token values will cause exceptions.

<table>
  <tr>
    <th>Path</th>
    <td colspan="3">/v1/sessions/:session_id/jobs/:job_id/result/:token</td>
  </tr>
  <tr>
    <th>Verb</th>
    <td>GET</td>
    <th>Response Code</td>
    <td>200 OK</td>
  </tr>
  <tr>
    <th rowspan="4">Path Parameter</th>
    <th>Parameter</th>
    <th colspan="2">Description</th> 
  </tr>
  <tr>
    <td>session_id</td>
    <td colspan="2">ID of the session from which the job is created.</td>
  </tr>
  <tr>
    <td>job_id</td>
    <td colspan="2">ID of the job whose results we're going to fetch.</td>
  </tr>
  <tr>
    <td>token</td>
    <td colspan="2">Indicate whether this request asks for a new part of the results or is a retry.</td>
  </tr>
  <tr>
    <th rowspan="2">Request Body</th>
    <th>Parameter</th>
    <th>Type</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>max_fetch_size</td>
    <td>int</td>
    <td>[optional] Max number of rows returned. Note that the number of returned rows might be smaller than this value.<br>If this request is a retry, this value must be the same with the previous request.</td>
  </tr>
  <tr>
    <th rowspan="4">Response Body</th>
    <td colspan="3">Empty if there is no more results. Otherwise:</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <th>Type</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>results</td>
    <td>array of result set</td>
    <td>A part of the results of the Flink job.</td>
  </tr>
  <tr>
    <td>next_result_uri</td>
    <td>string</td>
    <td>URL for fetching the next part of results. Follow this URL if the client successfully receives the current part of the results.</td>
  </tr>
</table>

## Get Job Status

Get the status of a running job. The gateway only forwards the job status it gets from the Flink cluster without further modifications. If the session expires or the job cannot be found on the cluster, an exception will be thrown.

<table>
  <tr>
    <th>Path</th>
    <td colspan="3">/v1/sessions/:session_id/jobs/:job_id/status</td>
  </tr>
  <tr>
    <th>Verb</th>
    <td>GET</td>
    <th>Response Code</td>
    <td>200 OK</td>
  </tr>
  <tr>
    <th rowspan="3">Path Parameter</th>
    <th>Parameter</th>
    <th colspan="2">Description</th> 
  </tr>
  <tr>
    <td>session_id</td>
    <td colspan="2">ID of the session from which the job is created.</td>
  </tr>
  <tr>
    <td>job_id</td>
    <td colspan="2">ID of the job whose status we're going to fetch.</td>
  </tr>
  <tr>
    <th>Request Body</th>
    <td colspan="3">empty</td>
  </tr>
  <tr>
    <th rowspan="2">Response Body</th>
    <th>Parameter</th>
    <th>Type</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>status</td>
    <td>string</td>
    <td>Status of the Flink job. See Flink's <a href="https://github.com/apache/flink/blob/master/flink-core/src/main/java/org/apache/flink/api/common/JobStatus.java"><code>JobStatus</code></a> for a complete list of status.</td>
  </tr>
</table>

## Cancel a Job

Cancel the running job. If the job is already canceled from the gateway before this API call does nothing. If the session expires or the job cannot be found on the cluster, an exception will be thrown.

<table>
  <tr>
    <th>Path</th>
    <td colspan="3">/v1/sessions/:session_id/jobs/:job_id</td>
  </tr>
  <tr>
    <th>Verb</th>
    <td>DELETE</td>
    <th>Response Code</td>
    <td>200 OK</td>
  </tr>
  <tr>
    <th rowspan="3">Path Parameter</th>
    <th>Parameter</th>
    <th colspan="2">Description</th> 
  </tr>
  <tr>
    <td>session_id</td>
    <td colspan="2">ID of the session from which the job is created.</td>
  </tr>
  <tr>
    <td>job_id</td>
    <td colspan="2">ID of the job we're going to cancel.</td>
  </tr>
  <tr>
    <th>Request Body</th>
    <td colspan="3">empty</td>
  </tr>
  <tr>
    <th rowspan="2">Response Body</th>
    <th>Parameter</th>
    <th>Type</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>status</td>
    <td>string</td>
    <td>Must be <code>CANCELED</code>.</td>
  </tr>
</table>
