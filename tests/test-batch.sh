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

if [[ -z "$HDFS_ADDRESS" ]]
then
    echo "Please define \$HDFS_LOCATION first" >> /dev/stderr
    exit 1
fi

echo "Preparing test data..."
hdfs dfs -rm -r hdfs://"$HDFS_ADDRESS"/tmp/flink-sql-gateway-test/
hdfs dfs -mkdir -p hdfs://"$HDFS_ADDRESS"/tmp/flink-sql-gateway-test/
hdfs dfs -copyFromLocal "$TEST_DIR/data/nation.tbl" hdfs://"$HDFS_ADDRESS"/tmp/flink-sql-gateway-test/

function cleanup() {
    kill %1
}
trap cleanup EXIT

source "$TEST_DIR"/test-config.sh

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
echo "$info_response"
echo "Product Name: `echo "$info_response" | jq -r ".product_name"`"
echo "Version: `echo "$info_response" | jq -r ".version"`"

session_id=`create_session "batch"`
send_heartbeat "$session_id"

execution_target=`echo "$flink_conf_output" | grep "execution.target" | cut -d',' -f2`

run_test "$TEST_DIR"/cases/test-catalog-and-database.sh "$session_id" "$execution_target"
# VIEW is buggy
# run_test "$TEST_DIR"/cases/test-table-and-view.sh "$session_id" "$execution_target"
run_test "$TEST_DIR"/cases/test-select-and-insert.sh "$session_id" "$execution_target"
run_test "$TEST_DIR"/cases/test-availability.sh "$session_id" "$execution_target"
