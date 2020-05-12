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

import com.ververica.flink.table.gateway.config.entries.ExecutionEntry;
import com.ververica.flink.table.gateway.rest.message.SessionCreateRequestBody;
import com.ververica.flink.table.gateway.rest.message.SessionCreateResponseBody;
import com.ververica.flink.table.gateway.rest.session.SessionManager;
import com.ververica.flink.table.gateway.utils.SqlGatewayException;

import org.apache.flink.api.common.time.Time;
import org.apache.flink.runtime.rest.handler.HandlerRequest;
import org.apache.flink.runtime.rest.handler.RestHandlerException;
import org.apache.flink.runtime.rest.messages.EmptyMessageParameters;
import org.apache.flink.runtime.rest.messages.MessageHeaders;

import org.apache.flink.shaded.netty4.io.netty.handler.codec.http.HttpResponseStatus;

import javax.annotation.Nonnull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Request handler for creating a session.
 */
public class SessionCreateHandler
	extends AbstractRestHandler<SessionCreateRequestBody, SessionCreateResponseBody, EmptyMessageParameters> {

	private static final List<String> AVAILABLE_PLANNERS = Arrays.asList(
		ExecutionEntry.EXECUTION_PLANNER_VALUE_OLD,
		ExecutionEntry.EXECUTION_PLANNER_VALUE_BLINK);

	private static final List<String> AVAILABLE_EXECUTION_TYPES = Arrays.asList(
		ExecutionEntry.EXECUTION_TYPE_VALUE_BATCH,
		ExecutionEntry.EXECUTION_TYPE_VALUE_STREAMING);

	private SessionManager sessionManager;

	public SessionCreateHandler(
		SessionManager sessionManager,
		Time timeout,
		Map<String, String> responseHeaders,
		MessageHeaders<SessionCreateRequestBody, SessionCreateResponseBody, EmptyMessageParameters> messageHeaders) {

		super(timeout, responseHeaders, messageHeaders);
		this.sessionManager = sessionManager;
	}

	@Override
	protected CompletableFuture<SessionCreateResponseBody> handleRequest(
		@Nonnull HandlerRequest<SessionCreateRequestBody, EmptyMessageParameters> request) throws RestHandlerException {

		String sessionName = request.getRequestBody().getSessionName();

		String planner = request.getRequestBody().getPlanner();
		if (planner == null) {
			throw new RestHandlerException("Planner must be provided.", HttpResponseStatus.BAD_REQUEST);
		} else if (!AVAILABLE_PLANNERS.contains(planner.toLowerCase())) {
			throw new RestHandlerException(
				"Planner must be one of these: " + String.join(", ", AVAILABLE_PLANNERS),
				HttpResponseStatus.BAD_REQUEST);
		}

		String executionType = request.getRequestBody().getExecutionType();
		if (executionType == null) {
			throw new RestHandlerException("Execution type must be provided.", HttpResponseStatus.BAD_REQUEST);
		} else if (!AVAILABLE_EXECUTION_TYPES.contains(executionType.toLowerCase())) {
			throw new RestHandlerException(
				"Execution type must be one of these: " + String.join(", ", AVAILABLE_EXECUTION_TYPES),
				HttpResponseStatus.BAD_REQUEST);
		}

		Map<String, String> properties = request.getRequestBody().getProperties();
		if (properties == null) {
			properties = Collections.emptyMap();
		}

		String sessionId;
		try {
			sessionId = sessionManager.createSession(sessionName, planner, executionType, properties);
		} catch (SqlGatewayException e) {
			throw new RestHandlerException(e.getMessage(), HttpResponseStatus.INTERNAL_SERVER_ERROR, e);
		}

		return CompletableFuture.completedFuture(new SessionCreateResponseBody(sessionId));
	}
}
