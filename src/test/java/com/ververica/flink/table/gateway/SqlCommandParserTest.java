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

package com.ververica.flink.table.gateway;

import com.ververica.flink.table.gateway.SqlCommandParser.SqlCommand;
import com.ververica.flink.table.gateway.SqlCommandParser.SqlCommandCall;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for {@link SqlCommandParser}.
 */
public class SqlCommandParserTest {

	@Test
	public void testSelect() {
		String query1 = "select * from MyTable";
		checkCommand(query1, SqlCommand.SELECT, query1);

		String query2 = "-- single-line comment \n select * from MyTable";
		checkCommand(query2, SqlCommand.SELECT, query2);

		String query3 = "/* multi-line comments \n more comments */ \n select * from MyTable";
		checkCommand(query3, SqlCommand.SELECT, query3);

		String query4 = "select * from MyTable \n -- single-line comment";
		checkCommand(query4, SqlCommand.SELECT, query4);

		String query5 = " \n select abc from MyTable \n";
		checkCommand(query5, SqlCommand.SELECT, query5);
	}

	@Test
	public void testSelectWith() {
		String query1 = "with t as " +
			"(select * from MyTable where a > 10) " +
			"select * from t where c is not null;";
		checkCommand(query1, SqlCommand.SELECT, query1);

		String query2 = "/* multi-line comments */ \n" +
			" -- single-line comments \n" +
			" with t as " +
			"(select * from MyTable where a > 10) " +
			"select * from t where c is not null \n --with statement; ";
		checkCommand(query2, SqlCommand.SELECT, query2);
	}

	@Test(expected = SqlParseException.class)
	public void testInvalidSelect() {
		String query = "select * from MyTable where";
		checkCommand(query, SqlCommand.SELECT, query);
	}

	@Test
	public void testInvalidSelectToCheckLineNumber() {
		String query = "\n\nselect * from MyTable \nwhere\n";
		try {
			SqlCommandParser.parse(query, true);
			fail();
		} catch (SqlParseException e) {
			assertTrue(e.getCause().getMessage().contains("Encountered \"<EOF>\" at line 4, column 6."));
		}
	}

	@Test
	public void testInsert() {
		String query1 = "insert into MySink select * from MyTable where a > 10";
		checkCommand(query1, SqlCommand.INSERT_INTO, query1);

		String query2 = "\n -- single-line comment \n insert into MySink select * from MyTable where a > 10 " +
			"/* multi-line comments \n more comments */ \n";
		checkCommand(query2, SqlCommand.INSERT_INTO, query2);
	}

	@Test
	public void testInsertOverwrite() {
		String query1 = "insert overwrite MySink select * from MyTable where a > 10";
		checkCommand(query1, SqlCommand.INSERT_OVERWRITE, query1);

		String query2 = "\n -- single-line comment \n insert overwrite MySink select * from MyTable where a > 10 " +
			"/* multi-line comments \n more comments */ \n";
		checkCommand(query2, SqlCommand.INSERT_OVERWRITE, query2);
	}

	@Test
	public void testCreateTable() {
		String query1 = "create table MyTable (a int);";
		checkCommand(query1, SqlCommand.CREATE_TABLE, query1);

		String query2 = " \n -- single-line comment \n create \n table \n MyTable (a int);";
		checkCommand(query2, SqlCommand.CREATE_TABLE, query2);
	}

	@Test
	public void testDropTable() {
		String query1 = "drop table MyTable;";
		checkCommand(query1, SqlCommand.DROP_TABLE, query1);

		String query2 = " \n -- single-line comment \n drop \n table \n MyTable;";
		checkCommand(query2, SqlCommand.DROP_TABLE, query2);
	}

	@Test
	public void testAlterTable() {
		String query1 = "alter table MyTable rename to MyTable2;";
		checkCommand(query1, SqlCommand.ALTER_TABLE, query1);

		String query2 = " \n -- single-line comment \n alter \n table MyTable rename to MyTable2" +
			"\n  /* multi-line comments */";
		checkCommand(query2, SqlCommand.ALTER_TABLE, query2);
	}

	@Test
	public void testCreateView() {
		String view = "MyView";
		String query = "select * from MyTable where a > 10";
		String parsedQuery = "SELECT *\n" +
			"FROM `MyTable`\n" +
			"WHERE `a` > 10";

		String stmt1 = "create view " + view + " as " + query + ";";
		checkCommand(stmt1, SqlCommand.CREATE_VIEW, view, parsedQuery);

		String stmt2 = " \n -- single-line comment \n create \n -- single-line comment \n view " + view + " as " +
			query + "\n  /* multi-line comments */";
		checkCommand(stmt2, SqlCommand.CREATE_VIEW, view, parsedQuery);
	}

	@Test
	public void testDropView() {
		String view = "MyView";

		String stmt1 = "drop view " + view + ";";
		checkCommand(stmt1, SqlCommand.DROP_VIEW, view, "false");

		String stmt2 = " \n -- single-line comment \n drop \n view \n " + view + ";";
		checkCommand(stmt2, SqlCommand.DROP_VIEW, view, "false");

		String stmt3 = "drop view if exists " + view + ";";
		checkCommand(stmt3, SqlCommand.DROP_VIEW, view, "true");

		String stmt4 = " \n -- single-line comment \n drop \n view \n if exists " + view + ";";
		checkCommand(stmt4, SqlCommand.DROP_VIEW, view, "true");
	}

	@Test
	public void testCreateDatabase() {
		String query1 = "create database MyDb;";
		checkCommand(query1, SqlCommand.CREATE_DATABASE, query1);

		String query2 = " \n -- single-line comment \n create \n database \n MyDb;";
		checkCommand(query2, SqlCommand.CREATE_DATABASE, query2);
	}

	@Test
	public void testDropDatabase() {
		String query1 = "drop database MyDb;";
		checkCommand(query1, SqlCommand.DROP_DATABASE, query1);

		String query2 = " \n -- single-line comment \n drop \n database \n MyDb;";
		checkCommand(query2, SqlCommand.DROP_DATABASE, query2);
	}

	@Test
	public void testAlterDatabase() {
		String query1 = "alter database MyDb set ('k1' = 'a');";
		checkCommand(query1, SqlCommand.ALTER_DATABASE, query1);

		String query2 = " \n -- single-line comment \n alter \n database MyDb set ('k1' = 'a')" +
			"\n  /* multi-line comments */";
		checkCommand(query2, SqlCommand.ALTER_DATABASE, query2);
	}

	@Test
	public void testUseCatalog() {
		String query1 = "use catalog MyCat";
		checkCommand(query1, SqlCommand.USE_CATALOG, "MyCat");

		String query2 = "\n -- comments \n use \n -- comments \n catalog MyCat ;  -- comments";
		checkCommand(query2, SqlCommand.USE_CATALOG, "MyCat");
	}

	@Test
	public void testUseDatabase() {
		String query1 = "use MyCat.MyDb";
		checkCommand(query1, SqlCommand.USE, "MyCat.MyDb");

		String query2 = "\n -- comments \n  use \n -- comments \n MyCat.MyDb ; -- comments";
		checkCommand(query2, SqlCommand.USE, "MyCat.MyDb");
	}

	@Test
	public void testShowCatalogs() {
		String query1 = "show catalogs";
		checkCommand(query1, SqlCommand.SHOW_CATALOGS);

		String query2 = "\n -- comments \n  show \n -- comments \n catalogs; -- comments";
		checkCommand(query2, SqlCommand.SHOW_CATALOGS);
	}

	@Test
	public void testShowDatabases() {
		String query1 = "show databases";
		checkCommand(query1, SqlCommand.SHOW_DATABASES);

		String query2 = "\n -- comments \n  show \n -- comments \n databases; -- comments";
		checkCommand(query2, SqlCommand.SHOW_DATABASES);
	}

	@Test
	public void testShowTables() {
		String query1 = "show tables";
		checkCommand(query1, SqlCommand.SHOW_TABLES);

		String query2 = "\n -- comments \n show \n -- comments \n tables; -- comments";
		checkCommand(query2, SqlCommand.SHOW_TABLES);
	}

	@Test
	public void testShowFunctions() {
		String query1 = "show functions";
		checkCommand(query1, SqlCommand.SHOW_FUNCTIONS);

		String query2 = "\n -- comments \n show \n -- comments \n functions; -- comments";
		checkCommand(query2, SqlCommand.SHOW_FUNCTIONS);
	}

	@Test
	public void testDescribeTable() {
		String query2 = "describe table MyTable";
		checkCommand(query2, SqlCommand.DESCRIBE_TABLE, "MyTable");

		String query3 = "\n -- comments \n describe \n -- comments \n table  MyTable; -- comments";
		checkCommand(query3, SqlCommand.DESCRIBE_TABLE, "MyTable");
	}

	@Test
	public void testExplainPlan() {
		String query1 = "explain plan for select * from MyTable";
		checkCommand(query1, SqlCommand.EXPLAIN, "SELECT *\nFROM `MyTable`");

		String query2 = "\n -- comments \n explain \n -- comments \n plan \n -- comments \n for \n " +
			"-- comments \n select * from MyTable; \n -- comments \n";
		checkCommand(query2, SqlCommand.EXPLAIN, "SELECT *\nFROM `MyTable`");
	}

	@Test
	public void testReset() {
		String query1 = "reset all";
		checkCommand(query1, SqlCommand.RESET);

		String query2 = "reset execution.parallelism";
		checkCommand(query2, SqlCommand.RESET, "execution.parallelism");

		String query3 = "\n -- comments \n reset \n -- comments \n all \n -- comments \n";
		checkCommand(query3, SqlCommand.RESET);

		String query4 = "\n -- comments \n reset \n -- comments \n execution.parallelism \n -- comments \n";
		checkCommand(query4, SqlCommand.RESET, "execution.parallelism");
	}

	@Test
	public void testSet() {
		String query1 = "set execution.parallelism=10";
		checkCommand(query1, SqlCommand.SET, "execution.parallelism", "10");

		String query2 = "set";
		checkCommand(query2, SqlCommand.SET);

		String query3 = "set `a.b-c_d`=`e-f_g.h`";
		checkCommand(query3, SqlCommand.SET, "a.b-c_d", "e-f_g.h");

		String query4 = "\n -- comments \n set \n -- comments \n execution.parallelism \n" +
			" -- comments \n = \n -- comments \n 10";
		checkCommand(query4, SqlCommand.SET, "execution.parallelism", "10");
	}

	@Test
	public void testShowModules() {
		String query = "show       modules";
		checkCommand(query, SqlCommand.SHOW_MODULES);
	}

	@Test
	public void testShowCurrentCatalog() {
		String query = "show      current  \t catalog";
		checkCommand(query, SqlCommand.SHOW_CURRENT_CATALOG);
	}

	@Test
	public void testShowCurrentDatabase() {
		String query = "show      current  \t database";
		checkCommand(query, SqlCommand.SHOW_CURRENT_DATABASE);
	}

	private void checkCommand(String stmt, SqlCommand expectedCmd, String... expectedOperand) {
		Optional<SqlCommandCall> cmd2 = SqlCommandParser.parse(stmt, true);
		assertTrue(cmd2.isPresent());
		assertEquals(expectedCmd, cmd2.get().command);
		assertArrayEquals(expectedOperand, cmd2.get().operands);
	}
}
