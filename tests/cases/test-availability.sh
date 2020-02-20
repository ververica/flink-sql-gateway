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

set -e

TEST_DIR=$(cd $(dirname $BASH_SOURCE)/..; pwd)
source "$TEST_DIR"/test-statements.sh "$1"

function cleanup() {
    use_database "tmp_db"
    drop_table_if_exists "nation"
    drop_database_if_exists "tmp_db"
}
trap cleanup EXIT

use_catalog "default_catalog"
create_database "tmp_db"
use_database "tmp_db"

nation_table=$(echo `cat <<EOF
nation (
  n_nationkey bigint  not null,
  n_name varchar  not null,
  n_regionkey bigint  not null,
  n_comment varchar  not null
) WITH (
  'connector.type'='filesystem',
  'connector.path'='hdfs://$HDFS_ADDRESS/tmp/flink-sql-gateway-test/nation.tbl',
  'format.type' = 'csv',
  'format.derive-schema' = 'true',
  'format.field-delimiter' = '|'
)
EOF`
)

create_table "$nation_table"

show_functions > /dev/null
show_modules > /dev/null
describe "nation" > /dev/null
explain "SELECT * FROM nation" > /dev/null
