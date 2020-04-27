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
data_dir="$3"

function cleanup() {
    use_database "tmp_db"
    drop_view_if_exists "region_view"
    drop_view_if_exists "nation_view"
    drop_table_if_exists "region"
    drop_table_if_exists "region_new"
    drop_table_if_exists "nation"
    drop_database_if_exists "tmp_db"
}
trap cleanup EXIT

use_catalog "default_catalog"

region_table=$(echo `cat <<EOF
region (
  r_regionkey bigint not null,
  r_name varchar not null,
  r_comment varchar not null
) WITH (
  'connector.type'='filesystem',
  'connector.path'='${data_dir}/nation.tbl',
  'format.type' = 'csv',
  'format.derive-schema' = 'true',
  'format.field-delimiter' = '|'
)
EOF`
)
nation_table=$(echo `cat <<EOF
nation (
  n_nationkey bigint  not null,
  n_name varchar  not null,
  n_regionkey bigint  not null,
  n_comment varchar  not null
) WITH (
  'connector.type'='filesystem',
  'connector.path'='${data_dir}/nation.tbl',
  'format.type' = 'csv',
  'format.derive-schema' = 'true',
  'format.field-delimiter' = '|'
)
EOF`
)

create_database "tmp_db"
use_database "tmp_db"

create_table "$region_table"
create_table "$nation_table"
table_rows=`show_tables`
assert_equals 2 "`echo "$table_rows" | grep -c "^"`"
assert_equals_after_sorting \
    "`make_array "region" "nation"`" \
    "$table_rows"

alter_table "region RENAME TO region_new"
table_rows=`show_tables`
assert_equals 2 "`echo "$table_rows" | grep -c "^"`"
assert_equals_after_sorting \
    "`make_array "region_new" "nation"`" \
    "$table_rows"
alter_table "region_new RENAME TO region"

create_view "region_view AS SELECT * FROM region"
create_view "nation_view AS SELECT * FROM nation"
table_rows=`show_tables`
assert_equals 4 "`echo "$table_rows" | grep -c "^"`"
assert_equals_after_sorting \
    "`make_array "region" "nation" "region_view" "nation_view"`" \
    "$table_rows"
view_rows=`show_views`
assert_equals 2 "`echo "$view_rows" | grep -c "^"`"
assert_equals_after_sorting \
    "`make_array "region_view" "nation_view"`" \
    "$view_rows"

drop_view "nation_view"
drop_table "nation"
table_rows=`show_tables`
assert_equals 2 "`echo "$table_rows" | grep -c "^"`"
assert_equals_after_sorting \
    "`make_array "region" "region_view"`" \
    "$table_rows"
view_rows=`show_views`
assert_equals 1 "`echo "$view_rows" | grep -c "^"`"
assert_equals_after_sorting \
    "`make_array "region_view"`" \
    "$view_rows"
