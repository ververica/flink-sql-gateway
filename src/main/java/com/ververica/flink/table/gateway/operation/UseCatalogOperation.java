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
import com.ververica.flink.table.gateway.rest.result.ResultSet;

import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.catalog.exceptions.CatalogException;

/**
 * Operation for USE CATALOG command.
 */
public class UseCatalogOperation implements NonJobOperation {
	private final ExecutionContext<?> context;
	private final String catalogName;

	public UseCatalogOperation(SessionContext context, String catalogName) {
		this.context = context.getExecutionContext();
		this.catalogName = catalogName;
	}

	@Override
	public ResultSet execute() {
		final TableEnvironment tableEnv = context.getTableEnvironment();

		context.wrapClassLoader(() -> {
			// Rely on TableEnvironment/CatalogManager to validate input
			try {
				tableEnv.useCatalog(catalogName);
				return null;
			} catch (CatalogException e) {
				throw new SqlExecutionException("Failed to switch to catalog " + catalogName, e);
			}
		});

		return OperationUtil.OK;
	}
}
