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

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link DropViewOperation}.
 */
public class DropViewOperationTest extends OperationTestBase {

	@Test
	public void testDropViewTest() {
		CreateViewOperation operation1 = new CreateViewOperation(context, "MyView1", "select 1 + 1");
		assertEquals(OperationUtil.AFFECTED_ROW_COUNT0, operation1.execute());

		String[] tables1 = context.getExecutionContext().getTableEnvironment().listTables();
		assertArrayEquals(new String[] { "MyView1" }, tables1);

		DropViewOperation operation2 = new DropViewOperation(context, "MyView1");
		assertEquals(OperationUtil.AFFECTED_ROW_COUNT0, operation2.execute());

		String[] tables2 = context.getExecutionContext().getTableEnvironment().listTables();
		assertEquals(0, tables2.length);
	}

	@Test(expected = SqlExecutionException.class)
	public void testDropViewTest_ViewNotExist() {
		DropViewOperation operation = new DropViewOperation(context, "MyView1");
		operation.execute();
	}

}
