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
import com.ververica.flink.table.gateway.context.DefaultContext;
import com.ververica.flink.table.gateway.context.SessionContext;
import com.ververica.flink.table.gateway.rest.result.ColumnInfo;
import com.ververica.flink.table.gateway.rest.result.ConstantNames;
import com.ververica.flink.table.gateway.rest.result.ResultKind;
import com.ververica.flink.table.gateway.rest.result.ResultSet;
import com.ververica.flink.table.gateway.utils.ResourceFileUtils;

import org.apache.flink.client.cli.DefaultCLI;
import org.apache.flink.client.deployment.DefaultClusterClientServiceLoader;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.types.logical.BooleanType;
import org.apache.flink.types.Row;

import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for operations when user jars are provided.
 *
 * <p>NOTE: before running this test, please make sure that {@code random-source-test-jar.jar}
 * exists in the target directory. If not, run {@code mvn clean package} first.
 */
public class OperationWithUserJarTest extends OperationTestBase {

	@Override
	protected DefaultContext getDefaultContext() {
		URL jarUrl = compileUserDefinedSource();
		return new DefaultContext(
			new Environment(),
			Collections.singletonList(jarUrl),
			new Configuration(),
			new DefaultCLI(new Configuration()),
			new DefaultClusterClientServiceLoader());
	}

	@Test
	public void testView() {
		createUserDefinedSource(context, "R");

		CreateViewOperation createViewOperation = new CreateViewOperation(
			context, "MyView1", "SELECT * FROM R");
		ResultSet createViewResult = createViewOperation.execute();
		assertEquals(OperationUtil.OK, createViewResult);

		String[] tables1 = context.getExecutionContext().getTableEnvironment().listTables();
		Arrays.sort(tables1);
		assertArrayEquals(new String[]{"MyView1", "R"}, tables1);
		assertTrue(context.getExecutionContext().getEnvironment().getTables().containsKey("MyView1"));

		DropViewOperation operation2 = new DropViewOperation(context, "MyView1", false);
		assertEquals(OperationUtil.OK, operation2.execute());

		String[] tables2 = context.getExecutionContext().getTableEnvironment().listTables();
		assertArrayEquals(new String[]{"R"}, tables2);
	}

	@Test
	public void testDescribe() throws Exception {
		createUserDefinedSource(context, "R");

		DescribeTableOperation operation = new DescribeTableOperation(context, "R");
		ResultSet resultSet = operation.execute();

		List<Row> expectedData = Arrays.asList(
			Row.of("a", "INT", true, null, null, null),
			Row.of("b", "BIGINT", true, null, null, null));
		ResultSet expected = ResultSet.builder()
			.resultKind(ResultKind.SUCCESS_WITH_CONTENT)
			.columns(
				ColumnInfo.create(ConstantNames.DESCRIBE_NAME, DataTypes.STRING().getLogicalType()),
				ColumnInfo.create(ConstantNames.DESCRIBE_TYPE, DataTypes.STRING().getLogicalType()),
				ColumnInfo.create(ConstantNames.DESCRIBE_NULL, new BooleanType()),
				ColumnInfo.create(ConstantNames.DESCRIBE_KEY, DataTypes.STRING().getLogicalType()),
				ColumnInfo.create(ConstantNames.DESCRIBE_COMPUTED_COLUMN, DataTypes.STRING().getLogicalType()),
				ColumnInfo.create(ConstantNames.DESCRIBE_WATERMARK, DataTypes.STRING().getLogicalType()))
			.data(expectedData)
			.build();

		assertEquals(expected, resultSet);
	}

	@Test
	public void testExplain() {
		createUserDefinedSource(context, "R");

		ExplainOperation operation = new ExplainOperation(context, "select * from R");
		ResultSet resultSet = operation.execute();

		String expectedExplain = ResourceFileUtils.readAll(
			"plan/operation-with-user-jar-test.test-explain.expected");
		OperationTestUtils.compareExplainResult(expectedExplain, resultSet);
	}

	private URL compileUserDefinedSource() {
		File resourceFile = new File(
			OperationTestBase.class.getClassLoader().getResource("service-file/test-random-source-file").getFile());
		File jarFile = new File(resourceFile.getParent() + "/../../random-source-test-jar.jar");
		try {
			return jarFile.toURI().toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException("Failed to find jar file of random-source", e);
		}
	}

	private void createUserDefinedSource(SessionContext context, String name) {
		String ddl = "CREATE TABLE " + name + "(\n" +
			"  a INT,\n" +
			"  b BIGINT\n" +
			") WITH (\n" +
			"  'connector.type' = 'random',\n" +
			"  'random.limit' = '10'\n" +
			")";
		DDLOperation createTableOperation = new DDLOperation(context, ddl, SqlCommandParser.SqlCommand.CREATE_TABLE);
		ResultSet createTableResult = createTableOperation.execute();
		assertEquals(OperationUtil.OK, createTableResult);
	}

}
