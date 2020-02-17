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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link CreateTableOperation}.
 */
public class CreateTableOperationTest extends OperationTestBase {

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
		CreateTableOperation operation = new CreateTableOperation(context, String.format(ddlTemplate, "MyTable1"));
		ResultSet resultSet = operation.execute();
		assertEquals(OperationUtil.AFFECTED_ROW_COUNT0, resultSet);

		String[] tables = context.getExecutionContext().getTableEnvironment().listTables();
		assertArrayEquals(new String[] { "MyTable1" }, tables);
	}
}
