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

package com.ververica.flink.table.gateway.rest;

import com.ververica.flink.table.gateway.SessionManager;
import com.ververica.flink.table.gateway.rest.handler.GetInfoHandler;
import com.ververica.flink.table.gateway.rest.handler.GetInfoHeaders;
import com.ververica.flink.table.gateway.rest.handler.JobCancelHandler;
import com.ververica.flink.table.gateway.rest.handler.JobCancelHeaders;
import com.ververica.flink.table.gateway.rest.handler.JobStatusHandler;
import com.ververica.flink.table.gateway.rest.handler.JobStatusHeaders;
import com.ververica.flink.table.gateway.rest.handler.ResultFetchHandler;
import com.ververica.flink.table.gateway.rest.handler.ResultFetchHeaders;
import com.ververica.flink.table.gateway.rest.handler.SessionCloseHandler;
import com.ververica.flink.table.gateway.rest.handler.SessionCloseHeaders;
import com.ververica.flink.table.gateway.rest.handler.SessionCreateHandler;
import com.ververica.flink.table.gateway.rest.handler.SessionCreateHeaders;
import com.ververica.flink.table.gateway.rest.handler.SessionHeartbeatHandler;
import com.ververica.flink.table.gateway.rest.handler.SessionHeartbeatHeaders;
import com.ververica.flink.table.gateway.rest.handler.StatementExecuteHandler;
import com.ververica.flink.table.gateway.rest.handler.StatementExecuteHeaders;

import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.runtime.rest.RestServerEndpoint;
import org.apache.flink.runtime.rest.RestServerEndpointConfiguration;
import org.apache.flink.runtime.rest.handler.RestHandlerSpecification;

import org.apache.flink.shaded.netty4.io.netty.channel.ChannelInboundHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A RestServerEndpoint for sql gateway.
 */
public class SqlGatewayEndpoint extends RestServerEndpoint {

	private final SessionManager sessionManager;

	public SqlGatewayEndpoint(
		RestServerEndpointConfiguration configuration,
		SessionManager sessionManager) throws IOException {
		super(configuration);
		this.sessionManager = sessionManager;
	}

	@Override
	protected List<Tuple2<RestHandlerSpecification, ChannelInboundHandler>> initializeHandlers(
		CompletableFuture<String> localAddressFuture) {
		Time timeout = Time.seconds(1);

		final SessionCreateHandler sessionCreateHandler = new SessionCreateHandler(
			sessionManager, timeout, responseHeaders, SessionCreateHeaders.getInstance());

		final SessionCloseHandler sessionCloseHandler = new SessionCloseHandler(
			sessionManager, timeout, responseHeaders, SessionCloseHeaders.getInstance());

		final SessionHeartbeatHandler sessionHeartbeatHandler = new SessionHeartbeatHandler(
			sessionManager, timeout, responseHeaders, SessionHeartbeatHeaders.getInstance());

		final StatementExecuteHandler statementExecuteHandler = new StatementExecuteHandler(
			sessionManager, timeout, responseHeaders, StatementExecuteHeaders.getInstance());

		final JobStatusHandler jobStatusHandler = new JobStatusHandler(
			sessionManager, timeout, responseHeaders, JobStatusHeaders.getInstance());

		final JobCancelHandler jobCancelHandler = new JobCancelHandler(
			sessionManager, timeout, responseHeaders, JobCancelHeaders.getInstance());

		final ResultFetchHandler resultFetchHandler = new ResultFetchHandler(
			sessionManager, timeout, responseHeaders, ResultFetchHeaders.getInstance());

		final GetInfoHandler getInfoHandler = new GetInfoHandler(
			timeout, responseHeaders, GetInfoHeaders.getInstance());

		ArrayList<Tuple2<RestHandlerSpecification, ChannelInboundHandler>> handlers = new ArrayList<>(30);
		handlers.add(Tuple2.of(SessionCreateHeaders.getInstance(), sessionCreateHandler));
		handlers.add(Tuple2.of(SessionCloseHeaders.getInstance(), sessionCloseHandler));
		handlers.add(Tuple2.of(SessionHeartbeatHeaders.getInstance(), sessionHeartbeatHandler));
		handlers.add(Tuple2.of(StatementExecuteHeaders.getInstance(), statementExecuteHandler));
		handlers.add(Tuple2.of(JobStatusHeaders.getInstance(), jobStatusHandler));
		handlers.add(Tuple2.of(JobCancelHeaders.getInstance(), jobCancelHandler));
		handlers.add(Tuple2.of(ResultFetchHeaders.getInstance(), resultFetchHandler));
		handlers.add(Tuple2.of(GetInfoHeaders.getInstance(), getInfoHandler));

		return handlers;
	}

	@Override
	protected void startInternal() throws Exception {
		sessionManager.open();
	}
}
