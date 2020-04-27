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

import com.ververica.flink.table.gateway.context.ExecutionContext;
import com.ververica.flink.table.gateway.context.SessionContext;
import com.ververica.flink.table.gateway.rest.result.ColumnInfo;
import com.ververica.flink.table.gateway.rest.result.ConstantNames;
import com.ververica.flink.table.gateway.rest.result.ResultKind;
import com.ververica.flink.table.gateway.rest.result.ResultSet;

import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.types.logical.VarCharType;
import org.apache.flink.types.Row;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Operation for SHOW TABLE command.
 */
public class ShowTableOperation implements NonJobOperation {
	private final ExecutionContext<?> context;

	public ShowTableOperation(SessionContext context) {
		this.context = context.getExecutionContext();
	}

	@Override
	public ResultSet execute() {
		List<Row> rows = new ArrayList<>();
		int maxNameLength = 1;

		final TableEnvironment tableEnv = context.getTableEnvironment();
		// listTables will return all tables and views
		for (String table : context.wrapClassLoader(() -> Arrays.asList(tableEnv.listTables()))) {
			rows.add(Row.of(table));
			maxNameLength = Math.max(maxNameLength, table.length());
		}

		return ResultSet.builder()
			.resultKind(ResultKind.SUCCESS_WITH_CONTENT)
			.columns(ColumnInfo.create(ConstantNames.TABLES, new VarCharType(false, maxNameLength)))
			.data(rows)
			.build();
	}
}
