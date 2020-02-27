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

import com.ververica.flink.table.gateway.SqlCommandParser.SqlCommand;
import com.ververica.flink.table.gateway.SqlExecutionException;
import com.ververica.flink.table.gateway.context.ExecutionContext;
import com.ververica.flink.table.gateway.context.SessionContext;
import com.ververica.flink.table.gateway.rest.result.ResultSet;

import org.apache.flink.table.api.StreamQueryConfig;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.java.StreamTableEnvironment;

/**
 * Operation for CREATE/DROP/ALTER TABLE/DATABASE command.
 */
public class DDLOperation implements NonJobOperation {
	private final ExecutionContext<?> context;
	private final String ddl;
	private final SqlCommand command;

	public DDLOperation(SessionContext context, String ddl, SqlCommand command) {
		this.context = context.getExecutionContext();
		this.ddl = ddl;
		this.command = command;
	}

	@Override
	public ResultSet execute() {
		final TableEnvironment tEnv = context.getTableEnvironment();
		// parse and validate statement
		try {
			context.wrapClassLoader(() -> {
				if (tEnv instanceof StreamTableEnvironment) {
					((StreamTableEnvironment) tEnv).sqlUpdate(ddl, (StreamQueryConfig) context.getQueryConfig());
				} else {
					tEnv.sqlUpdate(ddl);
				}
				return null;
			});
		} catch (Throwable t) {
			// catch everything such that the statement does not crash the executor
			throw new SqlExecutionException(getExceptionMsg(), t);
		}

		return OperationUtil.AFFECTED_ROW_COUNT0;
	}

	private String getExceptionMsg() {
		final String actionMsg;
		switch (command) {
			case CREATE_TABLE:
				actionMsg = "create a table";
				break;
			case CREATE_DATABASE:
				actionMsg = "create a database";
				break;
			case DROP_TABLE:
				actionMsg = "drop a table";
				break;
			case DROP_DATABASE:
				actionMsg = "drop a database";
				break;
			case ALTER_TABLE:
				actionMsg = "alter a table";
				break;
			case ALTER_DATABASE:
				actionMsg = "alter a database";
				break;
			default:
				actionMsg = null;
		}

		if (actionMsg != null) {
			return String.format("Could not %s from statement: %s.", actionMsg, ddl);
		} else {
			return "Invalid DDL statement.";
		}
	}
}
