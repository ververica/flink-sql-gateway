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

import com.ververica.flink.table.gateway.rest.result.ResultSet;

import org.junit.Test;

import static com.ververica.flink.table.gateway.SqlCommandParser.SqlCommand.CREATE_DATABASE;
import static com.ververica.flink.table.gateway.SqlCommandParser.SqlCommand.CREATE_TABLE;
import static com.ververica.flink.table.gateway.SqlCommandParser.SqlCommand.DROP_TABLE;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link DDLOperation}.
 */
public class DDLOperationTest extends OperationTestBase {

	@Test
	public void testCreateTable() {
		final String ddlTemplate = "create table %s(\n" +
			"  a int,\n" +
			"  b bigint,\n" +
			"  c varchar\n" +
			") with (\n" +
			"  'connector.type'='filesystem',\n" +
			"  'format.type'='csv',\n" +
			"  'connector.path'='xxx'\n" +
			")\n";
		DDLOperation operation = new DDLOperation(context, String.format(ddlTemplate, "MyTable1"), CREATE_TABLE);
		ResultSet resultSet = operation.execute();
		assertEquals(OperationUtil.OK, resultSet);

		String[] tables = context.getExecutionContext().getTableEnvironment().listTables();
		assertArrayEquals(new String[] { "MyTable1" }, tables);
	}

	@Test
	public void testDropTable() {
		String[] tables1 = context.getExecutionContext().getTableEnvironment().listTables();
		assertEquals(0, tables1.length);

		DDLOperation operation1 = new DDLOperation(context, "DROP TABLE IF EXISTS MyTable1", DROP_TABLE);
		assertEquals(OperationUtil.OK, operation1.execute());

		final String ddlTemplate = "create table %s(\n" +
			"  a int,\n" +
			"  b bigint,\n" +
			"  c varchar\n" +
			") with (\n" +
			"  'connector.type'='filesystem',\n" +
			"  'format.type'='csv',\n" +
			"  'connector.path'='xxx'\n" +
			")\n";
		DDLOperation operation = new DDLOperation(context, String.format(ddlTemplate, "MyTable1"), CREATE_TABLE);
		assertEquals(OperationUtil.OK, operation.execute());

		String[] tables2 = context.getExecutionContext().getTableEnvironment().listTables();
		assertArrayEquals(new String[] { "MyTable1" }, tables2);

		DDLOperation operation2 = new DDLOperation(context, "DROP TABLE MyTable1", DROP_TABLE);
		assertEquals(OperationUtil.OK, operation2.execute());

		String[] tables3 = context.getExecutionContext().getTableEnvironment().listTables();
		assertEquals(0, tables3.length);
	}

	@Test
	public void testCreateDatabase() {
		DDLOperation operation = new DDLOperation(context, "create database MyDatabase1", CREATE_DATABASE);
		assertEquals(OperationUtil.OK, operation.execute());

		String[] tables2 = context.getExecutionContext().getTableEnvironment().listDatabases();
		assertArrayEquals(new String[] { "default_database", "MyDatabase1" }, tables2);
	}
}
