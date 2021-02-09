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

import com.ververica.flink.table.gateway.operation.SqlCommandParser.SqlCommand;
import com.ververica.flink.table.gateway.rest.message.SessionIdPathParameter;
import com.ververica.flink.table.gateway.rest.message.SessionMessageParameters;
import com.ververica.flink.table.gateway.rest.message.StatementExecuteRequestBody;
import com.ververica.flink.table.gateway.rest.message.StatementExecuteResponseBody;
import com.ververica.flink.table.gateway.rest.result.ResultSet;
import com.ververica.flink.table.gateway.rest.session.SessionManager;
import com.ververica.flink.table.gateway.utils.SqlGatewayException;

import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.runtime.rest.handler.HandlerRequest;
import org.apache.flink.runtime.rest.handler.RestHandlerException;
import org.apache.flink.runtime.rest.messages.MessageHeaders;

import org.apache.flink.shaded.netty4.io.netty.handler.codec.http.HttpResponseStatus;

import javax.annotation.Nonnull;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Request handler for executing a statement.
 */
public class StatementExecuteHandler
	extends AbstractRestHandler<StatementExecuteRequestBody, StatementExecuteResponseBody, SessionMessageParameters> {

	private final SessionManager sessionManager;

	public StatementExecuteHandler(
		SessionManager sessionManager,
		Time timeout,
		Map<String, String> responseHeaders,
		MessageHeaders<
			StatementExecuteRequestBody,
			StatementExecuteResponseBody,
			SessionMessageParameters> messageHeaders) {

		super(timeout, responseHeaders, messageHeaders);
		this.sessionManager = sessionManager;
	}

	@Override
	protected CompletableFuture<StatementExecuteResponseBody> handleRequest(
		@Nonnull HandlerRequest<StatementExecuteRequestBody, SessionMessageParameters> request)
		throws RestHandlerException {

		String sessionId = request.getPathParameter(SessionIdPathParameter.class);

		String statement = request.getRequestBody().getStatement();
		if (statement == null) {
			throw new RestHandlerException("Statement must be provided.", HttpResponseStatus.BAD_REQUEST);
		}

		try {
			Tuple2<ResultSet, SqlCommand> tuple2 = sessionManager.getSession(sessionId).runStatement(statement);
			ResultSet resultSet = tuple2.f0;
			String statementType = tuple2.f1.name();

			return CompletableFuture.completedFuture(
				new StatementExecuteResponseBody(
					Collections.singletonList(resultSet),
					Collections.singletonList(statementType))
			);
		} catch (SqlGatewayException e) {
			throw new RestHandlerException(e.getMessage(), HttpResponseStatus.INTERNAL_SERVER_ERROR, e);
		}
	}
}
