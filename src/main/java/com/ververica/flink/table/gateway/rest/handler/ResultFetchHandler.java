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

import com.ververica.flink.table.gateway.Session;
import com.ververica.flink.table.gateway.SessionManager;
import com.ververica.flink.table.gateway.SqlGatewayException;
import com.ververica.flink.table.gateway.rest.message.JobIdPathParameter;
import com.ververica.flink.table.gateway.rest.message.ResultFetchMessageParameters;
import com.ververica.flink.table.gateway.rest.message.ResultFetchRequestBody;
import com.ververica.flink.table.gateway.rest.message.ResultFetchResponseBody;
import com.ververica.flink.table.gateway.rest.message.ResultTokenPathParameter;
import com.ververica.flink.table.gateway.rest.message.SessionIdPathParameter;
import com.ververica.flink.table.gateway.rest.result.ResultSet;

import org.apache.flink.api.common.JobID;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.runtime.rest.handler.HandlerRequest;
import org.apache.flink.runtime.rest.handler.RestHandlerException;
import org.apache.flink.runtime.rest.messages.MessageHeaders;
import org.apache.flink.runtime.rest.versioning.RestAPIVersion;

import org.apache.flink.shaded.netty4.io.netty.handler.codec.http.HttpResponseStatus;

import javax.annotation.Nonnull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Request handler for fetching job result.
 */
public class ResultFetchHandler
	extends AbstractRestHandler<ResultFetchRequestBody, ResultFetchResponseBody, ResultFetchMessageParameters> {

	private final SessionManager sessionManager;

	public ResultFetchHandler(
		SessionManager sessionManager,
		Time timeout,
		Map<String, String> responseHeaders,
		MessageHeaders<ResultFetchRequestBody, ResultFetchResponseBody, ResultFetchMessageParameters> messageHeaders) {

		super(timeout, responseHeaders, messageHeaders);
		this.sessionManager = sessionManager;
	}

	@Override
	protected CompletableFuture<ResultFetchResponseBody> handleRequest(
		@Nonnull HandlerRequest<ResultFetchRequestBody, ResultFetchMessageParameters> request)
		throws RestHandlerException {

		String sessionId = request.getPathParameter(SessionIdPathParameter.class);
		JobID jobId = request.getPathParameter(JobIdPathParameter.class);
		Long resultToken = request.getPathParameter(ResultTokenPathParameter.class);
		Integer maxFetchSize = request.getRequestBody().getMaxFetchSize();

		if (maxFetchSize != null && maxFetchSize <= 0) {
			throw new RestHandlerException("Max fetch size must be positive.", HttpResponseStatus.BAD_REQUEST);
		}
		maxFetchSize = maxFetchSize == null ? 0 : maxFetchSize;

		try {
			Session session = sessionManager.getSession(sessionId);
			Optional<ResultSet> resultSet = session.getJobResult(jobId, resultToken, maxFetchSize);
			List<ResultSet> results = null;
			if (resultSet.isPresent()) {
				results = Collections.singletonList(resultSet.get());
			}

			RestAPIVersion version = getCurrentVersion();
			String nextResultUri = null;
			if (resultSet.isPresent()) {
				nextResultUri = ResultFetchHeaders.buildNextResultUri(version, sessionId, jobId, resultToken + 1);
			}

			return CompletableFuture.completedFuture(new ResultFetchResponseBody(results, nextResultUri));
		} catch (SqlGatewayException e) {
			throw new RestHandlerException(e.getMessage(), HttpResponseStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
