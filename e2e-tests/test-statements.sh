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

SESSION_ID="$1"

function use_catalog() {
    run_ddl "$SESSION_ID" "USE CATALOG $1" "USE_CATALOG"
}

function use_database() {
    run_ddl "$SESSION_ID" "USE $1" "USE"
}

function create_database() {
    run_ddl "$SESSION_ID" "CREATE DATABASE $1" "CREATE_DATABASE"
}

function drop_database() {
    run_ddl "$SESSION_ID" "DROP DATABASE $1" "DROP_DATABASE"
}

function drop_database_if_exists() {
    run_ddl "$SESSION_ID" "DROP DATABASE IF EXISTS $1" "DROP_DATABASE"
}

function show_catalogs() {
    run_non_job_statement "$SESSION_ID" "SHOW CATALOGS" "SHOW_CATALOGS"
}

function show_current_catalog() {
    run_non_job_statement "$SESSION_ID" "SHOW CURRENT CATALOG" "SHOW_CURRENT_CATALOG"
}

function show_databases() {
    run_non_job_statement "$SESSION_ID" "SHOW DATABASES" "SHOW_DATABASES"
}

function show_current_database() {
    run_non_job_statement "$SESSION_ID" "SHOW CURRENT DATABASE" "SHOW_CURRENT_DATABASE"
}

function create_table() {
    run_ddl "$SESSION_ID" "CREATE TABLE $1" "CREATE_TABLE"
}

function alter_table() {
    run_ddl "$SESSION_ID" "ALTER TABLE $1" "ALTER_TABLE"
}

function drop_table() {
    run_ddl "$SESSION_ID" "DROP TABLE $1" "DROP_TABLE"
}

function drop_table_if_exists() {
    run_ddl "$SESSION_ID" "DROP TABLE IF EXISTS $1" "DROP_TABLE"
}

function create_view() {
    run_ddl "$SESSION_ID" "CREATE VIEW $1" "CREATE_VIEW"
}

function drop_view() {
    run_ddl "$SESSION_ID" "DROP VIEW $1" "DROP_VIEW"
}

function drop_view_if_exists() {
    run_ddl "$SESSION_ID" "DROP VIEW IF EXISTS $1" "DROP_VIEW"
}

function show_tables() {
    run_non_job_statement "$SESSION_ID" "SHOW TABLES" "SHOW_TABLES"
}

function show_views() {
    run_non_job_statement "$SESSION_ID" "SHOW VIEWS" "SHOW_VIEWS"
}

function select_from() {
    run_job_statement "$SESSION_ID" "SELECT $1" "SELECT" $2
}

function insert_into() {
    res=`run_job_statement "$SESSION_ID" "INSERT INTO $1" "INSERT_INTO" $2`
    assert_equals -2 "$res"
}

function show_functions() {
    run_non_job_statement "$SESSION_ID" "SHOW FUNCTIONS" "SHOW_FUNCTIONS"
}

function show_modules() {
    run_non_job_statement "$SESSION_ID" "SHOW MODULES" "SHOW_MODULES"
}

function describe() {
    run_non_job_statement "$SESSION_ID" "DESCRIBE $1" "DESCRIBE_TABLE"
}

function explain() {
    run_non_job_statement "$SESSION_ID" "EXPLAIN PLAN FOR $1" "EXPLAIN"
}
