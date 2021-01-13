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

package com.ververica.flink.table.gateway.result;

import com.ververica.flink.table.gateway.sink.CollectBatchTableSink;
import com.ververica.flink.table.gateway.utils.SqlExecutionException;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.common.accumulators.SerializedListAccumulator;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.core.execution.JobClient;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.sinks.TableSink;
import org.apache.flink.types.Row;
import org.apache.flink.util.AbstractID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * Collects results using accumulators and returns them as table snapshots.
 */
public class BatchResult<C> extends AbstractResult<C, Row> {

	private final String accumulatorName;
	private final CollectBatchTableSink tableSink;
	private final Object resultLock;
	private final ClassLoader classLoader;

	private AtomicReference<SqlExecutionException> executionException = new AtomicReference<>();
	private List<Row> resultTable;

	private boolean allResultRetrieved = false;

	public BatchResult(
			TableSchema tableSchema,
			RowTypeInfo outputType,
			ExecutionConfig config,
			ClassLoader classLoader) {
		// TODO supports large result set
		accumulatorName = new AbstractID().toString();
		tableSink = new CollectBatchTableSink(accumulatorName, outputType.createSerializer(config), tableSchema);
		resultLock = new Object();
		this.classLoader = checkNotNull(classLoader);
	}

	@Override
	public void startRetrieval(JobClient jobClient) {
		CompletableFuture.completedFuture(jobClient)
			.thenCompose(client -> client.getJobExecutionResult())
			.thenAccept(new ResultRetrievalHandler())
			.whenComplete((unused, throwable) -> {
				if (throwable != null) {
					executionException.compareAndSet(
						null,
						new SqlExecutionException("Error while submitting job.", throwable));
				}
			});
	}

	@Override
	public TypedResult<List<Row>> retrieveChanges() {
		synchronized (resultLock) {
			// the job finished with an exception
			SqlExecutionException e = executionException.get();
			if (e != null) {
				throw e;
			}

			// wait for a result
			if (null == resultTable) {
				return TypedResult.empty();
			}

			if (allResultRetrieved) {
				return TypedResult.endOfStream();
			} else {
				allResultRetrieved = true;
				return TypedResult.payload(resultTable);
			}
		}
	}

	@Override
	public TableSink<?> getTableSink() {
		return tableSink;
	}

	@Override
	public void close() {
	}

	// --------------------------------------------------------------------------------------------

	private class ResultRetrievalHandler implements Consumer<JobExecutionResult> {

		@Override
		public void accept(JobExecutionResult jobExecutionResult) {
			try {
				final ArrayList<byte[]> accResult = jobExecutionResult.getAccumulatorResult(accumulatorName);
				if (accResult == null) {
					throw new SqlExecutionException("The accumulator could not retrieve the result.");
				}
				final List<Row> resultTable = SerializedListAccumulator
					.deserializeList(accResult, tableSink.getSerializer());
				// sets the result table all at once
				synchronized (resultLock) {
					BatchResult.this.resultTable = resultTable;
				}
			} catch (ClassNotFoundException | IOException e) {
				throw new SqlExecutionException("Serialization error while deserializing collected data.", e);
			}
		}
	}
}
