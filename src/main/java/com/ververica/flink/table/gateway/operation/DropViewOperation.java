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
import com.ververica.flink.table.gateway.config.entries.TableEntry;
import com.ververica.flink.table.gateway.config.entries.ViewEntry;
import com.ververica.flink.table.gateway.context.ExecutionContext;
import com.ververica.flink.table.gateway.context.SessionContext;
import com.ververica.flink.table.gateway.rest.result.ResultSet;
import com.ververica.flink.table.gateway.utils.SqlExecutionException;

/**
 * Operation for DROP VIEW command.
 */
public class DropViewOperation implements NonJobOperation {
	private final SessionContext context;
	private final String viewName;
	private final boolean ifExists;

	public DropViewOperation(SessionContext context, String viewName, boolean ifExists) {
		this.context = context;
		this.viewName = viewName;
		this.ifExists = ifExists;
	}

	@Override
	public ResultSet execute() {
		Environment env = context.getExecutionContext().getEnvironment();
		TableEntry tableEntry = env.getTables().get(viewName);
		if (!(tableEntry instanceof ViewEntry)) {
			if (!ifExists) {
				throw new SqlExecutionException("'" + viewName + "' does not exist in the current session.");
			}
		}

		// Here we rebuild the ExecutionContext because we want to ensure that all the remaining views can work fine.
		// Assume the case:
		//   view1=select 1;
		//   view2=select * from view1;
		// If we delete view1 successfully, then query view2 will throw exception because view1 does not exist. we want
		// all the remaining views are OK, so do the ExecutionContext rebuilding to avoid breaking the view dependency.
		Environment newEnv = env.clone();
		if (newEnv.getTables().remove(viewName) != null) {
			ExecutionContext<?> oldExecutionContext = context.getExecutionContext();
			oldExecutionContext.wrapClassLoader(() -> {
				oldExecutionContext.getTableEnvironment().dropTemporaryView(viewName);
				return null;
			});
			// Renew the ExecutionContext.
			ExecutionContext<?> newExecutionContext = context
				.createExecutionContextBuilder(context.getOriginalSessionEnv())
				.env(newEnv)
				.sessionState(context.getExecutionContext().getSessionState())
				.build();
			context.setExecutionContext(newExecutionContext);
		}

		return OperationUtil.OK;
	}
}
