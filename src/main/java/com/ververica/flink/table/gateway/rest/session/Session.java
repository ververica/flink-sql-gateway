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

package com.ververica.flink.table.gateway.rest.session;

import com.ververica.flink.table.gateway.config.entries.ExecutionEntry;
import com.ververica.flink.table.gateway.context.SessionContext;
import com.ververica.flink.table.gateway.operation.JobOperation;
import com.ververica.flink.table.gateway.operation.Operation;
import com.ververica.flink.table.gateway.operation.OperationFactory;
import com.ververica.flink.table.gateway.operation.SqlCommandParser;
import com.ververica.flink.table.gateway.operation.SqlCommandParser.SqlCommandCall;
import com.ververica.flink.table.gateway.operation.SqlParseException;
import com.ververica.flink.table.gateway.rest.result.ResultSet;
import com.ververica.flink.table.gateway.utils.SqlGatewayException;

import org.apache.flink.api.common.JobID;
import org.apache.flink.api.common.JobStatus;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.table.planner.plan.metadata.FlinkDefaultRelMetadataProvider;

import org.apache.calcite.rel.metadata.JaninoRelMetadataProvider;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Similar to HTTP Session, which could maintain user identity and store user-specific data
 * during multiple request/response interactions between a client and the gateway server.
 */
public class Session {
	private static final Logger LOG = LoggerFactory.getLogger(Session.class);

	private final SessionContext context;
	private final String sessionId;

	private long lastVisitedTime;

	private final Map<JobID, JobOperation> jobOperations;

	public Session(SessionContext context) {
		this.context = context;
		this.sessionId = context.getSessionId();

		this.lastVisitedTime = System.currentTimeMillis();

		this.jobOperations = new ConcurrentHashMap<>();
	}

	public void touch() {
		lastVisitedTime = System.currentTimeMillis();
	}

	public long getLastVisitedTime() {
		return lastVisitedTime;
	}

	public SessionContext getContext() {
		return context;
	}

	public Tuple2<ResultSet, SqlCommandParser.SqlCommand> runStatement(String statement) {
		// TODO: This is a temporary fix to avoid NPE.
		//  In SQL gateway, TableEnvironment is created and used by different threads, thus causing this problem.
		RelMetadataQuery.THREAD_PROVIDERS
			.set(JaninoRelMetadataProvider.of(FlinkDefaultRelMetadataProvider.INSTANCE()));

		LOG.info("Session: {}, run statement: {}", sessionId, statement);
		boolean isBlinkPlanner = context.getExecutionContext().getEnvironment().getExecution().getPlanner()
			.equalsIgnoreCase(ExecutionEntry.EXECUTION_PLANNER_VALUE_BLINK);

		SqlCommandCall call;
		try {
			Optional<SqlCommandCall> callOpt = SqlCommandParser.parse(statement, isBlinkPlanner);
			if (!callOpt.isPresent()) {
				LOG.error("Session: {}, Unknown statement: {}", sessionId, statement);
				throw new SqlGatewayException("Unknown statement: " + statement);
			} else {
				call = callOpt.get();
			}
		} catch (SqlParseException e) {
			LOG.error("Session: {}, Failed to parse statement: {}", sessionId, statement);
			throw new SqlGatewayException(e.getMessage(), e.getCause());
		}

		Operation operation = OperationFactory.createOperation(call, context);
		ResultSet resultSet = operation.execute();

		if (operation instanceof JobOperation) {
			JobOperation jobOperation = (JobOperation) operation;
			jobOperations.put(jobOperation.getJobId(), jobOperation);
		}

		return Tuple2.of(resultSet, call.command);
	}

	public JobStatus getJobStatus(JobID jobId) {
		LOG.info("Session: {}, get status for job: {}", sessionId, jobId);
		return getJobOperation(jobId).getJobStatus();
	}

	public void cancelJob(JobID jobId) {
		LOG.info("Session: {}, cancel job: {}", sessionId, jobId);
		getJobOperation(jobId).cancelJob();
		jobOperations.remove(jobId);
	}

	public Optional<ResultSet> getJobResult(JobID jobId, long token, int maxFetchSize) {
		LOG.info("Session: {}, get result for job: {}, token: {}, maxFetchSize: {}",
			sessionId, jobId, token, maxFetchSize);
		return getJobOperation(jobId).getJobResult(token, maxFetchSize);
	}

	private JobOperation getJobOperation(JobID jobId) {
		JobOperation jobOperation = jobOperations.get(jobId);
		if (jobOperation == null) {
			String msg = String.format("Job: %s does not exist in current session: %s.", jobId, sessionId);
			LOG.error(msg);
			throw new SqlGatewayException(msg);
		} else {
			return jobOperation;
		}
	}

}
