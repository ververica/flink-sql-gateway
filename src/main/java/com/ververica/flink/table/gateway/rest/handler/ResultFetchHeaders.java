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

package com.ververica.flink.table.gateway.rest.handler;

import com.ververica.flink.table.gateway.rest.message.JobIdPathParameter;
import com.ververica.flink.table.gateway.rest.message.ResultFetchMessageParameters;
import com.ververica.flink.table.gateway.rest.message.ResultFetchRequestBody;
import com.ververica.flink.table.gateway.rest.message.ResultFetchResponseBody;
import com.ververica.flink.table.gateway.rest.message.ResultTokenPathParameter;
import com.ververica.flink.table.gateway.rest.message.SessionIdPathParameter;

import org.apache.flink.api.common.JobID;
import org.apache.flink.runtime.rest.HttpMethodWrapper;
import org.apache.flink.runtime.rest.messages.MessageHeaders;
import org.apache.flink.runtime.rest.versioning.RestAPIVersion;

import org.apache.flink.shaded.netty4.io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Message headers for fetching job result.
 */
public class ResultFetchHeaders
	implements MessageHeaders<ResultFetchRequestBody, ResultFetchResponseBody, ResultFetchMessageParameters> {

	private static final ResultFetchHeaders INSTANCE = new ResultFetchHeaders();

	public static final String URL = "/sessions/:" + SessionIdPathParameter.KEY +
		"/jobs/:" + JobIdPathParameter.KEY + "/result" + "/:" + ResultTokenPathParameter.KEY;

	private ResultFetchHeaders() {
	}

	@Override
	public Class<ResultFetchResponseBody> getResponseClass() {
		return ResultFetchResponseBody.class;
	}

	@Override
	public HttpResponseStatus getResponseStatusCode() {
		return HttpResponseStatus.OK;
	}

	@Override
	public String getDescription() {
		return "Fetch a part of result for a submitted job. " +
			"To fetch the next part of the result, increase token by 1 and call this API again " +
			"or just follow the uri provided in the next_result_uri." +
			"If the token is the same with the previous call to this API, " +
			"then the same part of the result will be provided again. " +
			"Other token values are considered invalid. " +
			"If it's the first time to fetch the result for a job, the token value must be 0.";
	}

	@Override
	public Class<ResultFetchRequestBody> getRequestClass() {
		return ResultFetchRequestBody.class;
	}

	@Override
	public ResultFetchMessageParameters getUnresolvedMessageParameters() {
		return new ResultFetchMessageParameters();
	}

	@Override
	public HttpMethodWrapper getHttpMethod() {
		return HttpMethodWrapper.GET;
	}

	@Override
	public String getTargetRestEndpointURL() {
		return URL;
	}

	public static ResultFetchHeaders getInstance() {
		return INSTANCE;
	}

	public static String buildNextResultUri(RestAPIVersion version, String sessionId, JobID jobId) {
		return buildNextResultUri(version, sessionId, jobId, 0);
	}

	public static String buildNextResultUri(RestAPIVersion version, String sessionId, JobID jobId, long token) {
		return "/" + version.getURLVersionPrefix() + URL
			.replace(":" + SessionIdPathParameter.KEY, sessionId)
			.replace(":" + JobIdPathParameter.KEY, jobId.toHexString())
			.replace(":" + ResultTokenPathParameter.KEY, String.valueOf(token));
	}
}
