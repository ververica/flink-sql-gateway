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

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link DropTableOperation}.
 */
public class DropTableOperationTest extends OperationTestBase {

	@Test
	public void testDropTable() {

		String[] tables1 = context.getExecutionContext().getTableEnvironment().listTables();
		assertEquals(0, tables1.length);

		DropTableOperation operation1 = new DropTableOperation(context, "DROP TABLE IF EXISTS MyTable1");
		assertEquals(OperationUtil.AFFECTED_ROW_COUNT0, operation1.execute());

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
		assertEquals(OperationUtil.AFFECTED_ROW_COUNT0, operation.execute());

		String[] tables2 = context.getExecutionContext().getTableEnvironment().listTables();
		assertArrayEquals(new String[] { "MyTable1" }, tables2);

		DropTableOperation operation2 = new DropTableOperation(context, "DROP TABLE MyTable1");
		assertEquals(OperationUtil.AFFECTED_ROW_COUNT0, operation2.execute());

		String[] tables3 = context.getExecutionContext().getTableEnvironment().listTables();
		assertEquals(0, tables3.length);
	}
}
