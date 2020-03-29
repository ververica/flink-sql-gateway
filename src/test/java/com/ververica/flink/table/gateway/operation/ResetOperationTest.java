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
import com.ververica.flink.table.gateway.rest.result.ColumnInfo;
import com.ververica.flink.table.gateway.rest.result.ConstantNames;
import com.ververica.flink.table.gateway.rest.result.ResultSet;
import com.ververica.flink.table.gateway.utils.EnvironmentFileUtil;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.types.logical.VarCharType;
import org.apache.flink.types.Row;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link ResetOperation}.
 */
public class ResetOperationTest extends OperationTestBase {

	private static final String DEFAULTS_ENVIRONMENT_FILE = "test-sql-gateway-defaults.yaml";

	@Override
	protected Environment getSessionEnvironment() throws Exception {
		final Map<String, String> replaceVars = new HashMap<>();
		replaceVars.put("$VAR_PLANNER", "old");
		replaceVars.put("$VAR_EXECUTION_TYPE", "batch");
		replaceVars.put("$VAR_UPDATE_MODE", "");
		return EnvironmentFileUtil.parseModified(DEFAULTS_ENVIRONMENT_FILE, replaceVars);
	}

	@Test
	public void testReset() {
		SetOperation showSetOperation1 = new SetOperation(context);
		ResultSet resultSet = showSetOperation1.execute();
		List<Row> properties = Arrays.asList(
			Row.of("execution.max-parallelism", "16"),
			Row.of("execution.planner", "old"),
			Row.of("execution.parallelism", "1"),
			Row.of("execution.type", "batch"),
			Row.of("deployment.response-timeout", "5000"),
			Row.of("table.optimizer.join-reorder-enabled", "false"));

		Configuration conf = new Configuration();
		conf.setString("table.optimizer.join-reorder-enabled", "false");

		ResultSet expected1 = new ResultSet(
			Arrays.asList(
				ColumnInfo.create(ConstantNames.KEY, new VarCharType(true, 36)),
				ColumnInfo.create(ConstantNames.VALUE, new VarCharType(true, 5))),
			properties);
		assertEquals(expected1, resultSet);
		assertEquals(conf, context.getExecutionContext().getTableEnvironment().getConfig().getConfiguration());

		SetOperation setOperation1 = new SetOperation(context, "table.optimizer.join-reorder-enabled", "true");
		assertEquals(OperationUtil.AFFECTED_ROW_COUNT0, setOperation1.execute());
		SetOperation setOperation2 = new SetOperation(context, "table.optimizer.join.broadcast-threshold", "TWO_PHASE");
		assertEquals(OperationUtil.AFFECTED_ROW_COUNT0, setOperation2.execute());
		Configuration newConf = new Configuration(conf);
		newConf.setString("table.optimizer.join-reorder-enabled", "true");
		newConf.setString("table.optimizer.join.broadcast-threshold", "TWO_PHASE");
		assertEquals(newConf, context.getExecutionContext().getTableEnvironment().getConfig().getConfiguration());

		ResetOperation resetOperation = new ResetOperation(context);
		assertEquals(OperationUtil.AFFECTED_ROW_COUNT0, resetOperation.execute());
		assertEquals(conf, context.getExecutionContext().getTableEnvironment().getConfig().getConfiguration());

		SetOperation showSetOperation2 = new SetOperation(context);
		ResultSet resultSet2 = showSetOperation2.execute();
		ResultSet expected2 = new ResultSet(
			Arrays.asList(
				ColumnInfo.create(ConstantNames.KEY, new VarCharType(true, 36)),
				ColumnInfo.create(ConstantNames.VALUE, new VarCharType(true, 5))),
			properties);
		assertEquals(expected2, resultSet2);
	}
}
