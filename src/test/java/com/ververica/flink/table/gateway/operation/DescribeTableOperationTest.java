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
import com.ververica.flink.table.gateway.rest.result.ResultKind;
import com.ververica.flink.table.gateway.rest.result.ResultSet;
import com.ververica.flink.table.gateway.utils.EnvironmentFileUtil;

import org.apache.flink.table.types.logical.BooleanType;
import org.apache.flink.table.types.logical.VarCharType;
import org.apache.flink.types.Row;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@link DescribeTableOperation}.
 */
public class DescribeTableOperationTest extends OperationTestBase {

	private static final String DEFAULTS_ENVIRONMENT_FILE = "test-sql-gateway-defaults.yaml";

	@Override
	protected Environment getSessionEnvironment() throws Exception {
		final Map<String, String> replaceVars = new HashMap<>();
		replaceVars.put("$VAR_PLANNER", "blink");
		replaceVars.put("$VAR_EXECUTION_TYPE", "streaming");
		replaceVars.put("$VAR_UPDATE_MODE", "");
		return EnvironmentFileUtil.parseModified(DEFAULTS_ENVIRONMENT_FILE, replaceVars);
	}

	@Test
	public void testDescribe() {
		String ddl = "CREATE TABLE T1(\n" +
			"  f0 char(10),\n" +
			"  f1 varchar(10),\n" +
			"  f2 string,\n" +
			"  f3 BOOLEAN,\n" +
			"  f4 BINARY(10),\n" +
			"  f5 VARBINARY(10),\n" +
			"  f6 BYTES,\n" +
			"  f7 DECIMAL(10, 3),\n" +
			"  f8 TINYINT,\n" +
			"  f9 SMALLINT,\n" +
			"  f10 INTEGER,\n" +
			"  f11 BIGINT,\n" +
			"  f12 FLOAT,\n" +
			"  f13 DOUBLE,\n" +
			"  f14 DATE,\n" +
			"  f15 TIME,\n" +
			"  f16 TIMESTAMP,\n" +
			"  f17 TIMESTAMP(3),\n" +
			"  f18 TIMESTAMP WITHOUT TIME ZONE,\n" +
			"  f19 TIMESTAMP(3) WITH LOCAL TIME ZONE,\n" +
			"  f20 TIMESTAMP WITH LOCAL TIME ZONE,\n" +
			"  f21 ARRAY<INT>,\n" +
			"  f22 MAP<INT, STRING>,\n" +
			"  f23 ROW<f0 INT, f1 STRING>,\n" +
			"  f24 int not null,\n" +
			"  f25 varchar not null,\n" +
			"  f26 row<f0 int not null, f1 int> not null,\n" +
			"  ts AS to_timestamp(f25),\n" +
			"  WATERMARK FOR ts AS ts - INTERVAL '1' SECOND\n" +
			") WITH (\n" +
			"  'connector.type' = 'random'\n" +
			")";
		DDLOperation ddlOperation = new DDLOperation(context, ddl, SqlCommandParser.SqlCommand.CREATE_TABLE);
		ddlOperation.execute();

		DescribeTableOperation operation = new DescribeTableOperation(context, "T1");
		ResultSet resultSet = operation.execute();

		List<Row> expectedData = Arrays.asList(
			Row.of("f0", "CHAR(10)", true, null, null, null),
			Row.of("f1", "VARCHAR(10)", true, null, null, null),
			Row.of("f2", "STRING", true, null, null, null),
			Row.of("f3", "BOOLEAN", true, null, null, null),
			Row.of("f4", "BINARY(10)", true, null, null, null),
			Row.of("f5", "VARBINARY(10)", true, null, null, null),
			Row.of("f6", "BYTES", true, null, null, null),
			Row.of("f7", "DECIMAL(10, 3)", true, null, null, null),
			Row.of("f8", "TINYINT", true, null, null, null),
			Row.of("f9", "SMALLINT", true, null, null, null),
			Row.of("f10", "INT", true, null, null, null),
			Row.of("f11", "BIGINT", true, null, null, null),
			Row.of("f12", "FLOAT", true, null, null, null),
			Row.of("f13", "DOUBLE", true, null, null, null),
			Row.of("f14", "DATE", true, null, null, null),
			Row.of("f15", "TIME(0)", true, null, null, null),
			Row.of("f16", "TIMESTAMP(6)", true, null, null, null),
			Row.of("f17", "TIMESTAMP(3)", true, null, null, null),
			Row.of("f18", "TIMESTAMP(6)", true, null, null, null),
			Row.of("f19", "TIMESTAMP(3) WITH LOCAL TIME ZONE", true, null, null, null),
			Row.of("f20", "TIMESTAMP(6) WITH LOCAL TIME ZONE", true, null, null, null),
			Row.of("f21", "ARRAY<INT>", true, null, null, null),
			Row.of("f22", "MAP<INT, STRING>", true, null, null, null),
			Row.of("f23", "ROW<`f0` INT, `f1` STRING>", true, null, null, null),
			Row.of("f24", "INT", false, null, null, null),
			Row.of("f25", "STRING", false, null, null, null),
			Row.of("f26", "ROW<`f0` INT NOT NULL, `f1` INT>", false, null, null, null),
			Row.of("ts", "TIMESTAMP(3)", true, null, "TO_TIMESTAMP(`f25`)", "`ts` - INTERVAL '1' SECOND"));
		ResultSet expected = ResultSet.builder()
			.resultKind(ResultKind.SUCCESS_WITH_CONTENT)
			.columns(
				ColumnInfo.create(ConstantNames.DESCRIBE_NAME, new VarCharType(false, 3)),
				ColumnInfo.create(ConstantNames.DESCRIBE_TYPE, new VarCharType(false, 33)),
				ColumnInfo.create(ConstantNames.DESCRIBE_NULL, new BooleanType(false)),
				ColumnInfo.create(ConstantNames.DESCRIBE_KEY, new VarCharType(true, 1)),
				ColumnInfo.create(ConstantNames.DESCRIBE_COMPUTED_COLUMN, new VarCharType(true, 19)),
				ColumnInfo.create(ConstantNames.DESCRIBE_WATERMARK, new VarCharType(true, 26)))
			.data(expectedData)
			.build();

		Assert.assertEquals(expected, resultSet);
	}
}
