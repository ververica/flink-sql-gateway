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

package com.ververica.flink.table.gateway.operation;

import com.ververica.flink.table.gateway.SqlExecutionException;
import com.ververica.flink.table.gateway.SqlGatewayException;
import com.ververica.flink.table.gateway.context.ExecutionContext;
import com.ververica.flink.table.gateway.context.SessionContext;
import com.ververica.flink.table.gateway.rest.result.ColumnInfo;
import com.ververica.flink.table.gateway.rest.result.ResultSet;

import org.apache.flink.api.common.JobID;
import org.apache.flink.api.common.JobStatus;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.client.deployment.ClusterDescriptor;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.types.Row;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * A default implementation of JobOperation.
 */
public abstract class AbstractJobOperation implements JobOperation {
	private static final Logger LOG = LoggerFactory.getLogger(AbstractJobOperation.class);
	private static final int DEFAULT_TIMEOUT_SECONDS = 30;

	protected final SessionContext context;
	protected final String sessionId;
	protected volatile JobID jobId;

	private long currentToken;
	private int previousMaxFetchSize;
	private int previousResultSetSize;
	private LinkedList<Row> bufferedResults;
	@Nullable
	private LinkedList<Boolean> bufferedChangeFlags;
	private boolean noMoreResults;
	private volatile boolean isJobCanceled;

	protected final Object lock = new Object();

	public AbstractJobOperation(SessionContext context) {
		this.context = context;
		this.sessionId = context.getSessionId();
		this.currentToken = 0;
		this.previousMaxFetchSize = 0;
		this.previousResultSetSize = 0;
		this.bufferedResults = new LinkedList<>();
		this.bufferedChangeFlags = null;
		this.noMoreResults = false;
		this.isJobCanceled = false;
	}

	@Override
	public JobStatus getJobStatus() throws SqlGatewayException {
		synchronized (lock) {
			if (jobId == null) {
				LOG.error("Session: {}. No job has been submitted. This is a bug.", sessionId);
				throw new IllegalStateException("No job has been submitted. This is a bug.");
			}

			return bridgeClientRequest(context.getExecutionContext(), jobId, clusterClient -> {
				try {
					return clusterClient.getJobStatus(jobId).get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
				} catch (InterruptedException | ExecutionException | TimeoutException e) {
					LOG.error(String.format("Session: %s. Failed to fetch job status for job %s", sessionId, jobId), e);
					throw new SqlGatewayException("Failed to fetch job status for job " + jobId, e);
				}
			});
		}
	}

	@Override
	public void cancelJob() {
		if (isJobCanceled) {
			// just for fast failure
			return;
		}
		synchronized (lock) {
			if (jobId == null) {
				LOG.error("Session: {}. No job has been submitted. This is a bug.", sessionId);
				throw new IllegalStateException("No job has been submitted. This is a bug.");
			}
			if (isJobCanceled) {
				return;
			}

			cancelJobInternal();
			isJobCanceled = true;
		}
	}

	protected abstract void cancelJobInternal();

	protected String getJobName(String statement) {
		Optional<String> sessionName = context.getSessionName();
		if (sessionName.isPresent()) {
			return String.format("%s:%s:%s", sessionName.get(), sessionId, statement);
		} else {
			return String.format("%s:%s", sessionId, statement);
		}
	}

	@Override
	public JobID getJobId() {
		if (jobId == null) {
			throw new IllegalStateException("No job has been submitted. This is a bug.");
		}
		return jobId;
	}

	@Override
	public synchronized Optional<ResultSet> getJobResult(long token, int maxFetchSize) throws SqlGatewayException {
		if (token == currentToken) {
			if (noMoreResults) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Session: {}. There is no more result for job: {}", sessionId, jobId);
				}
				return Optional.empty();
			}

			// a new token arrives, remove used results
			for (int i = 0; i < previousResultSetSize; i++) {
				bufferedResults.removeFirst();
				if (bufferedChangeFlags != null) {
					bufferedChangeFlags.removeFirst();
				}
			}

			if (bufferedResults.isEmpty()) {
				// buffered results have been totally consumed,
				// so try to fetch new results
				Optional<Tuple2<List<Row>, List<Boolean>>> newResults = fetchNewJobResults();
				if (newResults.isPresent()) {
					bufferedResults.addAll(newResults.get().f0);
					if (newResults.get().f1 != null) {
						if (bufferedChangeFlags == null) {
							bufferedChangeFlags = new LinkedList<>();
						}
						bufferedChangeFlags.addAll(newResults.get().f1);
					}
					currentToken++;
				} else {
					noMoreResults = true;
					return Optional.empty();
				}
			} else {
				// buffered results haven't been totally consumed
				currentToken++;
			}

			previousMaxFetchSize = maxFetchSize;
			if (maxFetchSize > 0) {
				previousResultSetSize = Math.min(bufferedResults.size(), maxFetchSize);
			} else {
				previousResultSetSize = bufferedResults.size();
			}
			if (LOG.isDebugEnabled()) {
				LOG.debug(
					"Session: {}. Fetching current result for job: {}, token: {}, maxFetchSize: {}, realReturnSize: {}.",
					sessionId, jobId, token, maxFetchSize, previousResultSetSize);
			}
		} else if (token == currentToken - 1 && token >= 0) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Session: {}. Fetching previous result for job: {}, token: {}, maxFetchSize: ",
					sessionId, jobId, token, maxFetchSize);
			}
			if (previousMaxFetchSize != maxFetchSize) {
				String msg = String.format(
					"As the same token is provided, fetch size must be the same. Expecting max_fetch_size to be %s.",
					previousMaxFetchSize);
				if (LOG.isDebugEnabled()) {
					LOG.error(String.format("Session: %s. %s", sessionId, msg));
				}
				throw new SqlGatewayException(msg);
			}
		} else {
			String msg;
			if (currentToken == 0) {
				msg = "Expecting token to be 0, but found " + token + ".";
			} else {
				msg = "Expecting token to be " + currentToken + " or " + (currentToken - 1) + ", but found " + token + ".";
			}
			if (LOG.isDebugEnabled()) {
				LOG.error(String.format("Session: %s. %s", sessionId, msg));
			}
			throw new SqlGatewayException(msg);
		}

		return Optional.of(new ResultSet(
			getColumnInfos(),
			getLinkedListElementsFromBegin(bufferedResults, previousResultSetSize),
			getLinkedListElementsFromBegin(bufferedChangeFlags, previousResultSetSize)));
	}

	protected abstract Optional<Tuple2<List<Row>, List<Boolean>>> fetchNewJobResults() throws SqlGatewayException;

	protected abstract List<ColumnInfo> getColumnInfos();

	private <T> List<T> getLinkedListElementsFromBegin(LinkedList<T> linkedList, int size) {
		if (linkedList == null) {
			return null;
		}
		List<T> ret = new ArrayList<>();
		Iterator<T> iter = linkedList.iterator();
		for (int i = 0; i < size; i++) {
			ret.add(iter.next());
		}
		return ret;
	}

	/**
	 * The reason of using ClusterClient instead of JobClient to retrieve a cluster is
	 * the JobClient can't know whether the job is finished on yarn-per-job mode.
	 *
	 * <p>If a job is finished, JobClient always get java.util.concurrent.TimeoutException
	 * when getting job status and canceling a job after job is finished.
	 * This method will throw org.apache.hadoop.yarn.exceptions.ApplicationNotFoundException
	 * when creating a ClusterClient if the job is finished. This is more user-friendly.
	 */
	protected <C, R> R bridgeClientRequest(
		ExecutionContext<C> executionContext,
		JobID jobId,
		Function<ClusterClient<?>, R> function) {
		// stop Flink job
		try (final ClusterDescriptor<C> clusterDescriptor = executionContext.createClusterDescriptor()) {
			try (ClusterClient<C> clusterClient =
				     clusterDescriptor.retrieve(executionContext.getClusterId()).getClusterClient()) {
				// retrieve existing cluster
				return function.apply(clusterClient);
			} catch (Exception e) {
				LOG.error(
					String.format("Session: %s, job: %s. Could not retrieve or create a cluster.", sessionId, jobId),
					e);
				throw new SqlExecutionException("Could not retrieve or create a cluster.", e);
			}
		} catch (SqlExecutionException e) {
			throw e;
		} catch (Exception e) {
			LOG.error(
				String.format("Session: %s, job: %s. Could not locate a cluster.", sessionId, jobId), e);
			throw new SqlExecutionException("Could not locate a cluster.", e);
		}
	}
}
