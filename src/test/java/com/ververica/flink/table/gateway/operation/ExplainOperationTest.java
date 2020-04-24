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
import com.ververica.flink.table.gateway.rest.result.ResultKind;
import com.ververica.flink.table.gateway.rest.result.ResultSet;
import com.ververica.flink.table.gateway.utils.EnvironmentFileUtil;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link ExplainOperation}.
 */
public class ExplainOperationTest extends OperationTestBase {

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
	public void testDescribe() {
		ExplainOperation operation = new ExplainOperation(context, "select * from TableNumber1");
		ResultSet resultSet = operation.execute();

		String expectedExplain = "== Abstract Syntax Tree ==\n" +
			"LogicalProject(IntegerField1=[$0], StringField1=[$1])\n" +
			"  EnumerableTableScan(table=[[default_catalog, default_database, TableNumber1]])\n" +
			"\n" +
			"== Optimized Logical Plan ==\n" +
			"BatchTableSourceScan(table=[[default_catalog, default_database, TableNumber1]], fields=[IntegerField1, StringField1], source=[CsvTableSource(read fields: IntegerField1, StringField1)])\n" +
			"\n" +
			"== Physical Execution Plan ==\n" +
			"Stage 1 : Data Source\n" +
			"\tcontent : collect elements with CollectionInputFormat\n" +
			"\tPartitioning : RANDOM_PARTITIONED\n" +
			"\n" +
			"\tStage 0 : Data Sink\n" +
			"\t\tcontent : org.apache.flink.api.java.io.DiscardingOutputFormat\n" +
			"\t\tship_strategy : Forward\n" +
			"\t\texchange_mode : PIPELINED\n" +
			"\t\tPartitioning : RANDOM_PARTITIONED\n" +
			"\n";
		compareResult(expectedExplain, resultSet);
	}

	public static void compareResult(String expectedExplain, ResultSet resultSet) {
		assertEquals(resultSet.getResultKind(), ResultKind.SUCCESS_WITH_CONTENT);
		String actualExplain = resultSet.getData().get(0).getField(0).toString();
		assertEquals(
			replaceStageId(expectedExplain),
			replaceStageId(actualExplain));
	}

	private static String replaceStageId(String s) {
		return s.replaceAll("\\r\\n", "\n").replaceAll("Stage \\d+", "");
	}
}
