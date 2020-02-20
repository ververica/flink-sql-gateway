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
    use_catalog "test_catalog"
    drop_database_if_exists "test_cat_2nd_db"
    use_catalog "default_catalog"
    drop_database_if_exists "def_cat_2nd_db"
}
trap cleanup EXIT

catalog_rows=`show_catalogs`
assert_equals 2 "`echo "$catalog_rows" | grep -c "^"`"
assert_equals_after_sorting \
    "`make_array "default_catalog" "test_catalog"`" \
    "$catalog_rows"

use_catalog "default_catalog"
current_cat=`show_current_catalog`
assert_equals "default_catalog" "$current_cat"
current_db=`show_current_database`
assert_equals "default_database" "$current_db"

create_database "def_cat_2nd_db"
database_rows=`show_databases`
assert_equals 2 "`echo "$database_rows" | grep -c "^"`"
assert_equals_after_sorting \
    "`make_array "default_database" "def_cat_2nd_db"`" \
    "$database_rows"

use_database "def_cat_2nd_db"
current_db=`show_current_database`
assert_equals "def_cat_2nd_db" "$current_db"

use_catalog "test_catalog"
current_cat=`show_current_catalog`
assert_equals "test_catalog" "$current_cat"

create_database "test_cat_2nd_db"
database_rows=`show_databases`
assert_equals 2 "`echo "$database_rows" | grep -c "^"`"
assert_equals_after_sorting \
    "`make_array "test_cat_def_db" "test_cat_2nd_db"`" \
    "$database_rows"

use_database "test_cat_2nd_db"
current_db=`show_current_database`
assert_equals "test_cat_2nd_db" "$current_db"

drop_database "test_cat_2nd_db"
database_rows=`show_databases`
assert_equals 1 "`echo "$database_rows" | grep -c "^"`"
assert_equals_after_sorting \
    "`make_array "test_cat_def_db"`" \
    "$database_rows"

use_catalog "default_catalog"
drop_database "def_cat_2nd_db"
database_rows=`show_databases`
assert_equals 1 "`echo "$database_rows" | grep -c "^"`"
assert_equals_after_sorting \
    "`make_array "default_database"`" \
    "$database_rows"
