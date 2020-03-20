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

package com.ververica.flink.table.gateway;

import com.ververica.flink.table.gateway.config.Environment;
import com.ververica.flink.table.gateway.config.entries.ExecutionEntry;
import com.ververica.flink.table.gateway.context.DefaultContext;
import com.ververica.flink.table.gateway.context.SessionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Session manager.
 */
public class SessionManager {
	private static final Logger LOG = LoggerFactory.getLogger(SessionManager.class);

	private final DefaultContext defaultContext;

	private final long idleTimeout;
	private final long checkInterval;
	private final long maxCount;

	private final Map<String, Session> sessions;

	private ScheduledExecutorService executorService;
	private ScheduledFuture timeoutCheckerFuture;

	public SessionManager(DefaultContext defaultContext) {
		this.defaultContext = defaultContext;
		Environment env = defaultContext.getDefaultEnv();
		this.idleTimeout = env.getSession().getIdleTimeout();
		this.checkInterval = env.getSession().getCheckInterval();
		this.maxCount = env.getSession().getMaxCount();
		this.sessions = new ConcurrentHashMap<>();
	}

	public void open() {
		if (checkInterval > 0 && idleTimeout > 0) {
			executorService = Executors.newSingleThreadScheduledExecutor();
			timeoutCheckerFuture = executorService.scheduleAtFixedRate(() -> {
				LOG.info("Start to remove expired session, current session count: {}", sessions.size());
				for (Map.Entry<String, Session> entry : sessions.entrySet()) {
					String sessionId = entry.getKey();
					Session session = entry.getValue();
					if (isSessionExpired(session)) {
						LOG.info("Session: {} is expired, close it...", sessionId);
						closeSession(session);
					}
				}
				LOG.info("Remove expired session finished, current session count: {}", sessions.size());
			}, checkInterval, checkInterval, TimeUnit.MILLISECONDS);
		}
	}

	public void close() {
		LOG.info("Start to close SessionManager");
		if (executorService != null) {
			timeoutCheckerFuture.cancel(true);
			executorService.shutdown();
		}
		LOG.info("SessionManager is closed");
	}

	public String createSession(
		String sessionName,
		String planner,
		String executionType,
		Map<String, String> properties) throws SqlGatewayException {
		checkSessionCount();

		Map<String, String> newProperties = new HashMap<>(properties);
		newProperties.put(Environment.EXECUTION_ENTRY + "." + ExecutionEntry.EXECUTION_PLANNER, planner);
		newProperties.put(Environment.EXECUTION_ENTRY + "." + ExecutionEntry.EXECUTION_TYPE, executionType);

		if (executionType.equalsIgnoreCase(ExecutionEntry.EXECUTION_TYPE_VALUE_BATCH)) {
			// for batch mode we ensure that results are provided in materialized form
			newProperties.put(
				Environment.EXECUTION_ENTRY + "." + ExecutionEntry.EXECUTION_RESULT_MODE,
				ExecutionEntry.EXECUTION_RESULT_MODE_VALUE_TABLE);
		} else {
			// for streaming mode we ensure that results are provided in changelog form
			newProperties.put(
				Environment.EXECUTION_ENTRY + "." + ExecutionEntry.EXECUTION_RESULT_MODE,
				ExecutionEntry.EXECUTION_RESULT_MODE_VALUE_CHANGELOG);
		}

		Environment sessionEnv = Environment.enrich(
			defaultContext.getDefaultEnv(), newProperties, Collections.emptyMap());

		String sessionId = SessionID.generate().toHexString();
		SessionContext sessionContext = new SessionContext(sessionName, sessionId, sessionEnv, defaultContext);

		Session session = new Session(sessionContext);
		sessions.put(sessionId, session);

		LOG.info("Session: {} is created. sessionName: {}, planner: {}, executionType: {}, properties: {}.",
			sessionId, sessionName, planner, executionType, properties);

		return sessionId;
	}

	public void closeSession(String sessionId) throws SqlGatewayException {
		Session session = getSession(sessionId);
		closeSession(session);
	}

	private void closeSession(Session session) {
		String sessionId = session.getContext().getSessionId();
		sessions.remove(sessionId);
		LOG.info("Session: {} is closed.", sessionId);
	}

	public Session getSession(String sessionId) throws SqlGatewayException {
		// TODO lock sessions to prevent fetching an expired session?
		Session session = sessions.get(sessionId);
		if (session == null) {
			String msg = String.format("Session: %s does not exist.", sessionId);
			LOG.error(msg);
			throw new SqlGatewayException(msg);
		}
		session.touch();
		return session;
	}

	private void checkSessionCount() throws SqlGatewayException {
		if (maxCount <= 0) {
			return;
		}
		if (sessions.size() > maxCount) {
			String msg = String.format(
				"Failed to create session, the count of active sessions exceeds the max count: %s", maxCount);
			LOG.error(msg);
			throw new SqlGatewayException(msg);
		}
	}

	private boolean isSessionExpired(Session session) {
		if (idleTimeout > 0) {
			return (System.currentTimeMillis() - session.getLastVisitedTime()) > idleTimeout;
		} else {
			return false;
		}
	}

}
