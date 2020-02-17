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

import com.ververica.flink.table.gateway.rest.message.SessionCreateRequestBody;
import com.ververica.flink.table.gateway.rest.message.SessionCreateResponseBody;

import org.apache.flink.runtime.rest.HttpMethodWrapper;
import org.apache.flink.runtime.rest.messages.EmptyMessageParameters;
import org.apache.flink.runtime.rest.messages.MessageHeaders;

import org.apache.flink.shaded.netty4.io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Message headers for creating a session.
 */
public class SessionCreateHeaders
	implements MessageHeaders<SessionCreateRequestBody, SessionCreateResponseBody, EmptyMessageParameters> {

	private static final SessionCreateHeaders INSTANCE = new SessionCreateHeaders();

	public static final String URL = "/sessions";

	private SessionCreateHeaders() {
	}

	@Override
	public Class<SessionCreateResponseBody> getResponseClass() {
		return SessionCreateResponseBody.class;
	}

	@Override
	public HttpResponseStatus getResponseStatusCode() {
		return HttpResponseStatus.OK;
	}

	@Override
	public String getDescription() {
		return "Creates a new session with a specific planner and execution type. " +
			"Specific properties can be given for current session " +
			"which will override the default properties of gateway.";
	}

	@Override
	public Class<SessionCreateRequestBody> getRequestClass() {
		return SessionCreateRequestBody.class;
	}

	@Override
	public EmptyMessageParameters getUnresolvedMessageParameters() {
		return EmptyMessageParameters.getInstance();
	}

	@Override
	public HttpMethodWrapper getHttpMethod() {
		return HttpMethodWrapper.POST;
	}

	@Override
	public String getTargetRestEndpointURL() {
		return URL;
	}

	public static SessionCreateHeaders getInstance() {
		return INSTANCE;
	}
}
