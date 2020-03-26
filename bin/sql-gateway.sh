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

################################################################################
# Adopted from "flink" bash script
################################################################################


if [ -z "$FLINK_HOME" ]; then
   (>&2  echo "FLINK_HOME is not found in environment variable.")
   (>&2  echo "Configures the FLINK_HOME environment variable using the following command: export FLINK_HOME=<flink-install-dir>")

    # exit to force process failure
    exit 1
fi

if [ ! -d "$FLINK_HOME" ]; then
    (>&2 echo "$FLINK_HOME does not exist.")

    # exit to force process failure
    exit 1
fi

target="$0"
# For the case, the executable has been directly symlinked, figure out
# the correct bin path by following its symlink up to an upper bound.
# Note: we can't use the readlink utility here if we want to be POSIX
# compatible.
iteration=0
while [ -L "$target" ]; do
    if [ "$iteration" -gt 100 ]; then
        echo "Cannot resolve path: You have a cyclic symlink in $target."
        break
    fi
    ls=`ls -ld -- "$target"`
    target=`expr "$ls" : '.* -> \(.*\)$'`
    iteration=$((iteration + 1))
done

# Convert relative path to absolute path
bin=`dirname "$target"`
FLINK_SQL_GATEWAY_HOME=`cd "$bin/.."; pwd -P`

FLINK_SQL_GATEWAY_CONF="$FLINK_SQL_GATEWAY_HOME"/conf
FLINK_SQL_GATEWAY_LIB="$FLINK_SQL_GATEWAY_HOME"/lib
FLINK_SQL_GATEWAY_LOG="$FLINK_SQL_GATEWAY_HOME"/log

FLINK_SQL_GATEWAY_DEFAULT_CONF="$FLINK_SQL_GATEWAY_CONF"/sql-gateway-defaults.yaml

FLINK_SQL_GATEWAY_JAR=$(find "$FLINK_SQL_GATEWAY_LIB" -regex ".*flink-sql-gateway.*.jar")

# build flink-sql-gateway classpath
FLINK_SQL_GATEWAY_CLASSPATH=""
while read -d '' -r jarfile ; do
    if [[ "$FLINK_SQL_GATEWAY_CLASSPATH" == "" ]]; then
        FLINK_SQL_GATEWAY_CLASSPATH="$jarfile";
    else
        FLINK_SQL_GATEWAY_CLASSPATH="$FLINK_SQL_GATEWAY_CLASSPATH":"$jarfile"
    fi
done < <(find "$FLINK_SQL_GATEWAY_LIB" ! -type d -name '*.jar' -print0 | sort -z)

# build flink class path
cd "$FLINK_HOME"/bin
# get flink config
. ./config.sh

if [ "$FLINK_IDENT_STRING" = "" ]; then
        FLINK_IDENT_STRING="$USER"
fi

CC_CLASSPATH=`constructFlinkClassPath`
FULL_CLASSPATH="`manglePathList "$CC_CLASSPATH:$INTERNAL_HADOOP_CLASSPATHS:$FLINK_SQL_GATEWAY_CLASSPATH"`"


# build log config
log=$FLINK_SQL_GATEWAY_LOG/flink-sql-gateway-$FLINK_IDENT_STRING-$HOSTNAME.log
log_setting=(-Dlog.file="$log" -Dlog4j.configuration=file:"$FLINK_SQL_GATEWAY_CONF"/log4j.properties -Dlogback.configurationFile=file:"$FLINK_SQL_GATEWAY_CONF"/logback.xml)

# read jvm args from config
#
jvm_args_output=`${JAVA_RUN} "${log_setting[@]}" -classpath ${FULL_CLASSPATH} com.ververica.flink.table.gateway.utils.BashJavaUtil "GET_SERVER_JVM_ARGS" "$@" --defaults "$FLINK_SQL_GATEWAY_DEFAULT_CONF" 2>&1 | tail -n 1000`
if [[ $? -ne 0 ]]; then
    echo "[ERROR] Cannot run BashJavaUtil to execute command GET_SERVER_JVM_ARGS." 1>&2
    # Print the output in case the user redirect the log to console.
    echo "$jvm_args_output" 1>&2
    exit 1
fi
JVM_ARGS=`extractExecutionParams "$jvm_args_output"`


if [ -n "$FLINK_SQL_GATEWAY_JAR" ]; then

    # start gateway with jar
    exec $JAVA_RUN $JVM_ARGS "${log_setting[@]}" -classpath ${FULL_CLASSPATH} com.ververica.flink.table.gateway.SqlGateway "$@" --defaults "$FLINK_SQL_GATEWAY_DEFAULT_CONF" --jar "`manglePath $FLINK_SQL_GATEWAY_JAR`"

# write error message to stderr
else
    (>&2 echo "[ERROR] Flink SQL Gateway JAR file 'flink-sql-gateway*.jar' should be located in $FLINK_SQL_GATEWAY_LIB.")

    # exit to force process failure
    exit 1
fi
