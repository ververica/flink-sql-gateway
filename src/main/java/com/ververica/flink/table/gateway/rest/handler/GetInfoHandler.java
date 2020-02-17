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

import com.ververica.flink.table.gateway.rest.message.GetInfoResponseBody;

import org.apache.flink.api.common.time.Time;
import org.apache.flink.runtime.rest.handler.HandlerRequest;
import org.apache.flink.runtime.rest.handler.RestHandlerException;
import org.apache.flink.runtime.rest.messages.EmptyMessageParameters;
import org.apache.flink.runtime.rest.messages.EmptyRequestBody;
import org.apache.flink.runtime.rest.messages.MessageHeaders;
import org.apache.flink.runtime.util.EnvironmentInformation;

import javax.annotation.Nonnull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Request handler for getting info.
 */
public class GetInfoHandler
	extends AbstractRestHandler<EmptyRequestBody, GetInfoResponseBody, EmptyMessageParameters> {

	public GetInfoHandler(
		Time timeout,
		Map<String, String> responseHeaders,
		MessageHeaders<EmptyRequestBody, GetInfoResponseBody, EmptyMessageParameters> messageHeaders) {

		super(timeout, responseHeaders, messageHeaders);
	}

	@Override
	protected CompletableFuture<GetInfoResponseBody> handleRequest(
		@Nonnull HandlerRequest<EmptyRequestBody, EmptyMessageParameters> request) throws RestHandlerException {
		String version = EnvironmentInformation.getVersion();
		return CompletableFuture.completedFuture(new GetInfoResponseBody("Apache Flink", version));
	}

}
