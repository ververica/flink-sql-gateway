#!/usr/bin/env bash
################################################################################
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
# limitations under the License.
################################################################################

FLINK_SQL_GATEWAY_LIB="$TEST_DIR"/../lib

# build flink-sql-gateway classpath
FLINK_SQL_GATEWAY_CLASSPATH=""
while read -d '' -r jarfile ; do
    if [[ "$FLINK_SQL_GATEWAY_CLASSPATH" == "" ]]; then
        FLINK_SQL_GATEWAY_CLASSPATH="$jarfile";
    else
        FLINK_SQL_GATEWAY_CLASSPATH="$FLINK_SQL_GATEWAY_CLASSPATH":"$jarfile"
    fi
done < <(find "$FLINK_SQL_GATEWAY_LIB" ! -type d -name '*.jar' -print0 | sort -z)

cd "$FLINK_HOME"/bin
# get flink config
. ./config.sh

if [[ "$FLINK_IDENT_STRING" = "" ]]; then
        FLINK_IDENT_STRING="$USER"
fi

CC_CLASSPATH=`constructFlinkClassPath`
FULL_CLASSPATH="`manglePathList "$CC_CLASSPATH:$INTERNAL_HADOOP_CLASSPATHS:$FLINK_SQL_GATEWAY_CLASSPATH"`"

function get_execution_target() {
    # read execution target from config
    execution_target_output=`${JAVA_RUN} -classpath ${FULL_CLASSPATH} com.ververica.flink.table.gateway.utils.BashJavaUtil "GET_EXECUTION_TARGET" "$@" --defaults ""$TEST_DIR"/data/test-config.yaml" 2> /dev/null | tail -n 1000`
    if [[ $? -ne 0 ]]; then
        echo "[ERROR] Cannot run BashJavaUtil to execute command GET_EXECUTION_TARGET." > /dev/stderr
        exit 1
    fi
    echo "$execution_target_output"
}
