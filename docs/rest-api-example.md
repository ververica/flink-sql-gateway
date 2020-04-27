# Flink SQL Gateway REST API Usage Examples

Here’s a step-by-step example of interacting with Flink SQL gateway in Python3 with the Requests library. By default the gateway runs on port `8083` (which can be changed with the server.port config option).

Before trying on this example, please make sure that

* Both Python3 and the Requests library have been installed on your machine.
* A Flink cluster is already running. In this example we use a standalone Flink cluster on the local machine, but you can of course use any types of Flink clusters as you wish.
* A Flink SQL gateway is already running. In this example we have our gateway also on our local machine.

## Create a Session

We'll start off with a session using Blink planner in streaming mode. Start a Python3 interactive shell and type the following commands:

```python3
import json, requests, pprint
host = 'http://localhost:8083'
data = {'planner': 'blink', 'execution_type': 'streaming'}
headers = {'Content-Type': 'application/json'}
r = requests.post(host + '/v1/sessions', data=json.dumps(data), headers=headers)

r.json()
# => {'session_id': '918e6d1eff0c14228069edec25144022'}
# your session_id will (obviously) be different from this
session_id = r.json()['session_id']
```

## Create a Testing Database and Table

We can now execute a `CREATE DATABASE` statement in the new session to create a testing database.

```python3
data = {'statement': 'CREATE DATABASE test_db'}
r = requests.post(host + '/v1/sessions/' + session_id + '/statements', data=json.dumps(data), headers=headers)

pprint.pprint(r.json())
''' =>
{'results': [{'columns': [{'name': 'result', 'type': 'VARCHAR(2)'}],
              'data': [['OK']],
              'result_kind': 'SUCCESS'}],
 'statement_types': ['CREATE_DATABASE']}
'''
```

We now goes into the `test_db` database with `USE` statement.

```python3
data = {'statement': 'USE test_db'}
r = requests.post(host + '/v1/sessions/' + session_id + '/statements', data=json.dumps(data), headers=headers)

pprint.pprint(r.json())
''' =>
{'results': [{'columns': [{'name': 'result', 'type': 'VARCHAR(2)'}],
              'data': [['OK']],
              'result_kind': 'SUCCESS'}],
 'statement_types': ['USE']}
'''
```

We now create a testing table with `CREATE TABLE` statement.

The DDL below defines a table which stores its data in `/tmp/t.csv` on our local machine. You'll need to make sure that `/tmp/t.csv` does not exist before executing this statement.

If you're running a remote Flink cluster with multiple workers, you might need to change the file path to an external storage system like HDFS.

```python3
ddl = '''
CREATE TABLE t(
  a INT,
  b VARCHAR
) WITH (
  'connector.type'='filesystem',
  'connector.path'='/tmp/t.csv',
  'format.type' = 'csv',
  'format.derive-schema' = 'true',
  'format.field-delimiter' = '|'
)
'''
data = {'statement': ddl}
r = requests.post(host + '/v1/sessions/' + session_id + '/statements', data=json.dumps(data), headers=headers)

pprint.pprint(r.json())
''' =>
{'results': [{'columns': [{'name': 'result', 'type': 'VARCHAR(2)'}],
              'data': [['OK']],
              'result_kind': 'SUCCESS'}],
 'statement_types': ['CREATE_TABLE']}
'''
```

Let's now double-check that our table has been successfully created with the `SHOW TABLES` statement.

```python3
data = {'statement': 'SHOW TABLES'}
r = requests.post(host + '/v1/sessions/' + session_id + '/statements', data=json.dumps(data), headers=headers)

pprint.pprint(r.json())
''' =>
{'results': [{'columns': [{'name': 'tables', 'type': 'VARCHAR(1) NOT NULL'}],
              'data': [['t']],
              'result_kind': 'SUCCESS_WITH_CONTENT'}],
 'statement_types': ['SHOW_TABLES']}
'''
```

## Insert Data into Testing Table

We now insert some test data into our testing table using the `INSERT` statement.

```python3
insertion = '''
INSERT INTO t VALUES
  (1, 'Flink'),
  (2, 'SQL'),
  (3, 'gateway'),
  (4, 'foo'),
  (5, 'bar'),
  (6, 'Hi'),
  (7, 'Hello')
'''
data = {'statement': insertion}
r = requests.post(host + '/v1/sessions/' + session_id + '/statements', data=json.dumps(data), headers=headers)

pprint.pprint(r.json())
''' =>
{'results': [{'columns': [{'name': 'job_id', 'type': 'VARCHAR(32) NOT NULL'}],
              'data': [['5c14805d6e502caf61510048192b3835']],
              'result_kind': 'SUCCESS_WITH_CONTENT'}],
 'statement_types': ['INSERT_INTO']}
'''
job_id = r.json()['results'][0]['data'][0][0]
#                            |          |  └--- the 1st column in the 1st result row
#                            |          └------ the 1st result row
#                            └----------------- the result of the 1st statement (we'll support executing multiple statements in the future)
```

Note that executing a `INSERT` statement means to submit it to the Flink cluster. To fetch the result of this statement, we need to do another API call.

```python3
r = requests.get(host + '/v1/sessions/' + session_id + '/jobs/' + job_id + '/result/0', headers=headers)

pprint.pprint(r.json())
''' =>
{'next_result_uri': '/v1/sessions/918e6d1eff0c14228069edec25144022/jobs/5c14805d6e502caf61510048192b3835/result/1',
 'results': [{'columns': [{'name': 'affected_row_count',
                           'type': 'BIGINT NOT NULL'}],
              'data': [[-2]],
              'result_kind': 'SUCCESS_WITH_CONTENT'}]}
'''
```

The result of a `INSERT` statement will be returned once the insertion finished (be aware that if you're inserting a never-ending stream, this API call will block forever unless the job is canceled).

We currently does not support returning the number of rows inserted, and the `-2` in the result is from JDBC standard, indicating that this operation successes with no information.

## Do some SQL Query

We can now perform SQL queries on our testing table.

```python3
data = {'statement': 'SELECT * FROM t'}
# this line may take some time
r = requests.post(host + '/v1/sessions/' + session_id + '/statements', data=json.dumps(data), headers=headers)

pprint.pprint(r.json())
''' =>
{'results': [{'columns': [{'name': 'job_id', 'type': 'VARCHAR(32) NOT NULL'}],
              'data': [['b1719d4ea8f6d41a830d44918cd7e4d0']],
              'result_kind': 'SUCCESS_WITH_CONTENT'}],
 'statement_types': ['SELECT']}
'''
job_id = r.json()['results'][0]['data'][0][0]
#                            |          |  └--- the 1st column in the 1st result row
#                            |          └------ the 1st result row
#                            └----------------- the result of the 1st statement (we'll support executing multiple statements in the future)
```

We now fetch the results of this query. To illustrate the usage of token, we limit the number of results returned each time with `max_fetch_size`.

```python3
data = {'max_fetch_size': 3}
token = 0
r = requests.get(host + '/v1/sessions/' + session_id + '/jobs/' + job_id + '/result/' + str(token), data=json.dumps(data), headers=headers)

pprint.pprint(r.json())
''' =>
{'next_result_uri': '/v1/sessions/8797639f35e1ed71b734c035e03e98da/jobs/b1719d4ea8f6d41a830d44918cd7e4d0/result/1',
 'results': [{'change_flags': [True, True, True],
              'columns': [{'name': 'a', 'type': 'INT'},
                          {'name': 'b', 'type': 'STRING'}],
              'data': [[1, 'Flink'], [2, 'SQL'], [3, 'gateway']],
              'result_kind': 'SUCCESS_WITH_CONTENT'}]}
'''
next_result_uri = r.json()['next_result_uri']
```

To continue fetching the next part of results, either follows the `next_result_uri`, or simply increase the `token` by 1 and send out the request.

```python3
# we still increase the token here for consistency, although we're not using it
token += 1
r = requests.get(host + next_result_uri, data=json.dumps(data), headers=headers)

pprint.pprint(r.json())
''' =>
{'next_result_uri': '/v1/sessions/8797639f35e1ed71b734c035e03e98da/jobs/b1719d4ea8f6d41a830d44918cd7e4d0/result/2',
 'results': [{'change_flags': [True, True, True],
              'columns': [{'name': 'a', 'type': 'INT'},
                          {'name': 'b', 'type': 'STRING'}],
              'data': [[4, 'foo'], [5, 'bar'], [6, 'Hi']],
              'result_kind': 'SUCCESS_WITH_CONTENT'}]}
'''

token += 1
r = requests.get(host + '/v1/sessions/' + session_id + '/jobs/' + job_id + '/result/' + str(token), data=json.dumps(data), headers=headers)

pprint.pprint(r.json())
''' =>
{'next_result_uri': '/v1/sessions/8797639f35e1ed71b734c035e03e98da/jobs/b1719d4ea8f6d41a830d44918cd7e4d0/result/3',
 'results': [{'change_flags': [True],
              'columns': [{'name': 'a', 'type': 'INT'},
                          {'name': 'b', 'type': 'STRING'}],
              'data': [[7, 'Hello']],
              'result_kind': 'SUCCESS_WITH_CONTENT'}]}
'''
```

If you fail to receive the results, just send out the same request again.

```python3
r = requests.get(host + '/v1/sessions/' + session_id + '/jobs/' + job_id + '/result/' + str(token), data=json.dumps(data), headers=headers)

pprint.pprint(r.json())
''' =>
{'next_result_uri': '/v1/sessions/8797639f35e1ed71b734c035e03e98da/jobs/b1719d4ea8f6d41a830d44918cd7e4d0/result/3',
 'results': [{'change_flags': [True],
              'columns': [{'name': 'a', 'type': 'INT'},
                          {'name': 'b', 'type': 'STRING'}],
              'data': [[7, 'Hello']],
              'result_kind': 'SUCCESS_WITH_CONTENT'}]}
'''
```

An empty response indicates the end of the results.

```python3
token += 1
r = requests.get(host + '/v1/sessions/' + session_id + '/jobs/' + job_id + '/result/' + str(token), data=json.dumps(data), headers=headers)

pprint.pprint(r.json())
# => {}
```

## Close the Session

We now close the session and finish our example.

```python3
r = requests.delete(host + '/v1/sessions/' + session_id, headers=headers)

pprint.pprint(r.json())
# => {'status': 'CLOSED'}
```
