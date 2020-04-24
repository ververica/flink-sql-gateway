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

import com.ververica.flink.table.gateway.rest.result.ColumnInfo;
import com.ververica.flink.table.gateway.rest.result.ConstantNames;
import com.ververica.flink.table.gateway.rest.result.ResultKind;
import com.ververica.flink.table.gateway.rest.result.ResultSet;
import com.ververica.flink.table.gateway.rest.result.TableSchemaUtil;

import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.types.logical.VarCharType;
import org.apache.flink.types.Row;

import static org.junit.Assert.assertEquals;

/**
 * Utilities for running operation tests.
 */
public class OperationTestUtils {

	public static void compareDescribeResult(TableSchema tableSchema, ResultSet resultSet) throws Exception {
		String schemaJson = TableSchemaUtil.writeTableSchemaToJson(tableSchema);

		ResultSet expected = ResultSet.builder()
			.resultKind(ResultKind.SUCCESS_WITH_CONTENT)
			.columns(ColumnInfo.create(ConstantNames.SCHEMA, new VarCharType(false, schemaJson.length())))
			.data(Row.of(schemaJson))
			.build();

		assertEquals(expected, resultSet);
	}

	public static void compareExplainResult(String expectedExplain, ResultSet resultSet) {
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
