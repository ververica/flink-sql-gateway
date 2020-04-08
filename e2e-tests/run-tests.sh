#!/usr/bin/env bash
################################################################################
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
################################################################################

TEST_DIR=$(cd $(dirname $BASH_SOURCE); pwd)
source "$TEST_DIR"/test-commons.sh

if [[ -z "$FLINK_HOME" ]]
then
    echo "Please define \$FLINK_HOME first" >> /dev/stderr
    exit 1
fi

echo "Preparing test data..."
if [[ -z "$HDFS_ADDRESS" ]]
then
    echo "No HDFS address provided. Putting test data into /tmp directory..."
    rm -rf /tmp/flink-sql-gateway-test/
    mkdir -p /tmp/flink-sql-gateway-test/
    cp "$TEST_DIR/data/nation.tbl" /tmp/flink-sql-gateway-test/
    data_dir=/tmp/flink-sql-gateway-test/
else
    echo "HDFS address provided, put test data into HDFS..."
    hdfs dfs -rm -r hdfs://"$HDFS_ADDRESS"/tmp/flink-sql-gateway-test/
    hdfs dfs -mkdir -p hdfs://"$HDFS_ADDRESS"/tmp/flink-sql-gateway-test/
    hdfs dfs -copyFromLocal "$TEST_DIR/data/nation.tbl" hdfs://"$HDFS_ADDRESS"/tmp/flink-sql-gateway-test/
    data_dir=hdfs://"$HDFS_ADDRESS"/tmp/flink-sql-gateway-test/
fi

function cleanup() {
    kill %1
}
trap cleanup EXIT

echo "Reading Flink config..."
source "$TEST_DIR"/test-config.sh
execution_target=`get_execution_target`

cd "$TEST_DIR/../bin"
./sql-gateway.sh -d "$TEST_DIR/data/test-config.yaml" > /tmp/flink-sql-gateway-test.out &
echo "Starting rest endpoint..."
while true
do
    line=`cat /tmp/flink-sql-gateway-test.out | tail -1`
    if [[ "$line"  == "Rest endpoint started." ]]
    then
        echo "Rest endpoint started."
        break
    fi
    sleep 1
done

info_response=`get_info`
echo "Product Name: `echo "$info_response" | jq -r ".product_name"`"
echo "Version: `echo "$info_response" | jq -r ".version"`"

function run_tests() {
    echo ""
    echo "################################################################################"
    echo "#                    Running tests in $1 mode"
    echo "################################################################################"
    echo ""

    session_id=`create_session "$1"`
    send_heartbeat "$session_id"

    run_test "$TEST_DIR"/cases/test-catalog-and-database.sh "$session_id" "$execution_target" "$data_dir"
    run_test "$TEST_DIR"/cases/test-table-and-view.sh "$session_id" "$execution_target" "$data_dir"
    run_test "$TEST_DIR"/cases/test-select-and-insert.sh "$session_id" "$execution_target" "$data_dir"
    run_test "$TEST_DIR"/cases/test-availability.sh "$session_id" "$execution_target" "$data_dir"

    delete_session "$session_id"
}

run_tests batch
run_tests streaming
