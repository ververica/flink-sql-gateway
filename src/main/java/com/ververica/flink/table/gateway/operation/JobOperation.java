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

import com.ververica.flink.table.gateway.rest.result.ResultSet;

import org.apache.flink.api.common.JobID;
import org.apache.flink.api.common.JobStatus;

import java.util.Optional;

/**
 * An operation will submit a flink job, the corresponding command can be SELECT or INSERT.
 */
public interface JobOperation extends Operation {

	/**
	 * Returns the job id after submit the job, that means execute method has been called.
	 */
	JobID getJobId();

	/**
	 * Get a part of job execution result, this method may be called many times to get all result.
	 * Returns Optional.empty if no more results need to be returned,
	 * else returns ResultSet (even if the data in the ResultSet is empty).
	 *
	 * <p>The token must be equal to the previous token + 1 or must be same with the previous token.
	 * otherwise a SqlGatewayException will be thrown. This method should return the same result for the same token.
	 *
	 * <p>The size of result data must be less than maxFetchSize if it's is positive value, else ignore it.
	 * Note: the maxFetchSize must be same for same token in each call, else a SqlGatewayException will be thrown.
	 */
	Optional<ResultSet> getJobResult(long token, int maxFetchSize);

	/**
	 * Returns the status of the flink job.
	 */
	JobStatus getJobStatus();

	/**
	 * Cancel the flink job.
	 */
	void cancelJob();
}
