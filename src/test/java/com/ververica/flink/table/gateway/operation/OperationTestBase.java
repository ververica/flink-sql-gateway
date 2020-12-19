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

import com.ververica.flink.table.gateway.config.Environment;
import com.ververica.flink.table.gateway.config.entries.ExecutionEntry;
import com.ververica.flink.table.gateway.context.DefaultContext;
import com.ververica.flink.table.gateway.context.SessionContext;
import com.ververica.flink.table.gateway.rest.session.SessionID;
import org.apache.flink.client.cli.DefaultCLI;
import org.apache.flink.client.deployment.DefaultClusterClientServiceLoader;
import org.apache.flink.configuration.Configuration;
import org.junit.Before;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Operation test base.
 */
public class OperationTestBase {

	protected SessionContext context;

	@Before
	public void setup() throws Exception {
		context = new SessionContext(
			"test-session",
			SessionID.generate().toString(),
			getSessionEnvironment(),
			getDefaultContext());
	}

	protected DefaultContext getDefaultContext() {
		return new DefaultContext(
			new Environment(),
			Collections.emptyList(),
			new Configuration(),
			new DefaultCLI(),
			new DefaultClusterClientServiceLoader());
	}

	protected Environment getSessionEnvironment() throws Exception {
		Map<String, String> newProperties = new HashMap<>();
		newProperties.put(Environment.EXECUTION_ENTRY + "." + ExecutionEntry.EXECUTION_PLANNER, "blink");
		newProperties.put(Environment.EXECUTION_ENTRY + "." + ExecutionEntry.EXECUTION_TYPE, "batch");
		return Environment.enrich(new Environment(), newProperties, Collections.emptyMap());
	}

}
