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

HOST=localhost
PORT=8083
API_VERSION=v1

function run_test() {
    echo "==================== Running Test: `basename "$1"` ===================="
    bash "$1" "${@:2}"
    if [[ "$?" -eq 0 ]]
    then
        echo -e "\033[0;32mPASSED\033[0m"
    else
        echo -e "\033[0;31mFAILED\033[0m"
    fi
}

function make_array() {
    res=""
    for x in "$@"
    do
        res="$res""$x""\n"
    done
    echo -e "$res"
}

function make_array_from_json() {
    len_outer=$(echo "$1" | jq ". | length")
    if [[ "$len_outer" -eq 0 ]]
    then
        echo ""
        return
    fi

    res=""
    for i in $(seq 0 $(($len_outer - 1)))
    do
        row=$(echo "$1" | jq -r ".[$i]")
        len_inner=$(echo "$row" | jq ". | length")
        if [[ "$len_inner" -eq 0 ]]
        then
            continue
        fi

        line=""
        for j in $(seq 0 $(($len_inner - 1)))
        do
            if [[ "$j" -ne 0 ]]
            then
                line="$line "
            fi
            line="$line""$(echo "$row" | jq -r ".[$j]")"
        done

        res="$res""$line""\n"
    done

    echo -e "$res"
}

function assert_equals() {
    if [[ "$1" != "$2" ]]
    then
        echo "Expecting $1 but found $2" >> /dev/stderr
        echo "Caller information: `caller`" >> /dev/stderr
        exit 1
    fi
}

function assert_equals_after_sorting() {
    expected=`echo "$1" | sort`
    actual=`echo "$2" | sort`
    if [[ "$expected" != "$actual" ]]
    then
        echo "Expecting $expected but found $actual" >> /dev/stderr
        echo "Caller information: `caller`" >> /dev/stderr
        exit 1
    fi
}

function send_request() {
    response=$(echo `curl -s -H "Content-type: application/json" -X "$1" -d "$2" "$3"`)
    error=`echo "$response" | jq ".errors"`
    if [[ "$error" != "null" ]]
    then
        echo "Request to $3 returns an error." >> /dev/stderr
        echo "Request Method: $1" >> /dev/stderr
        echo "Request Body: $2" >> /dev/stderr
        echo "$error" | jq -r ".[0]" >> /dev/stderr
        echo "$error" | jq -r ".[1]" >> /dev/stderr
        exit 1
    fi
    echo "$response"
}

function get_json_element() {
    echo "$1" | jq -r "$2"
}

function get_info() {
    send_request "GET" "" "http://$HOST:$PORT/$API_VERSION/info"
}

function create_session() {
    json=`cat <<EOF
{
    "planner": "blink",
    "execution_type": "$1"
}
EOF`
    response=`send_request "POST" "$json" "http://$HOST:$PORT/$API_VERSION/sessions"`
    if [[ "$?" -ne 0 ]]
    then
        exit 1
    fi

    session_id=`get_json_element "$response" ".session_id"`
    if [[ "$session_id" == "null" ]]
    then
        echo "Failed to create batch session" >> /dev/stderr
        exit 1
    fi
    echo "$session_id"
}

function send_heartbeat() {
    send_request "POST" "" "http://$HOST:$PORT/$API_VERSION/sessions/$1/heartbeat" > /dev/null
}

function run_non_job_statement() {
    json=`cat <<EOF
{
    "statement": "$2"
}
EOF`
    response=`send_request "POST" "$json" "http://$HOST:$PORT/$API_VERSION/sessions/$1/statements"`
    if [[ "$?" -ne 0 ]]
    then
        exit 1
    fi

    type=`get_json_element "$response" ".statement_types[0]"`
    assert_equals "$3" "$type"

    make_array_from_json "`get_json_element "$response" ".results[0].data"`"
}

function get_job_status() {
    response=`send_request "GET" '' "http://$HOST:$PORT/$API_VERSION/sessions/$1/jobs/$2/status"`
    if [[ "$?" -ne 0 ]]
    then
        exit 1
    fi
    get_json_element "$response" ".status"
}

function run_job_statement() {
    json=`cat <<EOF
{
    "statement": "$2"
}
EOF`
    submit_response=`send_request "POST" "$json" "http://$HOST:$PORT/$API_VERSION/sessions/$1/statements"`
    if [[ "$?" -ne 0 ]]
    then
        exit 1
    fi
    job_id=`get_json_element "$submit_response" ".results[0].data[0][0]"`
    res=""
    token=0
    while true
    do
        result_response=`send_request "GET" '{"max_fetch_size": 3}' "http://$HOST:$PORT/$API_VERSION/sessions/$1/jobs/$job_id/result/$token"`
        if [[ "$?" -ne 0 ]]
        then
            exit 1
        fi
        next_result_uri=`get_json_element "$result_response" ".next_result_uri"`
        if [[ "$next_result_uri" == "null" ]]
        then
            break
        fi
        partial_res="$(make_array_from_json "$(get_json_element "$result_response" ".results[0].data")")"
        if [[ "$partial_res" != "" ]]
        then
            res="$res""$partial_res""\n"
        fi
        token=$(($token + 1))
    done

    if [[ "$2" = "1" ]]
    then
        job_status=`get_job_status "$1" "$job_id"`
        assert_equals "FINISHED" "$job_status"
    fi

    echo -e "$res"
}

function run_ddl() {
    res=`run_non_job_statement "$1" "$2" "$3"`
    assert_equals 0 "$res"
}
