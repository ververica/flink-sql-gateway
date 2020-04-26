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
import com.ververica.flink.table.gateway.context.ExecutionContext;
import com.ververica.flink.table.gateway.context.SessionContext;
import com.ververica.flink.table.gateway.rest.result.ColumnInfo;
import com.ververica.flink.table.gateway.rest.result.ConstantNames;
import com.ververica.flink.table.gateway.rest.result.ResultKind;
import com.ververica.flink.table.gateway.rest.result.ResultSet;
import com.ververica.flink.table.gateway.rest.result.TableSchemaUtil;

import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.types.logical.VarCharType;
import org.apache.flink.types.Row;

import java.util.ArrayList;
import java.util.List;

/**
 * Operation for DESCRIBE_TABLE command.
 */
public class DescribeTableOperation implements NonJobOperation {
	private final ExecutionContext<?> context;
	private final String tableName;

	public DescribeTableOperation(SessionContext context, String tableName) {
		this.context = context.getExecutionContext();
		this.tableName = tableName;
	}

	@Override
	public ResultSet execute() {
		final TableEnvironment tableEnv = context.getTableEnvironment();
		try {
			TableSchema schema = context.wrapClassLoader(() -> tableEnv.scan(tableName).getSchema());
			String schemaJson = TableSchemaUtil.writeTableSchemaToJson(schema);
			int length = schemaJson.length();
			List<Row> data = new ArrayList<>();
			data.add(Row.of(schemaJson));

			return ResultSet.builder()
				.resultKind(ResultKind.SUCCESS_WITH_CONTENT)
				.columns(ColumnInfo.create(ConstantNames.SCHEMA, new VarCharType(false, length)))
				.data(data)
				.build();
		} catch (Throwable t) {
			// catch everything such that the query does not crash the executor
			throw new SqlExecutionException("No table with this name could be found.", t);
		}
	}
}
