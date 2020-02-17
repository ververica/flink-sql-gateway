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

import com.ververica.flink.table.gateway.rest.message.SessionIdPathParameter;
import com.ververica.flink.table.gateway.rest.message.SessionMessageParameters;
import com.ververica.flink.table.gateway.rest.message.StatementExecuteRequestBody;
import com.ververica.flink.table.gateway.rest.message.StatementExecuteResponseBody;

import org.apache.flink.runtime.rest.HttpMethodWrapper;
import org.apache.flink.runtime.rest.messages.MessageHeaders;

import org.apache.flink.shaded.netty4.io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Message headers for executing a statement.
 */
public class StatementExecuteHeaders
	implements MessageHeaders<StatementExecuteRequestBody, StatementExecuteResponseBody, SessionMessageParameters> {

	private static final StatementExecuteHeaders INSTANCE = new StatementExecuteHeaders();

	public static final String URL = "/sessions/:" + SessionIdPathParameter.KEY + "/statements";

	private StatementExecuteHeaders() {
	}

	@Override
	public Class<StatementExecuteResponseBody> getResponseClass() {
		return StatementExecuteResponseBody.class;
	}

	@Override
	public HttpResponseStatus getResponseStatusCode() {
		return HttpResponseStatus.OK;
	}

	@Override
	public String getDescription() {
		return "Runs the provided statement and returns the result. " +
			"We currently only support one single statement per API call.";
	}

	@Override
	public Class<StatementExecuteRequestBody> getRequestClass() {
		return StatementExecuteRequestBody.class;
	}

	@Override
	public SessionMessageParameters getUnresolvedMessageParameters() {
		return new SessionMessageParameters();
	}

	@Override
	public HttpMethodWrapper getHttpMethod() {
		return HttpMethodWrapper.POST;
	}

	@Override
	public String getTargetRestEndpointURL() {
		return URL;
	}

	public static StatementExecuteHeaders getInstance() {
		return INSTANCE;
	}
}
