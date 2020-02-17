/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ververica.flink.table.gateway.rest.message;

import com.ververica.flink.table.gateway.rest.result.ResultSet;

import org.apache.flink.runtime.rest.messages.ResponseBody;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;

import java.util.List;

/**
 * {@link ResponseBody} for fetching job result.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultFetchResponseBody implements ResponseBody {

	private static final String FIELD_NAME_RESULTS = "results";
	private static final String FIELD_NAME_NEXT_RESULT_URI = "next_result_uri";

	@JsonProperty(FIELD_NAME_RESULTS)
	@Nullable
	private List<ResultSet> results;

	@JsonProperty(FIELD_NAME_NEXT_RESULT_URI)
	@Nullable
	private String nextResultUri;

	public ResultFetchResponseBody(
		@Nullable @JsonProperty(FIELD_NAME_RESULTS) List<ResultSet> results,
		@Nullable @JsonProperty(FIELD_NAME_NEXT_RESULT_URI) String nextResultUri) {
		this.results = results;
		this.nextResultUri = nextResultUri;
	}

	@Nullable
	public List<ResultSet> getResults() {
		return results;
	}

	@Nullable
	public String getNextResultUri() {
		return nextResultUri;
	}

}
